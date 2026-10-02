#!/usr/bin/env python3
"""
REST calls against API Manager 4.7 and Identity Server 7.3, used by the setup phases.

  wso2.py key-manager     add the fsKeyManager key manager
  wso2.py policies        add the accelerator's MTLS, Consent Enforcement and Dynamic Endpoint policies
  wso2.py apis            import, wire up, deploy and publish both APIs
  wso2.py consent-access  give the IS admin the scope the consent APIs need
  wso2.py onboard         sign up the developer, create the client application, subscribe, generate
                          keys (approving each request), and create the customer
  wso2.py postman         write a Postman collection with the client application's values filled in

Every command reads its settings from the environment (see setup.env and lib/common.sh) and is
safe to run again: it skips anything that already exists.
"""

import base64
import json
import os
import ssl
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import re
import uuid

E = os.environ
STATE_FILE = os.path.join(E.get("STATE_DIR", "."), "state.json")
SSL = ssl._create_unverified_context()  # the servers use self-signed certificates

APIM = E.get("APIM_URL", "https://localhost:9443")
IS = E.get("IS_URL", "https://localhost:9446")
PUBLISHER = f"{APIM}/api/am/publisher/v4"
ADMIN = f"{APIM}/api/am/admin/v4"
DEVPORTAL = f"{APIM}/api/am/devportal/v3"

APIS = [
    {
        "key": "accounts",
        "name": "AccountInformationAPI",
        "version": "v1.0",
        "context": "/open-banking/{version}/account-information",
        "spec": "artifacts/apis/accounts/account-info-openapi.yaml",
        "backend": "/non/regulated/ob/demo/backend/services/accounts/accountservice",
    },
    {
        "key": "payments",
        "name": "PaymentInitiationAPI",
        "version": "v1.0",
        "context": "/open-banking/{version}/payment-initiation",
        "spec": "artifacts/apis/payments/payment-initiation-openapi.yaml",
        "backend": "/non/regulated/ob/demo/backend/services/payments/paymentservice",
    },
]

# Requests to these paths go to the accelerator's consent service; everything else to the bank.
CONSENT_ROUTING_REGEX = r".*\/consents.*"

POLICIES = [
    ("MTLSEnforcementPolicy", "mtls-enforcement-policy.json", "mtlsEnforcementPolicy.j2"),
    ("ConsentEnforcementPolicy", "consent-enforcement-policy.json", "consentEnforcementPolicy.j2"),
    ("DynamicEndpointPolicy", "dynamic-endpoint-policy.json", "dynamicEndpointPolicy.j2"),
]

ADMIN_SCOPES = " ".join([
    "apim:admin", "apim:admin_operations", "apim:keymanagers_manage",
    "apim:api_workflow_view", "apim:api_workflow_approve",
    "apim:api_view", "apim:api_create", "apim:api_manage", "apim:api_publish",
    "apim:common_operation_policy_view", "apim:common_operation_policy_manage",
])
DEVELOPER_SCOPES = "apim:subscribe apim:app_manage apim:sub_manage"


# --------------------------------------------------------------------------------------------
# HTTP
# --------------------------------------------------------------------------------------------

class HttpError(Exception):
    def __init__(self, method, url, status, body):
        super().__init__(f"{method} {url} -> HTTP {status}: {body[:800]}")
        self.status = status
        self.body = body


def basic(user, password):
    return "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode()


def call(method, url, auth=None, json_body=None, form=None, files=None, headers=None, ok=(200, 201, 202, 204)):
    """Send a request. Returns parsed JSON (or text, or None). Raises HttpError on other codes."""
    data, hdrs = None, {"Accept": "application/json"}
    if auth:
        hdrs["Authorization"] = auth
    if json_body is not None:
        data = json.dumps(json_body).encode()
        hdrs["Content-Type"] = "application/json"
    elif form is not None:
        data = urllib.parse.urlencode(form).encode()
        hdrs["Content-Type"] = "application/x-www-form-urlencoded"
    elif files is not None:
        boundary = uuid.uuid4().hex
        parts = []
        for field, (filename, content, ctype) in files.items():
            head = f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"'
            if filename:
                head += f'; filename="{filename}"'
            head += f"\r\nContent-Type: {ctype}\r\n\r\n"
            parts.append(head.encode() + content + b"\r\n")
        data = b"".join(parts) + f"--{boundary}--\r\n".encode()
        hdrs["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    hdrs.update(headers or {})
    req = urllib.request.Request(url, data=data, method=method, headers=hdrs)
    try:
        with urllib.request.urlopen(req, context=SSL, timeout=120) as resp:
            status, body = resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        status, body = e.code, e.read().decode(errors="replace")
    if status not in ok:
        raise HttpError(method, url, status, body)
    if not body:
        return None
    try:
        return json.loads(body)
    except ValueError:
        return body


def log(msg):
    print(f"\033[1;34m==>\033[0m {msg}", flush=True)


def done(msg):
    print(f"\033[1;32m ok\033[0m {msg}", flush=True)


def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE) as f:
            return json.load(f)
    return {}


