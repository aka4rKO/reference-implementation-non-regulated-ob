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

package org.wso2.non.regulated.ob.extensions.configurations;

import java.util.ArrayList;
import java.util.List;

/**
 * Deployment configuration for this extension.
 */
public class ConfigurableProperties {

    // Demo banking backend. A real deployment points these at the bank's core system.
    public static final String SHARABLE_ENDPOINT = "http://localhost:9763/non/regulated/ob/demo/backend/services/" +
            "bankaccounts/bankaccountservice/sharable-accounts";
    public static final String PAYABLE_ENDPOINT = "http://localhost:9763/non/regulated/ob/demo/backend/services/" +
            "bankaccounts/bankaccountservice/payable-accounts";

    // Payment limits and cut-off policy.
    /** Largest instructed amount a single payment consent may carry. */
    public static final String MAX_INSTRUCTED_AMOUNT = "1000.00";
    /** How far ahead a scheduled payment or standing order may be dated, in days. */
    public static final String MAX_FUTURE_PAYMENT_DAYS = "900";
    /** Whether the daily cut-off time is enforced at all. */
    public static final String CUTOFF_DATE_ENABLED = "false";
    /** What to do with a payment received after the cut-off: ACCEPT or REJECT. */
    public static final String CUTOFF_DATE_POLICY = "ACCEPT";
    /** Daily cut-off time, with UTC offset. */
    public static final String DAILY_CUTOFF = "12:30:30+00:00";
    /** Permissions this deployment refuses to grant, even if requested. Empty: no restrictions. */
    public static final List<String> RESTRICTED_PERMISSIONS = new ArrayList<>();

    // Default accounts consent validity.
    /** Days an account consent remains valid once authorized by default if expiration date time not sent. */
    public static final String ACCOUNTS_CONSENT_VALIDITY_PERIOD = "90";

    // Optional validations.
    // TODO: account-ID validation is disabled because it does not work as expected
    /** Whether an account ID in the request URI is valid against the account ID consented to. */
    public static final String VALIDATE_ACCOUNT_ID = "false";
    /** Whether a debtor account named in a payment request must
     * belong to the authenticating customer during initiation. */
    public static final String VALIDATE_DEBTOR_ACC = "true";

}
