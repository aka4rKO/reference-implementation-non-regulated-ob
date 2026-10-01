---
name: ob-setup
description: Set up the whole non-regulated Open Banking reference implementation on this machine (API Manager 4.7, Identity Server 7.3, the Financial Services accelerators, certificates, APIs, client application onboarding) so the user only has to import the Postman collection and call the APIs. Use when someone asks to set up, install, try out or run this repo, or to re-run or fix one part of the setup.
---

# Set up the reference implementation

The work is done by the scripts in `setup/`. Your job is to collect the inputs, run the phases in
order, check each one, and fix what breaks. Don't redo by hand what a phase does.

`TRYOUT.md` is the manual version of the same steps. Use it to understand a step, not to replace
the scripts.

## 1. Check the machine

Run these and report anything missing, with how to install it:

```bash
for c in java mvn mysql jq curl openssl python3 unzip keytool; do command -v $c >/dev/null && echo "ok $c" || echo "MISSING $c"; done
java -version 2>&1 | head -1
mysql --version
```

- Building this repo needs **Java 8** (`BUILD_JAVA_HOME`). The servers need a JDK that API Manager
  4.7 and Identity Server 7.3 support (`SERVER_JAVA_HOME`).
- MySQL must be running. Check with `mysql -u<user> -p<pass> -h<host> -e "SELECT 1"` once you
  have the credentials.

## 2. Collect the inputs and write `setup/setup.env`

If `setup/setup.env` exists, read it and confirm the values with the user. Otherwise copy
`setup/setup.env.example` and fill it in. Ask the user for:

| Setting | Ask for |
|---|---|
| `APIM_ZIP` or `APIM_HOME` | The API Manager 4.7 zip, or an already extracted folder |
| `IS_ZIP` or `IS_HOME` | The Identity Server 7.3 zip, or an already extracted folder |
| `FSAM_ZIP`, `FSIAM_ZIP` | The two accelerator 4.0 zips |
| `MYSQL_JDBC_JAR` | The MySQL connector jar |
| `WORK_DIR` | Where to extract the products (default `~/nonreg-ob`) |
| `DB_HOST`, `DB_USER`, `DB_PASS` | MySQL login |
| `SERVER_JAVA_HOME`, `BUILD_JAVA_HOME` | Only if their shell's `JAVA_HOME` isn't right for that job |

Check every path exists before you write the file. Keep the defaults for the rest unless the user
asks. `setup.env` holds passwords and is git-ignored; never commit it.

**Before any phase runs, tell the user plainly and get a yes:**
- `accelerators` drops and recreates the MySQL databases starting with `DB_PREFIX`
  (default `nonreg_ob_`). List them: `mysql ... -e "SHOW DATABASES LIKE '<prefix>%'"`.
- `certs` replaces both servers' `wso2carbon` keys.

## 3. Run the phases

Run each phase from the repo root with `setup/setup.sh <phase>`, one at a time, and read its
output before going on. Every phase can be run again safely.

| # | Phase | What it does | Notes |
|---|---|---|---|
| 1 | `extract` | Extracts the products and copies the accelerators into them | |
| 2 | `update` | WSO2 updates for products and accelerators | **The user runs it**, see below |
| 3 | `accelerators` | JDBC driver, `configure.properties`, `merge.sh`, `configure.sh` | Drops the databases |
| 4 | `certs` | New server certificates, truststores, client application keys and JWKS | Servers must be stopped |
| 5 | `deploy` | Builds and deploys the webapps, error formatter, approval workflow, toml changes | Run again after every `accelerators` |
| 6 | `start` | JWKS server, then Identity Server, then API Manager | Takes several minutes; run it in the background |
| 7 | `configure-apim` | Key manager, the three policies, both APIs imported, wired and published | |
| 8 | `register-rar` | Registers the RAR types in Identity Server | |
| 9 | `onboard` | Developer sign-up, client application, subscriptions, keys (each approved), RAR authorization, customer user | |
| 10 | `postman` | Writes the configured Postman collection | |

**Updates (phase 2):** the update tool asks for the user's WSO2 subscription login, so you can't run
it. After `extract`, ask the user to run `! setup/setup.sh update` in this session. If they have no
subscription, they can skip it.

**Start (phase 6):** run `setup/setup.sh start` with `run_in_background`, because it waits up to 15
minutes per server. Wait for the notification; don't poll.

For what each phase changes and how to check it worked, see [phases.md](phases.md).

## 4. When a phase fails

1. Read the error. For server problems, read the end of the server log:
   `tail -200 <IS_HOME or APIM_HOME>/repository/logs/wso2carbon.log`.
2. Look the symptom up in [troubleshooting.md](troubleshooting.md).
3. Fix the cause, then re-run only that phase.
4. If the fix belongs in the scripts (a wrong payload, a changed API), fix the script, say what
   you changed, and re-run. Keep `TRYOUT.md` in step with any change in behaviour.

Phases 7 and 9 call the API Manager and Identity Server REST APIs. If a call keeps failing and
can't be fixed, do that step by hand from `TRYOUT.md` (steps 8–13), then carry on with the next
phase.

## 5. Finish

When `postman` has run, tell the user:

- where the configured collection is (`setup/.state/postman/`);
- the two Postman settings they must change themselves: turn off SSL certificate verification,
  and add the client certificate (`setup/.state/client/transport.pem` and `transport.key`) for
  `<host>:9446` and `<host>:8243`;
- the customer login for the Authorize step (`CUSTOMER_USERNAME` / `CUSTOMER_PASSWORD`);
- that `setup/setup.sh stop` stops everything and `setup/setup.sh start` starts it again.

Don't print passwords other than the customer's. Never commit anything.
