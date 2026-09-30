/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com). All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.non.regulated.ob.extensions.constants;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Common constants that don't fall under any specific class.
 */
public class CommonConstants {

    // The RAR authorization detail types.
    public static final String ACCOUNT_INFORMATION = "account_information_v1.0";
    public static final String DOMESTIC_PAYMENT = "domestic_payment_v1.0";
    public static final String DOMESTIC_SCHEDULED_PAYMENT = "domestic_scheduled_payment_v1.0";
    public static final String DOMESTIC_STANDING_ORDER = "domestic_standing_order_v1.0";
    public static final String INTERNATIONAL_PAYMENT = "international_payment_v1.0";

    // Other common constants.
    public static final String ELECTED_RESOURCE = "electedResource";
    public static final String RESOURCE_PARAMS = "resourceParams";
    public static final String RESOURCE_PATH = "ResourcePath";
    public static final String RECEIPT = "receipt";
    public static final String AUTHORIZATION_DETAILS_TYPE = "type";
    public static final String AUTHORIZATION_DETAILS = "authorization_details";
    public static final String STATUS = "status";
    public static final String ACCOUNT_ID = "accountId";
    public static final String DISPLAY_NAME = "displayName";
    public static final String USER_ID_KEY_NAME = "userID";
    public static final String DATA_SIMPLE = "data";
    public static final String AUTHORIZATIONS_KEY = "authorizations";
    public static final String RESOURCES = "resources";
    public static final String BODY = "body";
    public static final String CONSENT_ID_KEY_NAME = "consentId";
    public static final String CONSENT_RESOURCE_ID = "id";

    // API resource-path patterns and endpoint related constants.
    public static final String ACCOUNT_REGEX = "/accounts";
    public static final String BALANCES_REGEX = "/balances";
    public static final String TRANSACTIONS_REGEX = "/transactions";
    public static final String ACCOUNT_ID_REGEX = "/accounts/[^/?]*";
    public static final String BALANCES_ID_REGEX = "/accounts/[^/?]*/balances";
    public static final String TRANSACTIONS_ID_REGEX = "/accounts/[^/?]*/transactions";
    public static final String TRANSACTIONS = "transactions";
    public static final String TO_BOOKING_DATE_TIME = "toBookingDateTime";
    public static final String FROM_BOOKING_DATE_TIME = "fromBookingDateTime";
    public static final String DOMESTIC_PAYMENTS_PATH = "domestic-payments";
    public static final String INTERNATIONAL_PAYMENTS_PATH = "international-payments";
    public static final String SCHEDULED_PAYMENTS_PATH = "scheduled-payments";
    public static final String STANDING_ORDERS_PATH = "standing-orders";
    public static final String DOMESTIC_STANDING_ORDERS_PATH = "domestic-standing-orders";
    public static final String INTERNATIONAL_PATH = "international";

    // Date-time formats accepted in addition to java.time's built-in ISO parser.
    public static final DateTimeFormatter BASIC_ISO_DATE_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .optionalStart().appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .optionalStart().appendZoneId().optionalEnd()
            .optionalStart().appendOffset("+HHmm", "+0000").optionalEnd()
            .optionalStart().appendOffset("+HH:mm", "+00:00").optionalEnd()
            .optionalStart().appendOffset("+HH", "+00").optionalEnd()
            .toFormatter();

    public static final DateTimeFormatter EXTENDED_ISO_DATE_TIME = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendZoneId().optionalEnd()
            .optionalStart().appendOffset("+HHmm", "+0000").optionalEnd()
            .optionalStart().appendOffset("+HH:mm", "+00:00").optionalEnd()
            .optionalStart().appendOffset("+HH", "+00").optionalEnd()
            .optionalStart()
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .toFormatter();

}
