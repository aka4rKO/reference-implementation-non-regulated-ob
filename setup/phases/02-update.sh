#!/usr/bin/env bash
#
# Phase 2: apply WSO2 updates to both products and both accelerators. (TRYOUT steps 1.2 and 2.3)
#
# The update tool asks for your WSO2 subscription login, so run this phase yourself in a terminal:
#   setup/setup.sh update
# It is skipped when you run the other phases.
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_product_homes

[ -t 0 ] || die "The update tool needs your WSO2 login. Run this in a terminal: setup/setup.sh update"

os=$(uname -s | tr '[:upper:]' '[:lower:]')
case "$(uname -m)" in
  x86_64|amd64) arch=amd64 ;;
  arm64|aarch64) arch=arm64 ;;
  *) die "Unsupported CPU: $(uname -m)" ;;
esac

update() {
  local label="$1" home="$2" tool
  log "Updating $label ($home)"
  cd "$home/bin"
  tool=""
  for candidate in "wso2update_${os}_${arch}" "wso2update_${os}"; do
    [ -x "$candidate" ] && tool="$candidate" && break
  done
  if [ -z "$tool" ]; then
    [ -x update_tool_setup.sh ] || die "No update tool or update_tool_setup.sh in $home/bin"
    ./update_tool_setup.sh
    for candidate in "wso2update_${os}_${arch}" "wso2update_${os}"; do
      [ -x "$candidate" ] && tool="$candidate" && break
    done
  fi
  [ -n "$tool" ] || die "update_tool_setup.sh did not produce an update tool in $home/bin"
  # The tool exits 1 or 2 when it updated itself or needs a re-run; run it until it settles.
  local rc
  for _ in 1 2 3; do
    set +e; "./$tool"; rc=$?; set -e
    [ "$rc" -eq 0 ] && break
    warn "$tool exited with $rc, running it again"
  done
  [ "$rc" -eq 0 ] || die "Updating $label failed (exit $rc)"
  ok "$label is up to date"
}

update "API Manager" "$APIM_HOME"
update "Identity Server" "$IS_HOME"
update "fsam accelerator" "$FSAM_HOME"
update "fsiam accelerator" "$FSIAM_HOME"

ok "Updates done. Run the accelerators phase next, so merge.sh copies the updated accelerators in."
