#!/usr/bin/env bash
#
# Phase 9: onboard the client application and let it use the RAR types. Each request is approved
# as the bank admin. Also creates the customer. (TRYOUT steps 12, 13 and 14.1)
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_servers_up
require_cmd jq

py wso2.py onboard

app_id=$(jq -r '.is_app_id // empty' "$STATE_DIR/state.json")
[ -n "$app_id" ] || die "No Identity Server application ID in $STATE_DIR/state.json."

log "Authorizing the client application for the RAR types"
export IS_HOST="$IS_URL"
export IS_AUTH="$IS_ADMIN_USERNAME:$IS_ADMIN_PASSWORD"
set +e
out=$(APP_ID="$app_id" bash "$REPO_DIR/artifacts/rar-schemas/scripts/authorize-app.sh" authorize 2>&1)
rc=$?
set -e
printf '%s\n' "$out"
if [ "$rc" -ne 0 ]; then
  # Already authorized from an earlier run is fine.
  printf '%s' "$out" | grep -q -i 'already' || die "Authorizing the RAR types failed."
fi
ok "Client application can use the RAR types"