def save_state(**values):
    state = load_state()
    state.update(values)
    with open(STATE_FILE, "w") as f:
        json.dump(state, f, indent=2)


# --------------------------------------------------------------------------------------------
# Tokens
# --------------------------------------------------------------------------------------------

def rest_client():
    """Register (once) the OAuth client the setup uses to call the API Manager REST APIs."""
    state = load_state()
    if state.get("rest_client_id"):
        return state["rest_client_id"], state["rest_client_secret"]
    body = {
        "callbackUrl": "https://localhost",
        "clientName": "nonreg_ob_setup",
        "owner": E["AM_ADMIN_USERNAME"],
        "grantType": "client_credentials password refresh_token",
        "saasApp": True,
    }
    res = call("POST", f"{APIM}/client-registration/v0.17/register",
               auth=basic(E["AM_ADMIN_USERNAME"], E["AM_ADMIN_PASSWORD"]), json_body=body)
    save_state(rest_client_id=res["clientId"], rest_client_secret=res["clientSecret"])
    return res["clientId"], res["clientSecret"]


def token(username, password, scopes):
    form = {"grant_type": "password", "username": username, "password": password, "scope": scopes}
    cid, secret = rest_client()
    try:
        res = call("POST", f"{APIM}/oauth2/token", auth=basic(cid, secret), form=form)
    except HttpError as e:
        if e.status not in (400, 401) or "invalid_client" not in e.body:
            raise
        # The saved client is no longer valid: register a new one and try again.
        save_state(rest_client_id=None, rest_client_secret=None)
        cid, secret = rest_client()
        res = call("POST", f"{APIM}/oauth2/token", auth=basic(cid, secret), form=form)
    return "Bearer " + res["access_token"]


def admin_token():
    return token(E["AM_ADMIN_USERNAME"], E["AM_ADMIN_PASSWORD"], ADMIN_SCOPES)


def developer_token():
    return token(E["DEVELOPER_USERNAME"], E["DEVELOPER_PASSWORD"], DEVELOPER_SCOPES)


def is_admin():
    return basic(E["IS_ADMIN_USERNAME"], E["IS_ADMIN_PASSWORD"])


# --------------------------------------------------------------------------------------------
# Key manager
# --------------------------------------------------------------------------------------------

def is_discovery():
    """Identity Server's issuer and supported grant types, from its discovery document."""
    res = call("GET", f"{IS}/oauth2/token/.well-known/openid-configuration")
    issuer, grants = res.get("issuer"), res.get("grant_types_supported") or []
    if not issuer or not grants:
        sys.exit("Identity Server's discovery document has no issuer or grant_types_supported.")
    return issuer, grants


