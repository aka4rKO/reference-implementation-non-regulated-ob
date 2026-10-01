#!/usr/bin/env bash
#
# Phase 10: write a Postman collection with the client application's values filled in.
# (TRYOUT step 14)
#
. "$(dirname "$0")/../lib/common.sh"
load_config

py wso2.py postman

cat <<EOF

Next, in Postman:
  1. Import the collection in $STATE_DIR/postman/
  2. Settings > General: turn off "SSL certificate verification".
  3. Settings > Certificates: add a client certificate for $OB_HOST:$IS_PORT and $OB_HOST:$GW_PORT
       CRT file: $CLIENT_DIR/transport.pem
       KEY file: $CLIENT_DIR/transport.key
  4. Run folder 0 once, then each other folder in order. In the Authorize step, sign in as
     $CUSTOMER_USERNAME / $CUSTOMER_PASSWORD.
EOF
