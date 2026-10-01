#!/usr/bin/env bash
#
# Set up the non-regulated Open Banking reference implementation end to end.
#
#   setup/setup.sh all            run every phase except "update", in order
#   setup/setup.sh <phase>...     run only the named phases, in the order given
#   setup/setup.sh list           list the phases
#
# Phases: extract update accelerators certs deploy start configure-apim register-rar
#         onboard postman stop
#
# "update" needs your WSO2 subscription login, so run it yourself in a terminal, between
# "extract" and "accelerators":  setup/setup.sh update
#
# Settings are read from setup/setup.env (copy setup/setup.env.example).
#
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

phase_script() {
  case "$1" in
    extract)        echo 01-extract.sh ;;
    update)         echo 02-update.sh ;;
    accelerators)   echo 03-accelerators.sh ;;
    certs)          echo 04-certs.sh ;;
    deploy)         echo 05-deploy.sh ;;
    start)          echo 06-start.sh ;;
    configure-apim) echo 07-configure-apim.sh ;;
    register-rar)   echo 08-register-rar.sh ;;
    onboard)        echo 09-onboard-client.sh ;;
    postman)        echo 10-postman.sh ;;
    stop)           echo stop.sh ;;
    *)              return 1 ;;
  esac
}

ALL="extract accelerators certs deploy start configure-apim register-rar onboard postman"

usage() { sed -n '3,16p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

[ $# -gt 0 ] || usage 1
case "$1" in
  -h|--help|help) usage 0 ;;
  list) for p in $ALL update stop; do printf '%-15s %s\n' "$p" "$(phase_script "$p")"; done; exit 0 ;;
  all) set -- $ALL ;;
esac

for p in "$@"; do
  phase_script "$p" >/dev/null || { echo "Unknown phase: $p" >&2; usage 1; }
done

for p in "$@"; do
  printf '\n\033[1;35m### %s\033[0m\n' "$p"
  bash "$DIR/phases/$(phase_script "$p")"
done

printf '\n\033[1;32mDone:\033[0m %s\n' "$*"