def cmd_key_manager():
    auth = admin_token()
    name = E.get("KEY_MANAGER_NAME", "FSKM")
    existing = call("GET", f"{ADMIN}/key-managers", auth=auth).get("list", [])
    issuer, grants = is_discovery()

    km = next((k for k in existing if k["name"] == name), None)
    if km:
        done(f"Key manager '{name}' already exists")
        full = call("GET", f"{ADMIN}/key-managers/{km['id']}", auth=auth)
        changed = []
        if full.get("issuer") != issuer:
            full["issuer"] = issuer
            changed.append(f"issuer {issuer}")
        if sorted(full.get("availableGrantTypes") or []) != sorted(grants):
            full["availableGrantTypes"] = grants
            changed.append(f"all {len(grants)} Identity Server grant types")
        if changed:
            call("PUT", f"{ADMIN}/key-managers/{km['id']}", auth=auth, json_body=full)
            done(f"Key manager '{name}' updated: " + ", ".join(changed))
    else:
        log(f"Adding key manager '{name}' (fsKeyManager)")
        body = {
            "name": name,
            "displayName": name,
            "type": "fsKeyManager",
            "description": "Identity Server 7.3 with the Financial Services accelerator",
            "enabled": True,
            "wellKnownEndpoint": f"{IS}/oauth2/token/.well-known/openid-configuration",
            "issuer": issuer,
            "clientRegistrationEndpoint": f"{IS}/api/identity/oauth2/dcr/v1.1/register",
            "introspectionEndpoint": f"{IS}/oauth2/introspect",
            "tokenEndpoint": f"{IS}/oauth2/token",
            "displayTokenEndpoint": f"{IS}/oauth2/token",
            "revokeEndpoint": f"{IS}/oauth2/revoke",
            "displayRevokeEndpoint": f"{IS}/oauth2/revoke",
            "userInfoEndpoint": f"{IS}/scim2/Me",
            "authorizeEndpoint": f"{IS}/oauth2/authorize",
            "scopeManagementEndpoint": f"{IS}/api/identity/oauth2/v1.0/scopes",
            "certificates": {"type": "JWKS", "value": f"{IS}/oauth2/jwks"},
            "availableGrantTypes": grants,
            "enableTokenGeneration": True,
            "enableMapOAuthConsumerApps": True,
            "enableOAuthAppCreation": True,
            "enableSelfValidationJWT": True,
            "consumerKeyClaim": "azp",
            "scopesClaim": "scope",
            "tokenValidation": [{"id": 1, "enable": True, "type": "JWT", "value": {"body": {}}}],
            "permissions": {"permissionType": "PUBLIC", "roles": []},
            "tokenType": "DIRECT",
            "additionalProperties": {
                "Username": E["IS_ADMIN_USERNAME"],
                "Password": E["IS_ADMIN_PASSWORD"],
                "api_resource_management_endpoint": f"{IS}/api/server/v1/api-resources",
                "is7_roles_endpoint": f"{IS}/scim2/v2/Roles",
                "self_validate_jwt": True,
            },
        }
        call("POST", f"{ADMIN}/key-managers", auth=auth, json_body=body)
        done(f"Added key manager '{name}'")


# --------------------------------------------------------------------------------------------
# Policies
# --------------------------------------------------------------------------------------------

def common_policies(auth):
    res = call("GET", f"{PUBLISHER}/operation-policies?limit=1000", auth=auth)
    return {(p["name"], p["version"]): p["id"] for p in res.get("list", [])}


def cmd_policies():
    auth = admin_token()
    existing = common_policies(auth)
    policy_dir = os.path.join(E["FSAM_HOME"], "repository", "resources", "mediation-policies")
    spec_dir = os.path.join(E["SETUP_DIR"], "templates", "policies")
    for name, spec_file, j2_file in POLICIES:
        if (name, "v1") in existing:
            done(f"Policy {name} already exists")
            continue
        log(f"Adding policy {name}")
        with open(os.path.join(spec_dir, spec_file), "rb") as f:
            spec = f.read()
        with open(os.path.join(policy_dir, j2_file), "rb") as f:
            j2 = f.read()
        call("POST", f"{PUBLISHER}/operation-policies", auth=auth, files={
            "policySpecFile": (spec_file, spec, "application/json"),
            "synapsePolicyDefinitionFile": (j2_file, j2, "application/octet-stream"),
        })
        done(f"Added policy {name}")


# --------------------------------------------------------------------------------------------
# APIs
# --------------------------------------------------------------------------------------------

def operation_token_types(spec_path):
    """{(VERB, path): 'user' | 'client'} from each operation's security scheme in the spec.

    Reads the YAML as text, so no YAML library is needed: paths are indented 2 spaces, verbs 4.
    """
    types, path, verb = {}, None, None
    with open(spec_path) as f:
        for line in f:
            m = re.match(r"^  (/[^:]*):\s*$", line)
            if m:
                path, verb = m.group(1), None
                continue
            m = re.match(r"^    (get|post|put|delete|patch):\s*$", line)
            if m and path:
                verb = m.group(1).upper()
                types[(verb, path)] = "user"
                continue
            if re.match(r"^\S", line):
                path, verb = None, None
            elif verb and "ClientOAuth2Security" in line:
                types[(verb, path)] = "client"
    return types


def policy_ref(policies, name, params):
    return {
        "policyName": name,
        "policyVersion": "v1",
        "policyType": "common",
        "policyId": policies[(name, "v1")],
        "parameters": params,
    }


