# Troubleshooting

Server logs: `<IS_HOME>/repository/logs/wso2carbon.log` and `<APIM_HOME>/repository/logs/wso2carbon.log`.

## Not yet tested end to end

These parts were written from the API Manager 4.7 REST API definitions and a working pack, but
haven't been run against fresh servers yet. If one fails, fix the script (or do the step by hand
from `TRYOUT.md`), and note what you found:

| Part | What to check if it fails |
|---|---|
| `wso2.py key-manager` payload | Compare with a key manager added by hand: `GET /api/am/admin/v4/key-managers/{id}` |
| `wso2.py policies` multipart upload | The field names are `policySpecFile` and `synapsePolicyDefinitionFile` |
| `wso2.py apis` policy wiring | `GET /api/am/publisher/v4/apis/{id}` on an API wired by hand, and compare `operations[].operationPolicies` and `apiPolicies` |
| Developer self sign-up | `POST https://<host>:9443/api/identity/user/v1.0/me`. If it is rejected, create the developer in the Carbon Console with the `Internal/subscriber` role instead |
| Workflow file loaded on first start | See "Requests are not waiting for approval" below |

## Phase 3: accelerators

- **`Cannot connect to MySQL`**: MySQL isn't running, or `DB_*` in `setup.env` is wrong.
- **`has no repository/resources/wso2am-<version>-deployment.toml`**: the accelerator doesn't
  support that product version. Use API Manager 4.7.0 and Identity Server 7.3.0, or an accelerator
  that lists your versions.
- **`sed: -e: No such file` or files ending in `-e`**: harmless on macOS. The accelerator's
  `configure.sh` uses GNU `sed -i -e`, which makes backup files on macOS.

## Phase 4: certs

- **`Stop the servers first`**: run `setup/setup.sh stop`, then `certs` again, then `start`.
- **Keystore password errors**: the phase assumes the default `wso2carbon` password.

## Phase 5: deploy

- **Maven fails in checkstyle or findbugs**: the build needs Java 8. Set `BUILD_JAVA_HOME`.
- **Edits missing after re-running `accelerators`**: `configure.sh` replaced `deployment.toml`.
  Run `deploy` again.

## Phase 6: start

- **Times out**: read the end of the server log.
  - `Address already in use`: another server is using 9443, 9446 or 8243. Stop it.
  - `Communications link failure`: MySQL is down.
  - TLS or `PKIX` errors between the servers: run `certs` again, with the servers stopped.
- **JWKS server didn't start**: something else uses `JWKS_PORT`. Change it in `setup.env`, then run
  `certs` and `start` again. The JWKS URL is saved on the client application, so if keys were
  already generated, the client application must be recreated.

## Phase 7: configure-apim

- **`invalid_grant` or 401 on `/oauth2/token`**: wrong `AM_ADMIN_*`, or the password grant is off.
  Delete `rest_client_*` from `setup/.state/state.json` and run again.
- **Key manager rejected (400)**: read the message, then compare the payload in `wso2.py` with
  `TRYOUT.md` step 8. Usually a connector field changed name.
- **Policy `jwtClaimBasedAccessValidator v1 not found`**: API Manager loads its default policies on
  first start. Check `GET /api/am/publisher/v4/operation-policies` for the version it has, and
  update `wso2.py`.
- **Revision limit reached**: each run of the phase creates a new revision of each API, and API
  Manager keeps a limited number (5 by default). Delete old revisions in the Publisher
  (**Deployments**), then run the phase again.
- **API import 409**: an API with the same name or context exists. The phase reuses an API with the
  same name and version. Otherwise delete the old one in the Publisher.

## Phase 9: onboard

- **Requests are not waiting for approval** (the application is approved straight away): API
  Manager started on these databases before `deploy` replaced
  `default-workflow-extensions.xml`, so the registry still has the default file. Either:
  - edit `/_system/governance/apimgt/applicationdata/workflow-extensions.xml` in the Carbon
    Console (`https://<host>:9443/carbon` → **Main** → **Registry** → **Browse**), pasting
    `artifacts/workflow-extensions/workflow-extensions.xml`; or
  - run `accelerators`, `deploy`, `start` again for fresh databases.
  Without approval the phase still works; it just finds nothing to approve.
- **Developer token fails after sign-up**: the `USER_SIGNUP` task wasn't approved yet. Run the phase
  again; it approves pending tasks.
- **`No Identity Server application found for client_id`**: key generation failed on the IS side.
  Check the IS log, and that the key manager is enabled and the Resident Key Manager disabled.
- **`authorize-app.sh` fails**: run `register-rar` first. If the error says the API is already
  authorized, that's fine.

## Postman

- **SSL or certificate errors**: turn off SSL certificate verification, and check the client
  certificate is set for both `<host>:9446` and `<host>:8243`.
- **`invalid_client` on PAR or token**: the JWKS server isn't running (`setup/setup.sh start`), or
  the `kid` doesn't match `jwks.json`.
- **Authorize step code expired**: authorization codes last 50 seconds (FAPI 2.0). Copy the code and
  run Token Exchange straight away.
