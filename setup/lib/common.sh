#!/usr/bin/env bash
#
# Shared helpers for the setup phases. Source it, then call load_config.
#

set -euo pipefail

SETUP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_DIR="$(cd "$SETUP_DIR/.." && pwd)"
STATE_DIR="$SETUP_DIR/.state"
LIB_DIR="$SETUP_DIR/lib"

IS_PORT=9446
APIM_PORT=9443
GW_PORT=8243
APIM_HTTP_PORT=9763

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m ok\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mwarn\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31merror\033[0m %s\n' "$*" >&2; exit 1; }

require_cmd() {
  local c
  for c in "$@"; do
    command -v "$c" >/dev/null 2>&1 || die "'$c' is required but not installed."
  done
}

expand_path() {
  local p="$1"
  case "$p" in
    "~") p="$HOME" ;;
    "~/"*) p="$HOME/${p#\~/}" ;;
  esac
  printf '%s' "$p"
}

# Name of the single top-level folder inside a zip. sed reads all input, so unzip never gets
# SIGPIPE (which pipefail would turn into a failure).
zip_top_dir() {
  local first
  first=$(unzip -Z1 "$1" | sed -n '1p')
  printf '%s' "${first%%/*}"
}

load_config() {
  local env_file="$SETUP_DIR/setup.env"
  [ -f "$env_file" ] || die "Missing $env_file. Copy setup/setup.env.example to setup/setup.env and fill it in."
  set -a
  # shellcheck disable=SC1090
  . "$env_file"
  set +a

  WORK_DIR="$(expand_path "${WORK_DIR:?WORK_DIR is not set}")"
  APIM_ZIP="$(expand_path "${APIM_ZIP:-}")"
  IS_ZIP="$(expand_path "${IS_ZIP:-}")"
  FSAM_ZIP="$(expand_path "${FSAM_ZIP:-}")"
  FSIAM_ZIP="$(expand_path "${FSIAM_ZIP:-}")"
  MYSQL_JDBC_JAR="$(expand_path "${MYSQL_JDBC_JAR:-}")"
  SERVER_JAVA_HOME="$(expand_path "${SERVER_JAVA_HOME:-}")"
  BUILD_JAVA_HOME="$(expand_path "${BUILD_JAVA_HOME:-}")"

  if [ -z "${APIM_HOME:-}" ]; then
    [ -f "$APIM_ZIP" ] || die "Set APIM_HOME, or APIM_ZIP to an existing zip."
    APIM_HOME="$WORK_DIR/$(zip_top_dir "$APIM_ZIP")"
  fi
  if [ -z "${IS_HOME:-}" ]; then
    [ -f "$IS_ZIP" ] || die "Set IS_HOME, or IS_ZIP to an existing zip."
    IS_HOME="$WORK_DIR/$(zip_top_dir "$IS_ZIP")"
  fi
  APIM_HOME="$(expand_path "$APIM_HOME")"
  IS_HOME="$(expand_path "$IS_HOME")"

  FSAM_DIR_NAME="${FSAM_DIR_NAME:-$( [ -f "$FSAM_ZIP" ] && zip_top_dir "$FSAM_ZIP" || echo wso2-fsam-accelerator-4.0.0 )}"
  FSIAM_DIR_NAME="${FSIAM_DIR_NAME:-$( [ -f "$FSIAM_ZIP" ] && zip_top_dir "$FSIAM_ZIP" || echo wso2-fsiam-accelerator-4.0.0 )}"
  FSAM_HOME="$APIM_HOME/$FSAM_DIR_NAME"
  FSIAM_HOME="$IS_HOME/$FSIAM_DIR_NAME"

  OB_HOST="${OB_HOST:-localhost}"
  DB_PREFIX="${DB_PREFIX:-nonreg_ob_}"
  JWKS_PORT="${JWKS_PORT:-8000}"
  KEY_MANAGER_NAME="${KEY_MANAGER_NAME:-FSKM}"

  IS_URL="https://$OB_HOST:$IS_PORT"
  APIM_URL="https://$OB_HOST:$APIM_PORT"
  GW_URL="https://$OB_HOST:$GW_PORT"
  JWKS_URL="http://$OB_HOST:$JWKS_PORT/jwks.json"
  CLIENT_DIR="$STATE_DIR/client"

  export SETUP_DIR REPO_DIR STATE_DIR LIB_DIR WORK_DIR APIM_HOME IS_HOME FSAM_HOME FSIAM_HOME \
    IS_PORT APIM_PORT GW_PORT APIM_HTTP_PORT IS_URL APIM_URL GW_URL JWKS_URL CLIENT_DIR OB_HOST \
    DB_PREFIX JWKS_PORT KEY_MANAGER_NAME

  mkdir -p "$STATE_DIR"
}

# Use SERVER_JAVA_HOME for the servers when it is set.
use_server_java() {
  if [ -n "${SERVER_JAVA_HOME:-}" ]; then
    export JAVA_HOME="$SERVER_JAVA_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
  [ -n "${JAVA_HOME:-}" ] || die "JAVA_HOME is not set. Set it in your shell, or SERVER_JAVA_HOME in setup.env."
}

require_product_homes() {
  [ -d "$APIM_HOME/repository/components" ] || die "API Manager not found at $APIM_HOME. Run the extract phase first."
  [ -d "$IS_HOME/repository/components" ] || die "Identity Server not found at $IS_HOME. Run the extract phase first."
}

# Wait until a URL answers with one of the given HTTP codes (default 200).
wait_for_url() {
  local url="$1" timeout="${2:-600}" codes="${3:-200}" waited=0 code
  while :; do
    code=$(curl -sk -o /dev/null -w '%{http_code}' "$url" || true)
    case " $codes " in *" $code "*) return 0 ;; esac
    [ "$waited" -ge "$timeout" ] && return 1
    sleep 5
    waited=$((waited + 5))
  done
}

is_up()   { [ "$(curl -sk -o /dev/null -w '%{http_code}' "$IS_URL/oauth2/token/.well-known/openid-configuration" || true)" = "200" ]; }
apim_up() { [ "$(curl -sk -o /dev/null -w '%{http_code}' "$APIM_URL/api/am/devportal/v3/apis" || true)" = "200" ]; }

require_servers_up() {
  is_up   || die "Identity Server is not answering at $IS_URL. Run the start phase first."
  apim_up || die "API Manager is not answering at $APIM_URL. Run the start phase first."
}

# Run a python helper from lib/ with the config exported.
py() {
  python3 "$LIB_DIR/$1" "${@:2}"
}