def find_api(auth, name, version):
    res = call("GET", f"{PUBLISHER}/apis?query=" + urllib.parse.quote(f'name:"{name}"'), auth=auth)
    for api in res.get("list", []):
        if api["name"] == name and api["version"] == version:
            return api
    return None


def cmd_apis():
    auth = admin_token()
    policies = common_policies(auth)
    for needed in [p[0] for p in POLICIES] + ["jwtClaimBasedAccessValidator"]:
        if (needed, "v1") not in policies:
            sys.exit(f"Policy {needed} v1 not found. Run the policies step first.")

    creds = base64.b64encode(f"{E['IS_ADMIN_USERNAME']}:{E['IS_ADMIN_PASSWORD']}".encode()).decode()
    bank_base = f"https://{E['OB_HOST']}:{E['APIM_PORT']}"
    dynamic_endpoint = {"endpoint_type": "default",
                        "production_endpoints": {"url": "default"},
                        "sandbox_endpoints": {"url": "default"}}

    for spec in APIS:
        spec_path = os.path.join(E["REPO_DIR"], spec["spec"])
        api = find_api(auth, spec["name"], spec["version"])
        if api is None:
            log(f"Importing {spec['name']}")
            props = {"name": spec["name"], "version": spec["version"], "context": spec["context"],
                     "policies": ["Unlimited"], "endpointConfig": dynamic_endpoint,
                     "keyManagers": ["all"]}
            with open(spec_path, "rb") as f:
                content = f.read()
            api = call("POST", f"{PUBLISHER}/apis/import-openapi", auth=auth, files={
                "file": (os.path.basename(spec_path), content, "application/yaml"),
                "additionalProperties": (None, json.dumps(props).encode(), "application/json"),
            })
        api = call("GET", f"{PUBLISHER}/apis/{api['id']}", auth=auth)

        token_types = operation_token_types(spec_path)
        consent_params = {
            "consentIdClaimName": "consent_id",
            "consentServiceBasicAuthCredentials": creds,
            "consentServiceBaseUrl": IS,
        }
        dynamic_params = {
            "consentServiceRoutingRegexPattern": CONSENT_ROUTING_REGEX,
            "consentServiceBasicAuthCredentials": creds,
            "consentServiceBaseUrl": IS,
            "bankBackendBaseUrl": bank_base + spec["backend"],
        }
        for op in api["operations"]:
            kind = token_types.get((op["verb"].upper(), op["target"]), "user")
            request = [policy_ref(policies, "jwtClaimBasedAccessValidator", {
                "accessVerificationClaim": "aut",
                "accessVerificationClaimValue": "APPLICATION_USER" if kind == "user" else "APPLICATION",
                "shouldAllowValidation": "false",
            })]
            if kind == "user":
                request.append(policy_ref(policies, "ConsentEnforcementPolicy", consent_params))
            request.append(policy_ref(policies, "DynamicEndpointPolicy", dynamic_params))
            op["operationPolicies"] = {"request": request, "response": [], "fault": []}
        api["apiPolicies"] = {"request": [policy_ref(policies, "MTLSEnforcementPolicy", {})],
                              "response": [], "fault": []}
        api["endpointConfig"] = dynamic_endpoint
        api["policies"] = api.get("policies") or ["Unlimited"]
        api["enableSchemaValidation"] = True
        call("PUT", f"{PUBLISHER}/apis/{api['id']}", auth=auth, json_body=api)
        done(f"{spec['name']}: dynamic endpoint, policies and schema validation set")

        rev = call("POST", f"{PUBLISHER}/apis/{api['id']}/revisions", auth=auth,
                   json_body={"description": "Created by setup"})
        call("POST", f"{PUBLISHER}/apis/{api['id']}/deploy-revision?revisionId={rev['id']}", auth=auth,
             json_body=[{"name": "Default", "vhost": E["OB_HOST"], "displayOnDevportal": True}])
        done(f"{spec['name']}: revision deployed to the Default gateway")

        if api.get("lifeCycleStatus") != "PUBLISHED":
            call("POST", f"{PUBLISHER}/apis/change-lifecycle?apiId={api['id']}&action=Publish", auth=auth)
        done(f"{spec['name']}: published")
        save_state(**{f"api_{spec['key']}_id": api["id"]})


