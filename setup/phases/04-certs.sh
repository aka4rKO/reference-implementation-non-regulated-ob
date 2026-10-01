#!/usr/bin/env bash
#
# Phase 4: new server certificates, exchanged between the servers, and the client application's
# signing and transport keys with its JWKS. (TRYOUT steps 3 and 4)
#
# The servers must be stopped. The client application's keys are created once and kept in
# setup/.state/client; delete that folder to create new ones.
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_product_homes
require_cmd keytool openssl python3

if is_up || apim_up; then
  die "Stop the servers first (setup/setup.sh stop). Keystores can't be changed while they run."
fi

PASS=wso2carbon
IS_SEC="$IS_HOME/repository/resources/security"
AM_SEC="$APIM_HOME/repository/resources/security"

# keytool, without its "JKS is proprietary" warning. Keeps keytool's exit code.
kt() {
  local out rc=0
  out=$(keytool "$@" 2>&1) || rc=$?
  printf '%s\n' "$out" | grep -v -i -e 'proprietary format' -e '^Warning:' -e '^[[:space:]]*$' || true
  return $rc
}

has_alias() { keytool -list -alias "$2" -keystore "$1" -storepass "$PASS" >/dev/null 2>&1; }

replace_cert() {
  local truststore="$1" alias="$2" file="$3"
  has_alias "$truststore" "$alias" && kt -delete -alias "$alias" -keystore "$truststore" -storepass "$PASS"
  kt -import -noprompt -alias "$alias" -file "$file" -keystore "$truststore" -storepass "$PASS"
}

new_server_key() {
  local label="$1" keystore="$2" out="$3"
  log "New certificate for $label"
  has_alias "$keystore" wso2carbon && kt -delete -alias wso2carbon -keystore "$keystore" -storepass "$PASS"
  kt -genkey -alias wso2carbon -keystore "$keystore" -storepass "$PASS" -keypass "$PASS" \
    -keyalg RSA -keysize 2048 -validity 3650 \
    -dname "CN=$OB_HOST" -ext "san=dns:$OB_HOST,dns:localhost,ip:127.0.0.1"
  kt -export -alias wso2carbon -keystore "$keystore" -storepass "$PASS" -file "$out"
}

mkdir -p "$STATE_DIR/certs"
new_server_key "Identity Server" "$IS_SEC/wso2carbon.p12" "$STATE_DIR/certs/is.pem"
new_server_key "API Manager" "$AM_SEC/wso2carbon.jks" "$STATE_DIR/certs/am.pem"

log "Making each server trust both certificates"
for ts in "$IS_SEC/client-truststore.p12" "$AM_SEC/client-truststore.jks"; do
  replace_cert "$ts" wso2is "$STATE_DIR/certs/is.pem"
  # Identity Server checks requests signed by API Manager against this alias (signature.alias).
  replace_cert "$ts" wso2am "$STATE_DIR/certs/am.pem"
done
ok "Server certificates exchanged (aliases wso2is and wso2am)"

mkdir -p "$CLIENT_DIR"
if [ -f "$CLIENT_DIR/signing.key" ] && [ -f "$CLIENT_DIR/transport.key" ]; then
  ok "Client application keys already exist in $CLIENT_DIR"
else
  log "Creating the client application's signing and transport keys (self-signed, for testing only)"
  openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=client-signing" \
    -keyout "$CLIENT_DIR/signing.key" -out "$CLIENT_DIR/signing.pem" 2>/dev/null
  openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=client-transport" \
    -keyout "$CLIENT_DIR/transport.key" -out "$CLIENT_DIR/transport.pem" 2>/dev/null
fi
python3 "$LIB_DIR/jwk.py" "$CLIENT_DIR/signing.key" client-signing-key "$CLIENT_DIR/jwk.json" "$CLIENT_DIR/jwks.json"
ok "JWK and JWKS written to $CLIENT_DIR"

for ts in "$IS_SEC/client-truststore.p12" "$AM_SEC/client-truststore.jks"; do
  replace_cert "$ts" client-transport "$CLIENT_DIR/transport.pem"
done
ok "Both servers trust the client application's transport certificate"
