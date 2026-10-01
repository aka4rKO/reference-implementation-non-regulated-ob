#!/usr/bin/env bash
#
# Phase 3: install the accelerators: JDBC driver, configure.properties, merge.sh and configure.sh.
# (TRYOUT step 2)
#
# configure.sh DROPS and recreates the databases named by DB_PREFIX, and replaces deployment.toml.
# Run phase 5 (deploy) again after this phase, since it edits deployment.toml.
#
. "$(dirname "$0")/../lib/common.sh"
load_config
require_product_homes
require_cmd mysql
[ -d "$FSAM_HOME/bin" ] || die "fsam accelerator not found at $FSAM_HOME. Run the extract phase first."
[ -d "$FSIAM_HOME/bin" ] || die "fsiam accelerator not found at $FSIAM_HOME. Run the extract phase first."

# Empty when there is no password. Left unquoted on purpose so it disappears.
mysql_pass="${DB_PASS:+-p$DB_PASS}"
mysql -u"$DB_USER" $mysql_pass -h"$DB_HOST" -e "SELECT 1" >/dev/null 2>&1 \
  || die "Cannot connect to MySQL at $DB_HOST as $DB_USER. Start MySQL or fix DB_* in setup.env."
ok "MySQL is reachable"

[ -f "$MYSQL_JDBC_JAR" ] || die "MYSQL_JDBC_JAR '$MYSQL_JDBC_JAR' does not exist."
cp "$MYSQL_JDBC_JAR" "$APIM_HOME/repository/components/lib/"
cp "$MYSQL_JDBC_JAR" "$IS_HOME/repository/components/lib/"
ok "Copied the MySQL JDBC driver into both products"

apim_version=$(basename "$APIM_HOME" | sed 's/^wso2am-//')
is_version=$(basename "$IS_HOME" | sed 's/^wso2is-//')
apim_conf="repository/resources/wso2am-${apim_version}-deployment.toml"
is_conf="repository/resources/wso2is-${is_version}-deployment.toml"
[ -f "$FSAM_HOME/$apim_conf" ] || die "The fsam accelerator has no $apim_conf for API Manager $apim_version."
[ -f "$FSIAM_HOME/$is_conf" ] || die "The fsiam accelerator has no $is_conf for Identity Server $is_version."

log "Writing configure.properties for both accelerators"
python3 "$LIB_DIR/edit.py" props "$FSAM_HOME/repository/conf/configure.properties" \
  "APIM_HOSTNAME=$OB_HOST" "IS_HOSTNAME=$OB_HOST" "BI_HOSTNAME=$OB_HOST" \
  "AM_ADMIN_USERNAME=$AM_ADMIN_USERNAME" "AM_ADMIN_PASSWORD=$AM_ADMIN_PASSWORD" "AM_ADMIN_NAME=$AM_ADMIN_NAME" \
  "IS_ADMIN_USERNAME=$IS_ADMIN_USERNAME" "IS_ADMIN_PASSWORD=$IS_ADMIN_PASSWORD" \
  "PRODUCT_CONF_PATH=$apim_conf" \
  "DB_TYPE=mysql" "DB_USER=$DB_USER" "DB_PASS=$DB_PASS" "DB_HOST=$DB_HOST" "DB_DRIVER=com.mysql.jdbc.Driver" \
  "DB_APIMGT=${DB_PREFIX}apimgtdb" "DB_USER_STORE=${DB_PREFIX}am_userdb" "DB_AM_CONFIG=${DB_PREFIX}am_configdb"
python3 "$LIB_DIR/edit.py" props "$FSIAM_HOME/repository/conf/configure.properties" \
  "IS_HOSTNAME=$OB_HOST" "APIM_HOSTNAME=$OB_HOST" "BI_HOSTNAME=$OB_HOST" \
  "IS_PRODUCT=wso2is-$is_version" \
  "IS_ADMIN_USERNAME=$IS_ADMIN_USERNAME" "IS_ADMIN_PASSWORD=$IS_ADMIN_PASSWORD" \
  "PRODUCT_CONF_PATH=$is_conf" \
  "DB_TYPE=mysql" "DB_USER=$DB_USER" "DB_PASS=$DB_PASS" "DB_HOST=$DB_HOST" "DB_DRIVER=com.mysql.jdbc.Driver" \
  "DB_IDENTITY=${DB_PREFIX}identitydb" "DB_USER_STORE=${DB_PREFIX}userdb" \
  "DB_IS_CONFIG=${DB_PREFIX}iskm_configdb" "DB_FS_STORE=${DB_PREFIX}consentdb"

# merge.sh and configure.sh expect to be run from their own bin folder.
log "Running merge.sh and configure.sh for API Manager"
(cd "$FSAM_HOME/bin" && bash merge.sh "$APIM_HOME" && bash configure.sh "$APIM_HOME")
log "Running merge.sh and configure.sh for Identity Server"
(cd "$FSIAM_HOME/bin" && bash merge.sh "$IS_HOME" && bash configure.sh "$IS_HOME")

events_sql="$IS_HOME/dbscripts/financial-services/event-notifications/mysql.sql"
if [ -f "$events_sql" ]; then
  log "Creating the event notification tables"
  mysql -u"$DB_USER" $mysql_pass -h"$DB_HOST" -D"${DB_PREFIX}consentdb" -e "SOURCE $events_sql"
fi

ok "Accelerators installed. The databases use the prefix '$DB_PREFIX'."