# --------------------------------------------------------------------------------------------
# Onboarding
# --------------------------------------------------------------------------------------------

def approve_pending(workflow_type, wait_seconds=60):
    """Approve every pending workflow task of a type, as the bank admin. Returns how many."""
    auth = admin_token()
    deadline = time.time() + wait_seconds
    while True:
        tasks = call("GET", f"{ADMIN}/workflows?workflowType=AM_{workflow_type}&limit=100", auth=auth)
        pending = [t for t in tasks.get("list", []) if t.get("workflowStatus") == "CREATED"]
        if pending or time.time() > deadline:
            break
        time.sleep(3)
    for t in pending:
        call("POST", f"{ADMIN}/workflows/update-workflow-status?workflowReferenceId={t['referenceId']}",
             auth=auth, json_body={"status": "APPROVED", "attributes": {}, "description": "Approved by setup"})
    if pending:
        done(f"Bank admin approved {len(pending)} {workflow_type} task(s)")
    return len(pending)


def sign_up_developer():
    user = E["DEVELOPER_USERNAME"]
    log(f"Signing up the client application's developer '{user}'")
    body = {
        "user": {
            "username": user,
            "realm": "PRIMARY",
            "password": E["DEVELOPER_PASSWORD"],
            "claims": [
                {"uri": "http://wso2.org/claims/givenname", "value": "Client"},
                {"uri": "http://wso2.org/claims/lastname", "value": "Developer"},
                {"uri": "http://wso2.org/claims/emailaddress",
                 "value": user if "@" in user else f"{user}@example.com"},
            ],
        },
        "properties": [],
    }
    try:
        call("POST", f"{APIM}/api/identity/user/v1.0/me",
             auth=basic(E["AM_ADMIN_USERNAME"], E["AM_ADMIN_PASSWORD"]), json_body=body)
        done("Developer signed up")
    except HttpError as e:
        if e.status == 409 or "already exist" in e.body.lower() or "20030" in e.body:
            done("Developer already exists")
        else:
            raise
    approve_pending("USER_SIGNUP", wait_seconds=15)


def find_application(auth, name):
    res = call("GET", f"{DEVPORTAL}/applications?query=" + urllib.parse.quote(name), auth=auth)
    for app in res.get("list", []):
        if app["name"] == name:
            return app
    return None


def wait_for(fn, what, seconds=90):
    deadline = time.time() + seconds
    while time.time() < deadline:
        value = fn()
        if value:
            return value
        time.sleep(3)
    sys.exit(f"Timed out waiting for {what}")


def application_properties(dev, km_name):
    """The key manager's client application fields with their defaults, plus the JWKS URI."""
    res = call("GET", f"{DEVPORTAL}/key-managers", auth=dev)
    km = next((k for k in res.get("list", []) if k["name"] == km_name), None)
    if km is None:
        sys.exit(f"Key manager '{km_name}' is not visible in the Developer Portal.")
    props = {c["name"]: c.get("default") or "" for c in km.get("applicationConfiguration", [])}
    props["jwks_uri"] = E["JWKS_URL"]
    return props


