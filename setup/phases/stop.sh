#!/usr/bin/env bash
#
# Stop API Manager, Identity Server and the JWKS server.
#
. "$(dirname "$0")/../lib/common.sh"
load_config
use_server_java

wait_down() {
  local check="$1" label="$2" waited=0
  while "$check"; do
    [ "$waited" -ge 180 ] && die "$label is still running after 3 minutes."
    sleep 5
    waited=$((waited + 5))
  done
  ok "$label stopped"
}

if apim_up; then
  log "Stopping API Manager"
  sh "$APIM_HOME/bin/api-manager.sh" stop >/dev/null || true
  wait_down apim_up "API Manager"
fi

if is_up; then
  log "Stopping Identity Server"
  sh "$IS_HOME/bin/wso2server.sh" stop >/dev/null || true
  wait_down is_up "Identity Server"
fi

jwks_pid="$STATE_DIR/jwks-server.pid"
if [ -f "$jwks_pid" ]; then
  kill "$(cat "$jwks_pid")" 2>/dev/null || true
  rm -f "$jwks_pid"
  ok "JWKS server stopped"
fi
