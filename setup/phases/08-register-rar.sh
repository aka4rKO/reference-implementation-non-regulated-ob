#!/usr/bin/env bash
#
# Phase 8: register the RAR types in Identity Server. (TRYOUT step 11)
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_servers_up
require_cmd jq

scripts="$REPO_DIR/artifacts/rar-schemas/scripts"
export IS_HOST="$IS_URL"
export IS_AUTH="$IS_ADMIN_USERNAME:$IS_ADMIN_PASSWORD"

registered() {
  bash "$scripts/register.sh" verify 2>/dev/null | grep -q '"account_information_v1.0"'
}

if registered; then
  ok "RAR types already registered"
else
  log "Registering the RAR types"
  bash "$scripts/register.sh" register
  registered || die "The RAR types don't show up in Identity Server's discovery endpoint."
  ok "RAR types registered"
fi
