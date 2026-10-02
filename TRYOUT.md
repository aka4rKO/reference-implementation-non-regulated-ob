# Try out the Accounts and Payments APIs

This guide sets up everything on one machine and takes you through a full Accounts and Payments
flow. For what's in the repo, see the [README](README.md).

To do all of this automatically, use the [automated setup](setup/README.md) instead.

## Contents

- [Before you start](#before-you-start)
- **Set up the servers**
  1. [Set up API Manager and Identity Server](#step-1-set-up-api-manager-and-identity-server)
  2. [Set up the accelerators](#step-2-set-up-the-accelerators)
  3. [Exchange certificates between the servers](#step-3-exchange-certificates-between-the-servers)
  4. [Create the client application's keys and certificates](#step-4-create-the-client-applications-keys-and-certificates)
  5. [Build and deploy the webapps](#step-5-build-and-deploy-the-webapps)
  6. [Add the configuration](#step-6-add-the-configuration): [Identity Server](#identity-server),
     [API Manager](#api-manager)
  7. [Start the servers](#step-7-start-the-servers)
- **Configure API Manager and Identity Server**
  8. [Add the key manager](#step-8-add-the-key-manager)
  9. [Turn on the approval workflow](#step-9-turn-on-the-approval-workflow)
  10. [Publish the APIs](#step-10-publish-the-apis)
  11. [Register the RAR types in Identity Server](#step-11-register-the-rar-types-in-identity-server)
  12. [Give the admin access to the consent APIs](#step-12-give-the-admin-access-to-the-consent-apis)
- **Onboard the client application and call the APIs**
  13. [Onboard the client application](#step-13-onboard-the-client-application)
  14. [Let the client application use the RAR types](#step-14-let-the-client-application-use-the-rar-types)
  15. [Call the APIs](#step-15-call-the-apis)

## Before you start

You need:

- The WSO2 Financial Services accelerators 4.0: `wso2-fsam-accelerator-4.0.0` and
  `wso2-fsiam-accelerator-4.0.0`
- A JDK supported by API Manager 4.7 and Identity Server 7.3
- Java 8 and Maven, to build this repo
- MySQL 8, which the accelerator's `configure.sh` sets up the databases in
- `jq`, `curl`, `openssl`, Node.js and Python 3
- [Postman](https://www.postman.com/downloads/)

This guide uses these placeholders:

| Placeholder | Meaning |
|---|---|
| `<APIM_HOME>` | Where API Manager is extracted |
| `<IS_HOME>` | Where Identity Server is extracted |
| `<REPO>` | Where this repo is cloned |

Once set up, the servers run on these ports:

| Server | Port | Used for |
|---|---|---|
| Identity Server | `9446` | Console, OAuth endpoints and the service extension |
| API Manager | `9443` | Publisher, Developer Portal, Admin Portal, Carbon Console |
| API Manager | `8243` | API gateway |
| API Manager | `9763` | Demo bank backend (plain HTTP) |

## Step 1: Set up API Manager and Identity Server

1. Download [WSO2 API Manager 4.7.0](https://wso2.com/api-manager/) and
   [WSO2 Identity Server 7.3.0](https://wso2.com/identity-server/), and extract them.
2. Update both products. In each product's `bin` folder:

   ```bash
   ./update_tool_setup.sh        # downloads the update tool for your OS
   ./wso2update_<os>_<arch>      # for example ./wso2update_darwin_arm64 or ./wso2update_linux_amd64
   ```

   On Windows, use `update_tool_setup.ps1` and the `.exe` tool it downloads.

## Step 2: Set up the accelerators

1. Copy the accelerators into the products:
   - `wso2-fsam-accelerator-4.0.0` into `<APIM_HOME>`
   - `wso2-fsiam-accelerator-4.0.0` into `<IS_HOME>`
2. Put the MySQL JDBC driver in `<APIM_HOME>/repository/components/lib` and
   `<IS_HOME>/repository/components/lib`.
3. Update the accelerators, the same way as the products in step 1. Run the update tool in each
   accelerator's `bin` folder.
4. Edit each accelerator's `repository/conf/configure.properties`:

   | Accelerator | Set |
   |---|---|
   | `fsam` | `PRODUCT_CONF_PATH=repository/resources/wso2am-4.7.0-deployment.toml` |
   | `fsiam` | `IS_PRODUCT=wso2is-7.3.0` and `PRODUCT_CONF_PATH=repository/resources/wso2is-7.3.0-deployment.toml` |

   In both, also check the hostnames (`localhost`), the admin credentials and the database
   settings. The default Identity Server admin is `is_admin@wso2.com` / `wso2123`. Later steps use
   it.

5. In each accelerator's `bin` folder, run:

   ```bash
   ./merge.sh       # copies the accelerator into the product
   ./configure.sh   # writes deployment.toml and creates the databases
   ```

> Whenever you update an accelerator again, run its `merge.sh` again.

For more detail, see the
[WSO2 accelerator setup guide](https://ob.docs.wso2.com/en/latest/get-started/set-up-accelerators/).

## Step 3: Exchange certificates between the servers

The default certificates are often expired. So give each server a new certificate, then make each
server trust both. The keystore password is `wso2carbon`.

1. **Identity Server.** In `<IS_HOME>/repository/resources/security`, replace the key and export
   the certificate:

   ```bash
   keytool -delete -alias wso2carbon -keystore wso2carbon.p12 -storepass wso2carbon
   keytool -genkey -alias wso2carbon -keystore wso2carbon.p12 -storepass wso2carbon \
     -keyalg RSA -keysize 2048 -validity 3650 \
     -dname "CN=localhost" -ext san=dns:localhost,ip:127.0.0.1
   keytool -export -alias wso2carbon -keystore wso2carbon.p12 -storepass wso2carbon -file is.pem
   ```

2. **API Manager.** In `<APIM_HOME>/repository/resources/security`, do the same:

   ```bash
   keytool -delete -alias wso2carbon -keystore wso2carbon.jks -storepass wso2carbon
   keytool -genkey -alias wso2carbon -keystore wso2carbon.jks -storepass wso2carbon -keypass wso2carbon \
     -keyalg RSA -keysize 2048 -validity 3650 \
     -dname "CN=localhost" -ext san=dns:localhost,ip:127.0.0.1
   keytool -export -alias wso2carbon -keystore wso2carbon.jks -storepass wso2carbon -file am.pem
   ```

3. Copy `is.pem` and `am.pem` into both folders. Then add both certificates to each server's
   truststore:

   ```bash
   # In <IS_HOME>/repository/resources/security
   keytool -import -noprompt -alias wso2is -file is.pem -keystore client-truststore.p12 -storepass wso2carbon
   keytool -import -noprompt -alias wso2am -file am.pem -keystore client-truststore.p12 -storepass wso2carbon

   # In <APIM_HOME>/repository/resources/security
   keytool -import -noprompt -alias wso2is -file is.pem -keystore client-truststore.jks -storepass wso2carbon
   keytool -import -noprompt -alias wso2am -file am.pem -keystore client-truststore.jks -storepass wso2carbon
   ```

Keep the alias `wso2am`. Identity Server uses it to check requests signed by API Manager (see
step 6).

If you use a hostname other than `localhost`, put it in `CN=` and `san=dns:`. For JKS files,
`keytool` warns that the format is proprietary. You can ignore that.

## Step 4: Create the client application's keys and certificates

The client application (the third-party app calling the APIs) needs two key pairs:

- **Signing:** signs the client application's JWTs. Identity Server checks them with the client
  application's public keys, which it reads from its JWKS URL.
- **Transport:** the client certificate for mTLS.

> **For testing only.** These certificates are self-signed. A real client application needs certificates issued by
> a trusted certificate authority.

1. Create the keys and certificates in a folder of your choice:

   ```bash
   openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=client-signing" \
     -keyout signing.key -out signing.pem
   openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=client-transport" \
     -keyout transport.key -out transport.pem
   ```

2. Turn the signing key into a JWK (`jwk.json`, private, used by Postman) and a JWKS
   (`jwks.json`, public, served to Identity Server):

   ```bash
   node -e '
   const c = require("crypto"), fs = require("fs");
   const key = c.createPrivateKey(fs.readFileSync("signing.key"));
   const meta = { kid: "client-signing-key", alg: "PS256", use: "sig" };
   fs.writeFileSync("jwk.json", JSON.stringify({ ...key.export({ format: "jwk" }), ...meta }));
   fs.writeFileSync("jwks.json", JSON.stringify({ keys: [{ ...c.createPublicKey(key).export({ format: "jwk" }), ...meta }] }, null, 2));
   '
   ```

3. Serve the JWKS, and leave it running:

   ```bash
   python3 -m http.server 8000
   ```

   The client application's JWKS URL is now `http://localhost:8000/jwks.json`.

4. Make both servers trust the transport certificate. Do this **before** you start the servers:

   ```bash
   keytool -import -noprompt -trustcacerts -alias client-transport -file transport.pem \
     -keystore <APIM_HOME>/repository/resources/security/client-truststore.jks -storepass wso2carbon
   keytool -import -noprompt -trustcacerts -alias client-transport -file transport.pem \
     -keystore <IS_HOME>/repository/resources/security/client-truststore.p12 -storepass wso2carbon
   ```

## Step 5: Build and deploy the webapps

```bash
cd <REPO>
mvn clean install

cp "non-regulated-ob-service-extension/target/non#regulated#ob#service#extension.war" \
   <IS_HOME>/repository/deployment/server/webapps/
cp "non-regulated-ob-demo-backend/target/non#regulated#ob#demo#backend.war" \
   <APIM_HOME>/repository/deployment/server/webapps/
```

- The **service extension** goes to Identity Server. The accelerator calls it at each consent step.
- The **demo backend** goes to API Manager. It is the bank the APIs route to.

## Step 6: Add the configuration

`configure.sh` already wrote a `deployment.toml` for each server. Now add the changes below.

> A TOML section can appear only once. If a section already exists in the file, add or change the
> keys inside it instead of adding the section again. Sections starting with `[[` are lists, so
> add those as new blocks.

### Identity Server

File: `<IS_HOME>/repository/conf/deployment.toml`

**1. Connect the service extension**

```toml
[financial_services.extensions.endpoint]
enabled = true
allowed_extensions = ["populate_consent_authorize_screen", "persist_authorized_consent",
    "pre_process_consent_revoke", "validate_consent_access", "map_accelerator_error_response"]
base_url = "https://localhost:9446/non/regulated/ob/service/extension"

[financial_services.extensions.endpoint.security]
username = "is_admin@wso2.com"
password = "wso2123"

[[resource.access_control]]
allowed_auth_handlers = ["BasicAuthentication"]
context = "(.*)/non/regulated/ob/service/extension/(.*)"
http_method = "all"
secure = "true"
```

**2. Consent validation**

```toml
[financial_services.consent.validation]
signature.alias = "wso2am"   # API Manager's certificate, from step 3
```

**3. Use Identity Server as API Manager's key manager**

Follow the Identity Server steps in
[Configure WSO2 IS 7 as a key manager](https://apim.docs.wso2.com/en/latest/api-security/key-management/third-party-key-managers/configure-wso2is7-connector/).

**4. Make Identity Server FAPI 2.0 compliant**

Follow the FAPI 2.0 steps in
[Register a FAPI-compliant app](https://is.docs.wso2.com/en/7.1.0/guides/applications/register-a-fapi-compliant-app/).

**5. Allow basic authentication only on the endpoints that need it**

```toml
[compatibility_setting.basic_auth.disable_basic_auth]
default_value = "true"

[compatibility_setting.basic_auth.allowed_endpoints]
default_value = "(/t/[^/]+)?/api/fs/consent/.*, (/t/[^/]+)?/non/regulated/ob/service/extension/.*, (/t/[^/]+)?/api/identity/oauth2/dcr/v1[.]1/register.*, (/t/[^/]+)?/api/identity/oauth2/v1[.]0/scopes.*, (/t/[^/]+)?/api/server/v1/applications.*, (/t/[^/]+)?/api/server/v1/api-resources.*, (/t/[^/]+)?/scim2/users.*, (/t/[^/]+)?/scim2/v2/roles.*, (/t/[^/]+)?/oauth2/introspect"
```

### API Manager

File: `<APIM_HOME>/repository/conf/deployment.toml`

**1. Open up the demo backend**

```toml
[[resource.access_control]]
context = "(.*)/non/regulated/ob/demo/backend/(.*)"
http_method = "all"
secure = "false"
```

**2. Add the error formatter**

Copy [`customErrorFormatter.xml`](artifacts/custom-synapse-error-formatter/customErrorFormatter.xml)
to `<APIM_HOME>/repository/deployment/server/synapse-configs/default/sequences/`, then add:

```toml
[apim.sync_runtime_artifacts.gateway]
skip_list.sequences = ["customErrorFormatter.xml"]
```

Then make the gateway use it. In
`<APIM_HOME>/repository/deployment/server/synapse-configs/default/sequences/_cors_request_handler_.xml`,
add this line just before the closing `</sequence>` tag:

```xml
<sequence key="customErrorFormatter"/>
```

**3. Set up the client application fields**

Replace all the `[[financial_services.keymanager.application.type.attributes]]` blocks with
[`am-deployment-snippet.toml`](artifacts/devportal-km-configs/am-deployment-snippet.toml). This adds
the **JWKS URI** field to the Developer Portal, and the hidden FAPI 2.0 defaults described in the
[README](README.md#client-application-settings).

**4. Other settings**

```toml
[apim.key_manager]
allow_subscription_validation_disabling = false

[transport.https.properties]
maxHttpHeaderSize = "65536"
```

## Step 7: Start the servers

Start Identity Server first, then API Manager:

```bash
<IS_HOME>/bin/wso2server.sh
<APIM_HOME>/bin/api-manager.sh
```

## Step 8: Add the key manager

This makes API Manager use Identity Server to issue and check tokens. Follow the API Manager steps
in [Configure WSO2 Financial Service Key Manager](https://ob.docs.wso2.com/en/latest/tryout-flows/accelerator-with-is-and-apim/configure-fskm/).

When you add it, set these from Identity Server's discovery document,
`https://localhost:9446/oauth2/token/.well-known/openid-configuration`:

- **Issuer**: its `issuer`, `https://localhost:9446/oauth2/oidcdiscovery`.
- **Grant Types**: every grant type under `grant_types_supported`.

Skip the page's last step, **Disable the Resident Key Manager**.

## Step 9: Turn on the approval workflow

With approval on, a bank admin approves each developer sign-up, client application, subscription
and key request.

1. Sign in to the Carbon Console at `https://localhost:9443/carbon`.
2. Go to **Main** → **Registry** → **Browse**.
3. Open `/_system/governance/apimgt/applicationdata/workflow-extensions.xml` and click
   **Edit as text**.
4. Replace the content with
   [`workflow-extensions.xml`](artifacts/workflow-extensions/workflow-extensions.xml) and click
   **Save Content**.

Requests waiting for approval show up in the Admin Portal under **Tasks**. To turn approval off
again, use [`default-workflow-extensions.xml`](artifacts/workflow-extensions/default-workflow-extensions.xml).

## Step 10: Publish the APIs

### Create the policies

Do this once. The policy files are in
`<APIM_HOME>/wso2-fsam-accelerator-4.0.0/repository/resources/mediation-policies`.

1. Sign in to the Publisher at `https://localhost:9443/publisher` and go to **Policies** →
   **Add New Policy**.
2. Create each policy below. For all three, set **Applicable Flows** to **Request** and
   **Supported API Types** to **HTTP**, upload the policy file, and add the attributes.

| Policy | File | Attributes |
|---|---|---|
| MTLS Enforcement Policy | `mtlsEnforcementPolicy.j2` | `transportCertAsHeaderEnabled` (Boolean), `transportCertHeaderName` (String), `isClientCertificateEncoded` (Boolean). All optional. |
| Consent Enforcement Policy | `consentEnforcementPolicy.j2` | `consentIdClaimName`, `consentServiceBasicAuthCredentials`, `consentServiceBaseUrl`. All required strings. |
| Dynamic Endpoint Policy | `dynamicEndpointPolicy.j2` | `consentServiceRoutingRegexPattern`, `consentServiceBasicAuthCredentials`, `consentServiceBaseUrl`, `bankBackendBaseUrl`. All required strings. |

For screenshots and every attribute's display name and description, see
[Create Policies](https://ob.docs.wso2.com/en/latest/learn/create-policies/) and the
[MTLS](https://ob.docs.wso2.com/en/latest/learn/mtls-enforcement-policy/),
[Consent Enforcement](https://ob.docs.wso2.com/en/latest/learn/consent-enforcement-policy/) and
[Dynamic Endpoint](https://ob.docs.wso2.com/en/latest/learn/dynamic-endpoint-policy/) policy pages.

### Create and publish each API

| API | OpenAPI spec | Context |
|---|---|---|
| Account Information | [`account-info-openapi.yaml`](artifacts/apis/accounts/account-info-openapi.yaml) | `/open-banking/{version}/account-information` |
| Payment Initiation | [`payment-initiation-openapi.yaml`](artifacts/apis/payments/payment-initiation-openapi.yaml) | `/open-banking/{version}/payment-initiation` |

1. In the Publisher, go to **REST API** → **Import Open API**, upload the spec and click **Next**.
2. Set the **Context** from the table above and the **Version** to `v1.0`. Leave the
   **Endpoint** empty, and click **Create**.
3. Under **Endpoints**, choose **Dynamic Endpoints** and save.
4. Under **API Configurations** → **Runtime**, turn on **Schema Validation** and save. The gateway
   then checks each request and response against the OpenAPI spec.
5. Under **Policies**, add these policies, in this order:

   | Policy | Where | Values |
   |---|---|---|
   | MTLS Enforcement Policy | API level | Leave the attributes empty |
   | JWT Claim Based Access Validator | Every operation | Claim `aut`. Value `APPLICATION_USER` on user-token operations, `APPLICATION` on client-token operations. |
   | Consent Enforcement Policy | User-token operations only | See below |
   | Dynamic Endpoint Policy | Every operation, always last | See below |

   The [APIs README](artifacts/apis/README.md) lists which operations take which token.

   **Consent Enforcement Policy** values:

   | Attribute | Value |
   |---|---|
   | `consentIdClaimName` | `consent_id` |
   | `consentServiceBasicAuthCredentials` | `aXNfYWRtaW5Ad3NvMi5jb206d3NvMjEyMw==` (`is_admin@wso2.com:wso2123` in base64) |
   | `consentServiceBaseUrl` | `https://localhost:9446` |

   **Dynamic Endpoint Policy** values:

   | Attribute | Account Information | Payment Initiation |
   |---|---|---|
   | `consentServiceRoutingRegexPattern` | `.*\/consents.*` | `.*\/consents.*` |
   | `bankBackendBaseUrl` | `https://localhost:9443/non/regulated/ob/demo/backend/services/accounts/accountservice` | `https://localhost:9443/non/regulated/ob/demo/backend/services/payments/paymentservice` |
   | `consentServiceBaseUrl` | `https://localhost:9446` | `https://localhost:9446` |
   | `consentServiceBasicAuthCredentials` | Same as above | Same as above |

   Requests whose path matches the regex go to the accelerator's consent service. Everything else
   goes to the demo bank backend. For Account Information, that sends `DELETE /consents/{ConsentId}` to the
   consent service. The Payment Initiation API uses the same regex. It has no consent endpoints,
   so nothing matches it and every call goes to the demo bank backend.

   For credentials other than the default, create the value with
   `printf 'user:password' | base64`.

6. Click **Save and Deploy**, then go to **Overview** and click **Publish**.

## Step 11: Register the RAR types in Identity Server

This tells Identity Server about the consent types, so it can check `authorization_details`.

```bash
cd <REPO>/artifacts/rar-schemas/scripts
export IS_HOST=https://localhost:9446
export IS_AUTH='is_admin@wso2.com:wso2123'

./register.sh register
./register.sh verify     # lists the types Identity Server now knows
```

See the [scripts README](artifacts/rar-schemas/scripts/README.md) for details.

## Step 12: Give the admin access to the consent APIs

In the Identity Server Console at `https://localhost:9446/console`:

1. Go to **API Resources** → **New API Resource**. Set **Identifier** and **Display Name** to
   `OB-internal-api-resource`, add the scope `ob-internal-api-access`, and keep
   **Requires authorization** on.
2. Go to **User Management** → **Roles** → **New Role**. Name it `OBInternalApiAccessRole`, give it
   the `ob-internal-api-access` permission from `OB-internal-api-resource`, and assign the user
   `is_admin@wso2.com`.

## Step 13: Onboard the client application

Now act as the client application's developer. Each request below needs approval. After each one,
sign in to the Admin Portal (`https://localhost:9443/admin`) as the bank admin and approve it under
**Tasks**.

1. **Sign up.** Go to the Developer Portal at `https://localhost:9443/devportal` and create an
   account. Approve it under **Tasks** → **User Creation**. Then sign in as the developer.
2. **Create the client application.** Approve it under **Tasks** → **Application Creation**.
3. **Subscribe** the client application to both APIs. Approve them under **Tasks** →
   **Subscription Creation**.
4. **Generate keys.** Open the client application, go to **Production Keys**, and enter:
   - **Grant types:** Code, Client Credentials and Refresh Token
   - **Callback URL:** `https://www.google.com`
   - **JWKS URI:** `http://localhost:8000/jwks.json`, from step 4

   Click **Generate Keys**. Approve it under **Tasks** → **Application Registration**.
5. Copy the **Consumer Key**. This is the client application's `client_id`.

## Step 14: Let the client application use the RAR types

1. Find the client application's ID in Identity Server. Sign in to the Console at
   `https://localhost:9446/console`, open the client application, and copy the ID from the URL.
2. Authorize it:

   ```bash
   cd <REPO>/artifacts/rar-schemas/scripts
   export IS_HOST=https://localhost:9446
   export IS_AUTH='is_admin@wso2.com:wso2123'

   APP_ID=<application-id> ./authorize-app.sh authorize
   ```

## Step 15: Call the APIs

1. **Create a customer.** In the Identity Server Console, go to **User Management** →
   **Users** → **Add User**. You sign in as this user to approve consents.
2. **Import** [the Postman collection](artifacts/postman-script) into Postman.
3. **Set up Postman:**
   - In **Settings** → **General**, turn off **SSL certificate verification**. The servers use
     self-signed certificates.
   - In **Settings** → **Certificates**, add the transport certificate (`transport.pem` and
     `transport.key` from step 4) for `localhost:9446` and `localhost:8243`.
4. **Fill in the collection variables:**

   | Variable | Value |
   |---|---|
   | `client_id` | The Consumer Key from step 13 |
   | `kid` | `client-signing-key` |
   | `jwk` | The contents of `jwk.json` from step 4 |
   | `pmlib_code` | The contents of [this JWT signing library](https://joolfe.github.io/postman-util-lib/dist/bundle.js) |

5. **Run the requests.** Run folder `0` once to get a client token. Then run each other folder's
   requests in order:
   1. **PAR** sends the consent.
   2. **Authorize** gives you a URL. Open it in a browser, sign in as the customer and approve
      the consent. You're sent to `https://www.google.com/?code=...`. Copy the `code` into the
      folder's `<type>_code` variable.
   3. **Token Exchange** swaps the code for an access token.
   4. The rest of the requests call the APIs.

   In the Accounts folder, run `DELETE /consents/{ConsentId}` last. It revokes the consent, so the
   account calls stop working after it.
