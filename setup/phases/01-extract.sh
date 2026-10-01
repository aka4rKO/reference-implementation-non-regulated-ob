#!/usr/bin/env bash
#
# Phase 1: extract API Manager and Identity Server, and copy the accelerators into them.
# (TRYOUT step 1, and step 2.1)
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_cmd unzip

mkdir -p "$WORK_DIR"

extract_product() {
  local label="$1" zip="$2" home="$3"
  if [ -d "$home/repository/components" ]; then
    ok "$label already at $home"
    return
  fi
  [ -f "$zip" ] || die "$label not found at $home, and its zip '$zip' does not exist."
  log "Extracting $label into $WORK_DIR"
  unzip -q "$zip" -d "$WORK_DIR"
  [ -d "$home/repository/components" ] || die "Extracted $zip, but $home is not a WSO2 product."
  ok "$label extracted to $home"
}

copy_accelerator() {
  local label="$1" zip="$2" product_home="$3" acc_home="$4"
  if [ -d "$acc_home/bin" ]; then
    ok "$label already at $acc_home"
    return
  fi
  [ -f "$zip" ] || die "$label not found at $acc_home, and its zip '$zip' does not exist."
  log "Copying $label into $product_home"
  unzip -q "$zip" -d "$product_home"
  [ -f "$acc_home/bin/merge.sh" ] || die "Extracted $zip, but $acc_home/bin/merge.sh is missing."
  ok "$label extracted to $acc_home"
}

extract_product "API Manager" "$APIM_ZIP" "$APIM_HOME"
extract_product "Identity Server" "$IS_ZIP" "$IS_HOME"
copy_accelerator "fsam accelerator" "$FSAM_ZIP" "$APIM_HOME" "$FSAM_HOME"
copy_accelerator "fsiam accelerator" "$FSIAM_ZIP" "$IS_HOME" "$FSIAM_HOME"