def cmd_onboard():
    sign_up_developer()
    dev = developer_token()
    name = E["CLIENT_APP_NAME"]

    app = find_application(dev, name)
    if app is None:
        log(f"Creating the client application '{name}'")
        app = call("POST", f"{DEVPORTAL}/applications", auth=dev,
                   json_body={"name": name, "throttlingPolicy": "Unlimited", "tokenType": "JWT",
                              "description": "Non-regulated Open Banking client application"})
    if app.get("status") != "APPROVED":
        approve_pending("APPLICATION_CREATION")
        wait_for(lambda: call("GET", f"{DEVPORTAL}/applications/{app['applicationId']}", auth=dev)
                 .get("status") == "APPROVED", "the application to be approved")
    app_id = app["applicationId"]
    done(f"Client application '{name}' is approved ({app_id})")

    subscribed = {s["apiId"] for s in call("GET", f"{DEVPORTAL}/subscriptions?applicationId={app_id}",
                                           auth=dev).get("list", [])}
    state = load_state()
    for spec in APIS:
        api_id = state.get(f"api_{spec['key']}_id")
        if not api_id:
            res = call("GET", f"{DEVPORTAL}/apis?query=" + urllib.parse.quote(f'name:"{spec["name"]}"'), auth=dev)
            api_id = next((a["id"] for a in res.get("list", []) if a["name"] == spec["name"]), None)
        if not api_id:
            sys.exit(f"{spec['name']} is not visible in the Developer Portal. Run the apis step first.")
        if api_id in subscribed:
            done(f"Already subscribed to {spec['name']}")
            continue
        call("POST", f"{DEVPORTAL}/subscriptions", auth=dev,
             json_body={"applicationId": app_id, "apiId": api_id, "throttlingPolicy": "Unlimited"})
        done(f"Subscription to {spec['name']} requested")
    approve_pending("SUBSCRIPTION_CREATION", wait_seconds=15)

    def consumer_key():
        keys = call("GET", f"{DEVPORTAL}/applications/{app_id}/oauth-keys", auth=dev).get("list", [])
        prod = [k for k in keys if k.get("keyType") == "PRODUCTION"]
        return prod[0] if prod else None

    key = consumer_key()
    if key is None:
        log("Generating production keys")
        km_name = E.get("KEY_MANAGER_NAME", "FSKM")
        call("POST", f"{DEVPORTAL}/applications/{app_id}/generate-keys", auth=dev, json_body={
            "keyType": "PRODUCTION",
            "keyManager": km_name,
            "grantTypesToBeSupported": ["authorization_code", "client_credentials", "refresh_token"],
            "callbackUrl": E["CALLBACK_URL"],
            "additionalProperties": application_properties(dev, km_name),
        })
        approve_pending("APPLICATION_REGISTRATION_PRODUCTION")
    key = wait_for(lambda: (k := consumer_key()) and k.get("consumerKey") and k, "the production keys")
    client_id = key["consumerKey"]
    done(f"Client application client_id: {client_id}")

    res = call("GET", f"{IS}/api/server/v1/applications?filter=" + urllib.parse.quote(f"clientId eq {client_id}"),
               auth=is_admin())
    apps = res.get("applications", [])
    if not apps:
        sys.exit(f"No Identity Server application found for client_id {client_id}")
    save_state(client_app_id=app_id, client_id=client_id, is_app_id=apps[0]["id"])
    done(f"Identity Server application ID: {apps[0]['id']}")

    create_customer()


def create_customer():
    user = E["CUSTOMER_USERNAME"]
    body = {
        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
        "userName": user,
        "password": E["CUSTOMER_PASSWORD"],
        "name": {"givenName": "Demo", "familyName": "Customer"},
        "emails": [{"value": user if "@" in user else f"{user}@example.com", "primary": True}],
    }
    try:
        call("POST", f"{IS}/scim2/Users", auth=is_admin(), json_body=body,
             headers={"Content-Type": "application/scim+json"})
        done(f"Created the customer '{user}' in Identity Server")
    except HttpError as e:
        if e.status == 409:
            done(f"Customer '{user}' already exists")
        else:
            raise


# --------------------------------------------------------------------------------------------
# Consent API access
# --------------------------------------------------------------------------------------------

CONSENT_API_RESOURCE = "OB-internal-api-resource"
CONSENT_API_SCOPE = "ob-internal-api-access"
CONSENT_API_ROLE = "OBInternalApiAccessRole"
SCIM = {"Content-Type": "application/scim+json"}


