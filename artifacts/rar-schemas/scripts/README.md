# Registering RAR types in WSO2 Identity Server

## What this is for

In this repo, consents are sent as `authorization_details`, using
**Rich Authorization Requests (RAR, RFC 9396)**. WSO2 Identity Server (IS 7.3) can check incoming
`authorization_details` on its authorize and token endpoints, but only for types it knows about.

The two scripts here tell IS about our types:

1. **`register.sh`** registers the types in IS.
2. **`authorize-app.sh`** allows an application to use them.

For background, see the [WSO2 RAR guide](https://is.docs.wso2.com/en/next/guides/authorization/rich-authorization-requests/).

## The types

The types are registered as two **API resources** in IS. Both are always registered together.

| API resource (identifier) | RAR type | Schema file |
|---|---|---|
| Account Information API (`account_information_api`) | `account_information_v1.0` | `account_information.schema.json` |
| Payments API (`payments_api`) | `domestic_payment_v1.0` | `domestic_payment.schema.json` |
| | `domestic_scheduled_payment_v1.0` | `domestic_scheduled_payment.schema.json` |
| | `domestic_standing_order_v1.0` | `domestic_standing_order.schema.json` |
| | `international_payment_v1.0` | `international_payment.schema.json` |

## Folder contents

```
rar-schemas/
  *.schema.json        one JSON Schema (Draft 2020-12) per type, covering the full shape
  scripts/
    types.json         groups the types into the two API resources, and gives each type
                       its name, description and schema file
    register.sh        step 1: registers the API resources
    authorize-app.sh   step 2: authorizes an application to use them
    README.md
```

The scripts find `types.json` next to themselves and the schema files one folder up, so you can
run them from any directory.

## Before you start

You need:

- `jq` and `curl` on your PATH
- a running WSO2 IS 7.3
- admin credentials for Basic auth (the default is `admin:admin`)

## Step 1: Register the types

```bash
./register.sh print      # dry run: shows the request bodies, calls nothing
./register.sh register   # registers both API resources in IS
./register.sh verify     # shows which types IS now advertises
```

How it works:

- `register` uses `jq` to build one request body per API resource and sends it to
  `POST /api/server/v1/api-resources` (the API Resource Management REST API).
- Each body has an `authorizationDetailsTypes[]` list. Every entry in it is
  `{type, name, description, schema}`, where `schema` is the type's JSON Schema.
- `verify` reads `GET /oauth2/token/.well-known/openid-configuration` and prints its
  `authorization_details_types_supported` list.

## Step 2: Authorize your application

Registering the types makes IS aware of them, but an application still can't request
`authorization_details` for them until you authorize it. **Run step 1 first.** This step depends on
the API resources already existing.

```bash
./authorize-app.sh print                               # dry run: shows the request bodies
APP_ID=<application-id> ./authorize-app.sh authorize   # authorizes the app
```

`print` doesn't change anything, but it still contacts IS to look up the API resource IDs, so IS
must be running and step 1 must be done.

`APP_ID` is required for `authorize`. It's the application's ID in WSO2 IS. You can find it:

- in the Console URL when you open the application, or
- with `GET /api/server/v1/applications?filter=name+eq+<app-name>`

How it works: the script looks up each API resource's ID by its identifier (from `types.json`). It
then authorizes the application for **both** API resources in one run, with the `RBAC` policy and
all of each resource's types. It does this through
`POST /api/server/v1/applications/{applicationId}/authorized-apis` (the Application Management
REST API).

## Using a different IS host or credentials

Both scripts read these environment variables:

| Variable | Default | Meaning |
|---|---|---|
| `IS_HOST` | `https://localhost:9443` | IS base URL |
| `IS_AUTH` | `admin:admin` | Basic auth as `user:password` |
| `APP_ID` | *(none)* | Application to authorize. Required for `authorize-app.sh authorize` |

The scripts call `curl` with `-k`, so they skip TLS certificate checks. That lets them work against
a local IS with a self-signed certificate.

```bash
IS_HOST=https://my-is-host:9443 IS_AUTH=admin:'S3cret!' ./register.sh register
IS_HOST=https://my-is-host:9443 IS_AUTH=admin:'S3cret!' APP_ID=<application-id> ./authorize-app.sh authorize
```
