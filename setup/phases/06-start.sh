#!/usr/bin/env bash
#
# Phase 6: start the JWKS server, Identity Server and then API Manager, and wait until all are up.
# (TRYOUT step 7)
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_product_homes
use_server_java
require_cmd python3 curl

[ -f "$CLIENT_DIR/jwks.json" ] || die "No JWKS in $CLIENT_DIR. Run the certs phase first."

pid_running() { [ -f "$1" ] && kill -0 "$(cat "$1")" 2>/dev/null; }

jwks_pid="$STATE_DIR/jwks-server.pid"
if pid_running "$jwks_pid"; then
  ok "JWKS server already running"
else
  log "Starting the JWKS server on port $JWKS_PORT"
  (cd "$CLIENT_DIR" && nohup python3 -m http.server "$JWKS_PORT" >"$STATE_DIR/jwks-server.log" 2>&1 & echo $! >"$jwks_pid")
  wait_for_url "$JWKS_URL" 30 || die "The JWKS server did not start. See $STATE_DIR/jwks-server.log"
  ok "JWKS served at $JWKS_URL"
fi

if is_up; then
  ok "Identity Server already running"
else
  log "Starting Identity Server (log: $IS_HOME/repository/logs/wso2carbon.log)"
  sh "$IS_HOME/bin/wso2server.sh" start >/dev/null
  wait_for_url "$IS_URL/oauth2/token/.well-known/openid-configuration" 900 \
    || die "Identity Server did not start in 15 minutes. Check $IS_HOME/repository/logs/wso2carbon.log"
  ok "Identity Server is up at $IS_URL"
fi

if apim_up; then
  ok "API Manager already running"
else
  log "Starting API Manager (log: $APIM_HOME/repository/logs/wso2carbon.log)"
  sh "$APIM_HOME/bin/api-manager.sh" start >/dev/null
  wait_for_url "$APIM_URL/api/am/devportal/v3/apis" 900 \
    || die "API Manager did not start in 15 minutes. Check $APIM_HOME/repository/logs/wso2carbon.log"
  ok "API Manager is up at $APIM_URL"
fi