def cmd_consent_access():
    """Give the IS admin the consent APIs' scope, through an API resource and a role."""
    auth = is_admin()
    quote = urllib.parse.quote

    res = call("GET", f"{IS}/api/server/v1/api-resources?filter="
               + quote(f"identifier eq {CONSENT_API_RESOURCE}"), auth=auth)
    if res.get("apiResources"):
        done(f"API resource '{CONSENT_API_RESOURCE}' already exists")
    else:
        call("POST", f"{IS}/api/server/v1/api-resources", auth=auth, json_body={
            "identifier": CONSENT_API_RESOURCE, "name": CONSENT_API_RESOURCE,
            "requiresAuthorization": True,
            "scopes": [{"name": CONSENT_API_SCOPE, "displayName": CONSENT_API_SCOPE,
                        "description": "Call the Financial Services accelerator's consent APIs"}],
        })
        done(f"Added API resource '{CONSENT_API_RESOURCE}' with scope '{CONSENT_API_SCOPE}'")

    admin = E["IS_ADMIN_USERNAME"]
    users = call("GET", f"{IS}/scim2/Users?filter=" + quote(f'userName eq "{admin}"'),
                 auth=auth).get("Resources", [])
    if not users:
        sys.exit(f"Identity Server has no user '{admin}'")
    admin_id = users[0]["id"]

    roles = call("GET", f"{IS}/scim2/v2/Roles?filter=" + quote(f"displayName eq {CONSENT_API_ROLE}"),
                 auth=auth).get("Resources", [])
    if not roles:
        call("POST", f"{IS}/scim2/v2/Roles", auth=auth, headers=SCIM, json_body={
            "schemas": ["urn:ietf:params:scim:schemas:extension:2.0:Role"],
            "displayName": CONSENT_API_ROLE,
            "permissions": [{"value": CONSENT_API_SCOPE}],
            "users": [{"value": admin_id}],
        })
        done(f"Added role '{CONSENT_API_ROLE}' with '{CONSENT_API_SCOPE}', assigned to '{admin}'")
        return

    role = call("GET", f"{IS}/scim2/v2/Roles/{roles[0]['id']}", auth=auth)
    if CONSENT_API_SCOPE not in [p.get("value") for p in role.get("permissions", [])]:
        call("PATCH", f"{IS}/scim2/v2/Roles/{role['id']}", auth=auth, headers=SCIM, json_body={
            "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
            "Operations": [{"op": "add", "path": "permissions", "value": [{"value": CONSENT_API_SCOPE}]}],
        })
        done(f"Added '{CONSENT_API_SCOPE}' to role '{CONSENT_API_ROLE}'")
    if admin_id not in [u.get("value") for u in role.get("users", [])]:
        call("PATCH", f"{IS}/scim2/v2/Roles/{role['id']}/Users", auth=auth, headers=SCIM, json_body={
            "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
            "Operations": [{"op": "add", "path": "users", "value": [{"value": admin_id}]}],
        })
        done(f"Assigned '{admin}' to role '{CONSENT_API_ROLE}'")
    done(f"'{admin}' can call the consent APIs (role '{CONSENT_API_ROLE}')")


# --------------------------------------------------------------------------------------------
# Postman
# --------------------------------------------------------------------------------------------

PMLIB_URL = "https://joolfe.github.io/postman-util-lib/dist/bundle.js"


def cmd_postman():
    state = load_state()
    if not state.get("client_id"):
        sys.exit("No client_id yet. Run the onboard step first.")
    src_dir = os.path.join(E["REPO_DIR"], "artifacts", "postman-script")
    src = next(f for f in os.listdir(src_dir) if f.endswith(".postman_collection.json"))
    with open(os.path.join(src_dir, src)) as f:
        collection = json.load(f)

    cache = os.path.join(E["STATE_DIR"], "pmlib-bundle.js")
    if not os.path.exists(cache):
        log("Downloading the Postman JWT signing library")
        with urllib.request.urlopen(PMLIB_URL, timeout=60) as resp, open(cache, "wb") as out:
            out.write(resp.read())
    with open(cache) as f:
        pmlib = f.read()
    with open(os.path.join(E["CLIENT_DIR"], "jwk.json")) as f:
        jwk = f.read().strip()

    values = {
        "client_id": state["client_id"],
        "kid": "client-signing-key",
        "jwk": jwk,
        "pmlib_code": pmlib,
        "is_host": E["OB_HOST"],
        "is_port": E["IS_PORT"],
        "gw_host": E["OB_HOST"],
        "gw_port": E["GW_PORT"],
        "redirect_uri": E["CALLBACK_URL"],
    }
    for var in collection.get("variable", []):
        if var["key"] in values:
            var["value"] = values[var["key"]]
    collection["info"]["name"] += " (configured)"

    out_dir = os.path.join(E["STATE_DIR"], "postman")
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, src.replace(".postman_collection.json", ".configured.postman_collection.json"))
    with open(out, "w") as f:
        json.dump(collection, f, indent=2)
    done(f"Postman collection written to {out}")


COMMANDS = {
    "key-manager": cmd_key_manager,
    "policies": cmd_policies,
    "apis": cmd_apis,
    "onboard": cmd_onboard,
    "consent-access": cmd_consent_access,
    "postman": cmd_postman,
}

if __name__ == "__main__":
    if len(sys.argv) != 2 or sys.argv[1] not in COMMANDS:
        sys.exit(__doc__)
    try:
        COMMANDS[sys.argv[1]]()
    except HttpError as e:
        sys.exit(f"\033[1;31merror\033[0m {e}")
