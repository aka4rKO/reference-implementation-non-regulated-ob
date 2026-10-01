# What each phase does

Scripts are in `setup/phases/`. All of them read `setup/setup.env` through `setup/lib/common.sh`.
State that later phases need is kept in `setup/.state/` (git-ignored):

| File | Written by | Holds |
|---|---|---|
| `state.json` | phases 7, 9 | REST client, API IDs, client application ID, `client_id`, IS application ID |
| `client/` | phase 4 | `signing.key/.pem`, `transport.key/.pem`, `jwk.json` (private), `jwks.json` (public) |
| `certs/` | phase 4 | `is.pem`, `am.pem` |
| `postman/` | phase 10 | The configured Postman collection |

Ports: Identity Server `9446`, API Manager `9443`, gateway `8243`, demo bank backend `9763` (HTTP),
JWKS server `JWKS_PORT` (default `8000`).

## 1. extract — `01-extract.sh`

- Unzips `APIM_ZIP` and `IS_ZIP` into `WORK_DIR`, unless the homes already exist.
- Unzips the accelerators into `<APIM_HOME>` and `<IS_HOME>`.
- **Check:** `<APIM_HOME>/wso2-fsam-accelerator-4.0.0/bin/merge.sh` and
  `<IS_HOME>/wso2-fsiam-accelerator-4.0.0/bin/merge.sh` exist.

## 2. update — `02-update.sh` (run by the user)

- Runs `update_tool_setup.sh` if needed, then `wso2update_<os>_<arch>`, in each product's and each
  accelerator's `bin`. Needs a terminal.
- Must run **before** `accelerators`, so `merge.sh` copies the updated accelerator files.

## 3. accelerators — `03-accelerators.sh`

- Checks MySQL, copies `MYSQL_JDBC_JAR` into both `repository/components/lib`.
- Writes both `configure.properties`: hostnames, admin users, `PRODUCT_CONF_PATH` for the detected
  versions, database names `<DB_PREFIX>apimgtdb`, `<DB_PREFIX>identitydb` and so on.
- Runs `merge.sh` and `configure.sh` for both. `configure.sh` **drops and recreates the databases**
  and **replaces `deployment.toml`**.
- Creates the event notification tables in `<DB_PREFIX>consentdb`.
- **Check:** `mysql ... -e "SHOW DATABASES LIKE '<prefix>%'"` lists 7 databases.

## 4. certs — `04-certs.sh` (servers stopped)

- Replaces `wso2carbon` in `wso2carbon.p12` (IS) and `wso2carbon.jks` (APIM) with a new 10-year key
  for `OB_HOST`, `localhost` and `127.0.0.1`.
- Imports both certificates into both client truststores as `wso2is` and `wso2am`. Identity Server
  checks API Manager's signed consent validation requests against `wso2am` (`signature.alias`).
- Creates the client application's self-signed signing and transport keys (once), its JWK and JWKS
  (`setup/lib/jwk.py`, no Node needed), and imports the transport certificate into both
  truststores as `client-transport`.
- **Check:** `keytool -list -keystore <truststore> -storepass wso2carbon | grep -E 'wso2is|wso2am|client-transport'`.

## 5. deploy — `05-deploy.sh`

- `mvn clean install` with `BUILD_JAVA_HOME`, then copies the service extension WAR to IS and the
  demo bank backend WAR to APIM.
- Copies `customErrorFormatter.xml` to the gateway's `sequences` folder.
- Replaces `<APIM_HOME>/repository/resources/default-workflow-extensions.xml` with
  `artifacts/workflow-extensions/workflow-extensions.xml`. API Manager copies that file into its
  registry the **first time it starts with an empty database**, which turns approval on. If API
  Manager has already started once on these databases, see troubleshooting.
- Downloads `wso2is.notification.event.handlers-2.1.3.jar` into IS `dropins`.
- Edits both `deployment.toml` files (originals kept as `deployment.toml.orig`), exactly as
  `TRYOUT.md` step 6 lists. `setup/lib/edit.py` only touches the named keys.
- **Check:** `grep -n 'non/regulated/ob' <IS_HOME>/repository/conf/deployment.toml`.

## 6. start — `06-start.sh`

- Serves `setup/.state/client/` with `python3 -m http.server JWKS_PORT`, so `JWKS_URL` works.
- Starts IS with `wso2server.sh start`, waits for its well-known endpoint, then starts APIM with
  `api-manager.sh start` and waits for the Developer Portal API.
- **Check:** `curl -s http://localhost:8000/jwks.json`; both servers' logs show "started".

## 7. configure-apim — `07-configure-apim.sh` → `setup/lib/wso2.py`

- Registers a REST client (`/client-registration/v0.17/register`) and gets an admin token with a
  password grant.
- `key-manager`: adds `KEY_MANAGER_NAME` of type `fsKeyManager` pointing at IS, then disables the
  Resident Key Manager.
- `policies`: creates the common policies `MTLSEnforcementPolicy`, `ConsentEnforcementPolicy` and
  `DynamicEndpointPolicy` (v1) from `setup/templates/policies/*.json` and the accelerator's `.j2`
  files.
- `apis`: imports each spec with its context and version `v1.0`, sets Dynamic Endpoints, and adds
  the policies. MTLS goes at API level. Every operation gets the JWT claim policy (`aut`), then
  consent enforcement on user-token operations, then the dynamic endpoint. The token type of each
  operation comes from its security scheme in the spec. Then it creates a revision, deploys it to
  the `Default` gateway and publishes.
- **Check:** both APIs are `PUBLISHED` in the Publisher.

## 8. register-rar — `08-register-rar.sh`

- Runs `artifacts/rar-schemas/scripts/register.sh register` against IS, unless `verify` already
  lists `account_information_v1.0`.

## 9. onboard — `09-onboard-client.sh` → `setup/lib/wso2.py onboard`

- Self-signs-up `DEVELOPER_USERNAME` through `/api/identity/user/v1.0/me` and approves the
  `USER_SIGNUP` task.
- As the developer: creates `CLIENT_APP_NAME`, subscribes it to both APIs, and generates production
  keys with the authorization code, client credentials and refresh token grants, `CALLBACK_URL`
  and `jwks_uri = JWKS_URL`. After each request, approves the matching admin task
  (`APPLICATION_CREATION`, `SUBSCRIPTION_CREATION`, `APPLICATION_REGISTRATION_PRODUCTION`).
- Finds the IS application by `clientId` and runs `authorize-app.sh authorize` for it.
- Creates `CUSTOMER_USERNAME` in IS with SCIM2.

## 10. postman — `10-postman.sh` → `setup/lib/wso2.py postman`

- Copies the collection from `artifacts/postman-script/`, filling in `client_id`, `kid`
  (`client-signing-key`), `jwk`, `pmlib_code` (downloaded once), hosts, ports and `redirect_uri`.
- Writes it to `setup/.state/postman/`.
