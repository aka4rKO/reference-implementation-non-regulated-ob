# Automated setup

These scripts do everything in [TRYOUT.md](../TRYOUT.md) up to calling the APIs. When they finish,
you import the generated Postman collection and run it.

With Claude Code, ask it to **set up this repo**. It runs the
[`ob-setup` skill](../.claude/skills/ob-setup/SKILL.md), which asks for the inputs and runs the
phases below. Without it, follow this page.

## Before you start

You need:

- API Manager 4.7.0 and Identity Server 7.3.0 zips, and the `wso2-fsam-accelerator-4.0.0` and
  `wso2-fsiam-accelerator-4.0.0` zips
- A running MySQL 8, and the MySQL JDBC driver jar
- Java 8 and Maven, to build this repo, and a JDK supported by the servers
- `jq`, `curl`, `openssl`, `python3`, `unzip` and `keytool`

## Run it

```bash
cp setup/setup.env.example setup/setup.env   # then fill it in
setup/setup.sh extract
setup/setup.sh update                        # asks for your WSO2 login; skip if you have none
setup/setup.sh accelerators certs deploy start configure-apim register-rar onboard postman
```

`setup/setup.sh all` runs every phase except `update`.

> `accelerators` **drops and recreates** the MySQL databases whose names start with `DB_PREFIX`
> (default `nonreg_ob_`), and `certs` replaces both servers' keys.

Every phase can be run again on its own, for example `setup/setup.sh deploy`. Use
`setup/setup.sh stop` and `setup/setup.sh start` to stop and start the servers.

| Phase | Does | TRYOUT step |
|---|---|---|
| `extract` | Extracts the products and copies the accelerators into them | 1, 2 |
| `update` | WSO2 updates for products and accelerators | 1, 2 |
| `accelerators` | JDBC driver, `configure.properties`, `merge.sh`, `configure.sh` | 2 |
| `certs` | Server certificates, truststores, client application keys and JWKS | 3, 4 |
| `deploy` | Webapps, error formatter, approval workflow, `deployment.toml` changes | 5, 6, 9 |
| `start` | JWKS server, Identity Server, API Manager | 7 |
| `configure-apim` | Key manager, policies, both APIs published | 8, 10 |
| `register-rar` | RAR types in Identity Server | 11 |
| `onboard` | Developer sign-up, client application, subscriptions, keys, RAR authorization, customer | 12, 13, 14 |
| `postman` | Configured Postman collection | 14 |

## Then, in Postman

1. Import the collection from `setup/.state/postman/`.
2. **Settings** → **General**: turn off **SSL certificate verification**.
3. **Settings** → **Certificates**: add `setup/.state/client/transport.pem` and `transport.key` for
   `localhost:9446` and `localhost:8243`.
4. Run folder `0`, then each other folder in order. In the Authorize step, sign in as the customer
   (`CUSTOMER_USERNAME` / `CUSTOMER_PASSWORD` in `setup.env`).

## Files

| Path | What it is |
|---|---|
| `setup.env.example` | Settings to copy to `setup.env` (git-ignored) |
| `phases/` | One script per phase |
| `lib/common.sh` | Config loading and shared helpers |
| `lib/edit.py` | Idempotent edits to `configure.properties` and `deployment.toml` |
| `lib/jwk.py` | Turns the signing key into a JWK and JWKS |
| `lib/wso2.py` | REST calls for the key manager, policies, APIs, onboarding and Postman |
| `templates/` | Policy definitions and `deployment.toml` blocks |
| `.state/` | Generated keys, IDs and the Postman collection (git-ignored) |

If something fails, see the skill's [troubleshooting guide](../.claude/skills/ob-setup/troubleshooting.md).
