#!/usr/bin/env bash
#
# Phase 7: add the key manager, create the three accelerator policies, and import, wire up, deploy
# and publish both APIs. (TRYOUT steps 8 and 10)
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_servers_up

py wso2.py key-manager
py wso2.py policies
py wso2.py apis
