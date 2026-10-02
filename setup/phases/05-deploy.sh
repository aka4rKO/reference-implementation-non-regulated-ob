#!/usr/bin/env bash
#
# Phase 5: build and deploy the webapps, add the error formatter and approval workflow, and apply
# the deployment.toml changes. (TRYOUT steps 5, 6 and 9)
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_product_homes
require_cmd mvn curl python3

edit() { python3 "$LIB_DIR/edit.py" "$@"; }
TPL="$SETUP_DIR/templates/toml"
IS_TOML="$IS_HOME/repository/conf/deployment.toml"
AM_TOML="$APIM_HOME/repository/conf/deployment.toml"

# ---- Build and copy the webapps ------------------------------------------------------------

log "Building this repo with Maven"
(
  if [ -n "${BUILD_JAVA_HOME:-}" ]; then
    export JAVA_HOME="$BUILD_JAVA_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
  cd "$REPO_DIR" && mvn -q clean install
)
cp "$REPO_DIR/non-regulated-ob-service-extension/target/non#regulated#ob#service#extension.war" \
   "$IS_HOME/repository/deployment/server/webapps/"
cp "$REPO_DIR/non-regulated-ob-demo-backend/target/non#regulated#ob#demo#backend.war" \
   "$APIM_HOME/repository/deployment/server/webapps/"
ok "Service extension deployed to Identity Server, demo bank backend to API Manager"

# ---- Error formatter and approval workflow -------------------------------------------------

SEQUENCES="$APIM_HOME/repository/deployment/server/synapse-configs/default/sequences"
cp "$REPO_DIR/artifacts/custom-synapse-error-formatter/customErrorFormatter.xml" "$SEQUENCES/"
ok "Error formatter copied to the gateway's sequences"
python3 - "$SEQUENCES/_cors_request_handler_.xml" <<'EOF'
import sys
path = sys.argv[1]
text = open(path).read()
call = '<sequence key="customErrorFormatter"/>'
if call not in text:
    end = text.rindex("</sequence>")
    text = text[:end] + "   " + call + "\n" + text[end:]
    open(path, "w").write(text)
EOF
ok "Error formatter called from _cors_request_handler_.xml"

# API Manager copies this file into its registry the first time it starts with an empty
# database. configure.sh creates empty databases, so replacing it here turns approval on.
wf="$APIM_HOME/repository/resources/default-workflow-extensions.xml"
[ -f "$wf.orig" ] || cp "$wf" "$wf.orig"
cp "$REPO_DIR/artifacts/workflow-extensions/workflow-extensions.xml" "$wf"
ok "Approval workflow set (original kept as $(basename "$wf").orig)"

# ---- Identity Server as key manager: notification handler ---------------------------------

jar="wso2is.notification.event.handlers-2.1.3.jar"
if [ ! -f "$IS_HOME/repository/components/dropins/$jar" ]; then
  log "Downloading $jar"
  curl -sSfL -o "$IS_HOME/repository/components/dropins/$jar" \
    "https://maven.wso2.org/nexus/content/repositories/releases/org/wso2/km/ext/wso2is/wso2is.notification.event.handlers/2.1.3/$jar"
fi
ok "Notification event handler in Identity Server dropins"

# ---- Identity Server deployment.toml -------------------------------------------------------

append_tpl() {
  local file="$1" marker="$2" tpl="$3" tmp
  tmp=$(mktemp)
  sed "s/@OB_HOST@/$OB_HOST/g" "$TPL/$tpl" > "$tmp"
  edit toml-append "$file" "$marker" "$tmp"
  rm -f "$tmp"
}

log "Updating Identity Server deployment.toml"
[ -f "$IS_TOML.orig" ] || cp "$IS_TOML" "$IS_TOML.orig"

# 1. Connect the service extension
edit toml-set "$IS_TOML" financial_services.extensions.endpoint enabled true
edit toml-set "$IS_TOML" financial_services.extensions.endpoint allowed_extensions \
  '["populate_consent_authorize_screen", "persist_authorized_consent", "pre_process_consent_revoke", "validate_consent_access", "map_accelerator_error_response"]'
