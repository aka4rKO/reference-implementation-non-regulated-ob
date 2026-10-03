# Open Banking reference implementation for non-regulated markets

An Open Banking standard for markets without an Open Banking regulation. It borrows ideas from the
UK, Australian, Berlin Group and US FDX standards, and runs on the
[WSO2 Open Banking Accelerator](https://ob.docs.wso2.com/en/latest/).

It covers two APIs:

- **Account Information**: read a customer's accounts, balances and transactions.
- **Payment Initiation**: domestic payments, domestic scheduled payments, domestic standing orders
  and international payments.

Consents are sent as [Rich Authorization Requests](https://datatracker.ietf.org/doc/html/rfc9396)
(`authorization_details`).

**Want to run it?** Let the [automated setup](setup/README.md) do it, or follow the
[tryout guide](TRYOUT.md) step by step.

## Actors

These docs use the terms in the first column. Other standards use different names for the same
actors.

| Actor | What they do | Also called |
|---|---|---|
| **Customer** | Owns the bank accounts. Signs in at the bank and approves or rejects each consent. | PSU, payment service user (UK, Berlin Group); consumer (Australia, FDX); end user |
| **Client application** | The third-party app that calls the APIs for the customer. It asks for consent, then reads account data or makes payments. | TPP, third-party provider; AISP and PISP (UK, Berlin Group); data recipient (Australia, FDX); API consumer |
| **Client application's developer** | The company or person that builds the client application. Signs up in the Developer Portal, creates the application, subscribes it to the APIs and generates its keys. | TPP (the organisation); subscriber (API Manager) |
| **Bank** | Holds the customer's accounts and runs the APIs. In this repo, the [demo bank backend](non-regulated-ob-demo-backend) stands in for it. | ASPSP, account servicing payment service provider (UK, Berlin Group); data holder (Australia); data provider (FDX); financial institution |
| **Bank admin** | Works for the bank. Approves developer sign-ups, client applications, subscriptions and keys in the API Manager Admin Portal. | Administrator |

## Products

| Product | Version |
|---|---|
| WSO2 API Manager | 4.7.0 |
| WSO2 Identity Server | 7.3.0 |
| WSO2 Financial Services Accelerator (`fsam` and `fsiam`) | 4.0 |

## What's in this repo

### Artifacts

Most of what you need to set things up is in [`artifacts/`](artifacts).

| Folder | What it's for |
|---|---|
| [`apis/`](artifacts/apis) | OpenAPI specs to publish in API Manager, and sample `authorization_details` for each consent type. [Read the README](artifacts/apis/README.md) to see which policies each endpoint needs. |
| [`rar-schemas/`](artifacts/rar-schemas) | A JSON Schema for each consent type, and [scripts](artifacts/rar-schemas/scripts/README.md) that register them in Identity Server. |
| [`devportal-km-configs/`](artifacts/devportal-km-configs) | Settings for client applications created in the Developer Portal. The client application's developer only enters its JWKS URI and callback URL, and selects the grant types. The rest are hidden defaults that apply the settings recommended by FAPI 2.0 and by this standard. See [Client application settings](#client-application-settings). |
| [`workflow-extensions/`](artifacts/workflow-extensions) | Turns on bank admin approval for developer sign-ups, client applications, subscriptions and keys. [`default-workflow-extensions.xml`](artifacts/workflow-extensions/default-workflow-extensions.xml) is the original, for reverting. |
| [`custom-synapse-error-formatter/`](artifacts/custom-synapse-error-formatter) | Formats gateway errors as `{Code, Message, Errors}`. |
| [`postman-script/`](artifacts/postman-script) | A Postman collection that runs every API's success flow. |

### Client application settings

[`am-deployment-snippet.toml`](artifacts/devportal-km-configs/am-deployment-snippet.toml) sets how
every client application is registered.

The client application's developer fills in only these fields when generating keys:

| Field | Why |
|---|---|
| JWKS URI | Where Identity Server gets the client application's public keys, to check its signed client assertions and request objects. The snippet adds this field. |
| Callback URL | Where the customer is sent back to after approving the consent |
| Grant types | Which tokens the client application can get: authorization code for user tokens, client credentials for client tokens, and refresh token to renew user tokens |

The rest are hidden, so every client application gets the settings recommended by FAPI 2.0 and by this standard:

| Setting | Value | What it means |
|---|---|---|
| `regulatory` | `true` | Treated as an Open Banking application |
| `token_endpoint_auth_method` | `private_key_jwt` | The client application proves who it is with a signed JWT, not a client secret |
| `token_endpoint_auth_signing_alg` | `PS256` | Algorithm for that JWT |
| `require_signed_request_object` | `true` | Authorization requests must be signed |
| `request_object_signing_alg` | `PS256` | Algorithm for the request object |
| `id_token_signed_response_alg` | `PS256` | Algorithm Identity Server signs ID tokens with |
| `require_pushed_authorization_requests` | `true` | Authorization requests must go through PAR (`/oauth2/par`) |
| `tls_client_certificate_bound_access_tokens` | `true` | Tokens are bound to the client application's mTLS certificate, so a stolen token can't be used without it |
| `ext_pkce_mandatory` | `true` | PKCE is required |
| `ext_pkce_support_plain` | `false` | Only `S256` PKCE is allowed, not `plain` |
| `ext_public_client` | `false` | The client application must always authenticate |
| `token_endpoint_allow_reuse_pvt_key_jwt` | `false` | Each client assertion JWT can be used only once |
| `ext_allowed_audience` | `organization` | The application's role audience in Identity Server: it uses organization roles, not application roles |

To change a default, or to let developers set it themselves, edit the snippet before adding it to
API Manager's `deployment.toml`.

### Code

| Module | What it is | Deployed to |
|---|---|---|
| [`non-regulated-ob-service-extension`](non-regulated-ob-service-extension) | The service extensions the accelerator calls at each consent step: showing the consent screen, saving the consent, checking API calls against it, revoking it, and formatting errors. | Identity Server |
| [`non-regulated-ob-demo-backend`](non-regulated-ob-demo-backend) | The demo bank backend, with sample accounts and payments. It knows nothing about consents. | API Manager |

Deployment settings, such as the demo bank backend URLs, payment limits and consent validity, are in
[`ConfigurableProperties.java`](non-regulated-ob-service-extension/src/main/java/org/wso2/non/regulated/ob/extensions/configurations/ConfigurableProperties.java).

## Build

You need Java 8 and Maven.

```bash
mvn clean install
```

This builds two WARs:

- `non-regulated-ob-service-extension/target/non#regulated#ob#service#extension.war`
- `non-regulated-ob-demo-backend/target/non#regulated#ob#demo#backend.war`

The `#` in each name becomes a `/` in its URL. For example, the service extension is served at
`/non/regulated/ob/service/extension`.

## License

[Apache License 2.0](LICENSE)
