# Try out the Accounts and Payments APIs

This guide sets up everything on one machine and takes you through a full Accounts and Payments
flow. For what's in the repo, see the [README](README.md).

## Contents

- [Before you start](#before-you-start)
- **Set up the servers**
  1. [Set up API Manager and Identity Server](#step-1-set-up-api-manager-and-identity-server)
  2. [Set up the accelerators](#step-2-set-up-the-accelerators)
  3. [Exchange certificates between the servers](#step-3-exchange-certificates-between-the-servers)
  4. [Create the TPP's keys and certificates](#step-4-create-the-tpps-keys-and-certificates)
  5. [Build and deploy the webapps](#step-5-build-and-deploy-the-webapps)
  6. [Add the configuration](#step-6-add-the-configuration): [Identity Server](#identity-server),
     [API Manager](#api-manager)
  7. [Start the servers](#step-7-start-the-servers)
- **Configure API Manager and Identity Server**
  8. [Add the key manager](#step-8-add-the-key-manager)
  9. [Turn on the approval workflow](#step-9-turn-on-the-approval-workflow)
  10. [Publish the APIs](#step-10-publish-the-apis)
  11. [Register the RAR types in Identity Server](#step-11-register-the-rar-types-in-identity-server)
- **Onboard the TPP and call the APIs**
  12. [Onboard the TPP](#step-12-onboard-the-tpp)
  13. [Let the application use the RAR types](#step-13-let-the-application-use-the-rar-types)
  14. [Call the APIs](#step-14-call-the-apis)

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

## Step 4: Create the TPP's keys and certificates

The TPP (the third-party app calling the APIs) needs two key pairs:

- **Signing:** signs the TPP's JWTs. Identity Server checks them with the TPP's public keys, which
  it reads from the TPP's JWKS URL.
- **Transport:** the client certificate for mTLS.

> **For testing only.** These certificates are self-signed. A real TPP needs certificates issued by
> a trusted certificate authority.

1. Create the keys and certificates in a folder of your choice:

   ```bash
   openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=tpp-signing" \
     -keyout signing.key -out signing.pem
   openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=tpp-transport" \
     -keyout transport.key -out transport.pem
   ```

2. Turn the signing key into a JWK (`jwk.json`, private, used by Postman) and a JWKS
   (`jwks.json`, public, served to Identity Server):

   ```bash
   node -e '
   const c = require("crypto"), fs = require("fs");
   const key = c.createPrivateKey(fs.readFileSync("signing.key"));
   const meta = { kid: "tpp-signing-key", alg: "PS256", use: "sig" };
   fs.writeFileSync("jwk.json", JSON.stringify({ ...key.export({ format: "jwk" }), ...meta }));
   fs.writeFileSync("jwks.json", JSON.stringify({ keys: [{ ...c.createPublicKey(key).export({ format: "jwk" }), ...meta }] }, null, 2));
   '
   ```

3. Serve the JWKS, and leave it running:

   ```bash
   python3 -m http.server 8000
   ```

   The TPP's JWKS URL is now `http://localhost:8000/jwks.json`.

4. Make both servers trust the transport certificate. Do this **before** you start the servers:

   ```bash
   keytool -import -noprompt -trustcacerts -alias tpp-transport -file transport.pem \
     -keystore <APIM_HOME>/repository/resources/security/client-truststore.jks -storepass wso2carbon
   keytool -import -noprompt -trustcacerts -alias tpp-transport -file transport.pem \
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
- The **demo backend** goes to API Manager. It is the mock bank the APIs route to.

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

**2. Consent settings**

```toml
[financial_services.consent.pre_initiated]
scopes = []

[financial_services.consent.scope_based]
scopes = ["accounts"]

[financial_services.consent.validation]
signature.alias = "wso2am"   # API Manager's certificate, from step 3
```

**3. Other settings**

```toml
[oauth.oidc]
enable_claims_separation_for_access_tokens = false

[oauth.dcr]
# Comment out this line:
#enable_fapi_enforcement = true

[financial_services.app_registration.sca.authenticator_config]
enable_setting_authenticators_on_app_update = false
```

**4. Use Identity Server as API Manager's key manager**

```toml
[oauth]
authorize_all_scopes = true

[[event_listener]]
id = "token_revocation"
type = "org.wso2.carbon.identity.core.handler.AbstractIdentityHandler"
name = "org.wso2.is.notification.ApimOauthEventInterceptor"
order = 1

[event_listener.properties]
notification_endpoint = "https://localhost:9443/internal/data/v1/notify"
username = "${admin.username}"
password = "${admin.password}"
'header.X-WSO2-KEY-MANAGER' = "WSO2-IS"
```

Also download
[`wso2is.notification.event.handlers-2.1.3.jar`](https://maven.wso2.org/nexus/content/repositories/releases/org/wso2/km/ext/wso2is/wso2is.notification.event.handlers/2.1.3/wso2is.notification.event.handlers-2.1.3.jar)
into `<IS_HOME>/repository/components/dropins`.

See [Configure WSO2 IS 7 as a key manager](https://apim.docs.wso2.com/en/latest/api-security/key-management/third-party-key-managers/configure-wso2is7-connector/).

**5. Make Identity Server FAPI 2.0 compliant**

```toml
[oauth]
timestamp_skew = 10

[oauth.token_validation]
authorization_code_validity = 50

[oauth.oidc]
fapi.version = "2"
id_token.issuer = "https://$ref{server.hostname}:${carbon.management.port}/oauth2/oidcdiscovery"
id_token.use_entityid_as_issuer = true

[oauth.mutualtls]
client_certificate_header = "x-wso2-mtls-cert"

[transport.https.sslHostConfig.properties]
ciphers = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"

[myaccount.idp_configs]
wellKnownEndpoint = "https://localhost:9446/oauth2/token/.well-known/openid-configuration"

[console.idp_configs]
wellKnownEndpoint = "https://localhost:9446/oauth2/token/.well-known/openid-configuration"
```

See [Register a FAPI-compliant app](https://is.docs.wso2.com/en/7.1.0/guides/applications/register-a-fapi-compliant-app/).

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

**3. Set up the TPP application fields**

- Replace all the `[[financial_services.keymanager.application.type.attributes]]` blocks with
  [`am-deployment-snippet.toml`](artifacts/devportal-km-configs/am-deployment-snippet.toml). This
  adds the **JWKS URI** field to the Developer Portal.
- Remove this block:

  ```toml
  [financial_services.gateway.application_registration]
  tls_client_certificate_bound_access_tokens = true
  ```

**4. Use Identity Server as the key manager**

```toml
[[apim.tenant_sharing]]
type = "WSO2-IS-7"

[apim.tenant_sharing.properties]
identity_server_base_url = "https://localhost:9446"
dcr_request_extension = "org.wso2.financial.services.accelerator.keymanager.extension.FSDCRRequestExtension"
```

See [Configure WSO2 IS 7 as a key manager](https://apim.docs.wso2.com/en/latest/api-security/key-management/third-party-key-managers/configure-wso2is7-connector/).

**5. Other settings**

```toml
[apim.key_manager]
allow_subscription_validation_disabling = false

[encryption]
key = "<your key>"   # create one with: openssl rand -hex 32

[transport.https.properties]
maxHttpHeaderSize = "65536"

[system.parameter]
disableRoleValidationAtScopeCreation = "true"
```

## Step 7: Start the servers

Start Identity Server first, then API Manager:

```bash
<IS_HOME>/bin/wso2server.sh
<APIM_HOME>/bin/api-manager.sh
```

## Step 8: Add the key manager

This makes API Manager use Identity Server to issue and check tokens.

1. Sign in to the Admin Portal at `https://localhost:9443/admin`.
2. Go to **Key Managers** → **Add Key Manager**.
3. Enter a name, and choose **fsKeyManager** as the type.
4. Set the **Well-known URL** to
   `https://localhost:9446/oauth2/token/.well-known/openid-configuration` and click **Import**.
   This fills in most fields. Check that the **Issuer** is `https://localhost:9446/oauth2/token`.
5. Under **Connector Configurations**, enter the Identity Server admin username and password.
6. Set **Key Manager Permission** to **Public**.
7. Under **Advanced Configuration**:
   - tick **Token Generation**, **Out Of Band Provisioning** and **OAuth App Creation**
   - set **Token Validation Method** to **Self Validate JWT**
8. Click **Add**.
9. Disable the **Resident Key Manager**.

For every field, see
[Configure IS as Key Manager](https://github.com/wso2/docs-open-banking/blob/master/en/docs/tryout-flows/accelerator-with-is-and-apim/configure-fskm.md)
and [Configure WSO2 IS 7 as a key manager](https://apim.docs.wso2.com/en/latest/api-security/key-management/third-party-key-managers/configure-wso2is7-connector/).

## Step 9: Turn on the approval workflow

With approval on, the bank approves each TPP application, subscription and key request.

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

Do this for each API:

| API | OpenAPI spec |
|---|---|
| Account Information | [`account-info-openapi.yaml`](artifacts/apis/accounts/account-info-openapi.yaml) |
| Payment Initiation | [`payment-initiation-openapi.yaml`](artifacts/apis/payments/payment-initiation-openapi.yaml) |

1. Sign in to the Publisher at `https://localhost:9443/publisher`.
2. Go to **REST API** → **Import Open API**, and upload the spec.
3. Leave the endpoint empty, and click **Create**.
4. Under **Endpoints**, choose **Dynamic Endpoints** and save.
5. Under **Policies**, add these policies in this order:

   | Policy | Where |
   |---|---|
   | mTLS | Every endpoint |
   | JWT claim based access | Every endpoint. Use the claim `aut`, with `APPLICATION_USER` for user-token endpoints and `APPLICATION` for client-token endpoints. |
   | Consent enforcement | User-token endpoints only |
   | Dynamic endpoint | Every endpoint, always last |

   The [APIs README](artifacts/apis/README.md) lists which endpoints take which token.

6. Deploy the API, then go to **Overview** and click **Publish**.

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

## Step 12: Onboard the TPP

Now act as the TPP. Each request below needs approval, so after each one, sign in to the Admin
Portal (`https://localhost:9443/admin`) and approve it under **Tasks**.

1. Go to the Developer Portal at `https://localhost:9443/devportal`, create an account and sign in.
2. **Create an application.** Approve it under **Tasks** → **Application Creation**.
3. **Subscribe** the application to both APIs. Approve them under **Tasks** →
   **Subscription Creation**.
4. **Generate keys.** Open the application, go to **Production Keys**, and enter:
   - **Grant types:** Code and Client Credentials
   - **Callback URL:** `https://www.google.com`
   - **JWKS URI:** `http://localhost:8000/jwks.json`, from step 4

   Click **Generate Keys**. Approve it under **Tasks** → **Application Registration**.
5. Copy the **Consumer Key**. This is the TPP's `client_id`.

## Step 13: Let the application use the RAR types

1. Find the application's ID in Identity Server. Sign in to the Console at
   `https://localhost:9446/console`, open the application, and copy the ID from the URL.
2. Authorize it:

   ```bash
   cd <REPO>/artifacts/rar-schemas/scripts
   export IS_HOST=https://localhost:9446
   export IS_AUTH='is_admin@wso2.com:wso2123'

   APP_ID=<application-id> ./authorize-app.sh authorize
   ```

## Step 14: Call the APIs

1. **Create a bank customer.** In the Identity Server Console, go to **User Management** →
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
   | `client_id` | The Consumer Key from step 12 |
   | `kid` | `tpp-signing-key` |
   | `jwk` | The contents of `jwk.json` from step 4 |
   | `pmlib_code` | The contents of [this JWT signing library](https://joolfe.github.io/postman-util-lib/dist/bundle.js) |

5. **Run the requests.** Run folder `0` once to get a client token. Then run each other folder's
   requests in order:
   1. **PAR** sends the consent.
   2. **Authorize** gives you a URL. Open it in a browser, sign in as the bank customer and approve
      the consent. You're sent to `https://www.google.com/?code=...`. Copy the `code` into the
      folder's `<type>_code` variable.
   3. **Token Exchange** swaps the code for an access token.
   4. The rest of the requests call the APIs.

   In the Accounts folder, run `DELETE /consents/{ConsentId}` last. It revokes the consent, so the
   account calls stop working after it.