edit toml-set "$IS_TOML" financial_services.extensions.endpoint base_url \
  "\"https://$OB_HOST:$IS_PORT/non/regulated/ob/service/extension\""
edit toml-set "$IS_TOML" financial_services.extensions.endpoint.security username "\"$IS_ADMIN_USERNAME\""
edit toml-set "$IS_TOML" financial_services.extensions.endpoint.security password "\"$IS_ADMIN_PASSWORD\""
append_tpl "$IS_TOML" "/non/regulated/ob/service/extension/(.*)" is-service-extension-access.toml

# 2. Consent validation: API Manager's certificate alias from phase 4
edit toml-set "$IS_TOML" financial_services.consent.validation signature.alias '"wso2am"'

# 3. Identity Server as API Manager's key manager
edit toml-set "$IS_TOML" oauth authorize_all_scopes true
append_tpl "$IS_TOML" "ApimOauthEventInterceptor" is-km-event-listener.toml

# 4. FAPI 2.0
edit toml-set "$IS_TOML" oauth timestamp_skew 10
edit toml-set "$IS_TOML" oauth.token_validation authorization_code_validity 50
edit toml-set "$IS_TOML" oauth.oidc fapi.version '"2"'
edit toml-set "$IS_TOML" oauth.oidc id_token.issuer \
  '"https://$ref{server.hostname}:${carbon.management.port}/oauth2/oidcdiscovery"'
edit toml-set "$IS_TOML" oauth.oidc id_token.use_entityid_as_issuer true
edit toml-set "$IS_TOML" oauth.mutualtls client_certificate_header '"x-wso2-mtls-cert"'
edit toml-set "$IS_TOML" transport.https.sslHostConfig.properties ciphers \
  '"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"'
append_tpl "$IS_TOML" "[myaccount.idp_configs]" is-fapi-idp-configs.toml

# 5. Basic auth: allow it only on the endpoints that need it
edit toml-set "$IS_TOML" compatibility_setting.basic_auth.disable_basic_auth default_value '"true"'
edit toml-set "$IS_TOML" compatibility_setting.basic_auth.allowed_endpoints default_value \
  '"(/t/[^/]+)?/api/fs/consent/.*, (/t/[^/]+)?/non/regulated/ob/service/extension/.*, (/t/[^/]+)?/api/identity/oauth2/dcr/v1[.]1/register.*, (/t/[^/]+)?/api/identity/oauth2/v1[.]0/scopes.*, (/t/[^/]+)?/api/server/v1/applications.*, (/t/[^/]+)?/api/server/v1/api-resources.*, (/t/[^/]+)?/scim2/users.*, (/t/[^/]+)?/scim2/v2/roles.*, (/t/[^/]+)?/oauth2/introspect"'
ok "Identity Server deployment.toml updated (original kept as deployment.toml.orig)"

# ---- API Manager deployment.toml -----------------------------------------------------------

log "Updating API Manager deployment.toml"
[ -f "$AM_TOML.orig" ] || cp "$AM_TOML" "$AM_TOML.orig"

# 1. Open up the demo bank backend
append_tpl "$AM_TOML" "/non/regulated/ob/demo/backend/(.*)" am-demo-backend-access.toml

# 2. Error formatter
edit toml-set "$AM_TOML" apim.sync_runtime_artifacts.gateway skip_list.sequences '["customErrorFormatter.xml"]'

# 3. Client application fields: replace the accelerator's defaults with this repo's snippet
edit toml-remove-array "$AM_TOML" financial_services.keymanager.application.type.attributes
edit toml-append "$AM_TOML" 'name="jwks_uri"' "$REPO_DIR/artifacts/devportal-km-configs/am-deployment-snippet.toml"

# 4. Other settings
edit toml-set "$AM_TOML" apim.key_manager allow_subscription_validation_disabling false
edit toml-set "$AM_TOML" transport.https.properties maxHttpHeaderSize '"65536"'
ok "API Manager deployment.toml updated (original kept as deployment.toml.orig)"
