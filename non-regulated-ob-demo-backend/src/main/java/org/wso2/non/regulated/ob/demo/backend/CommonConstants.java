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

package org.wso2.non.regulated.ob.demo.backend;

/**
 * Constants shared by the demo backend services.
 */
public class CommonConstants {

    // Payment types, as they appear as a path segment in the payment service URLs.
    public static final String DOMESTIC_PAYMENT = "domestic-payments";
    public static final String DOMESTIC_SCHEDULED_PAYMENT = "domestic-scheduled-payments";
    public static final String DOMESTIC_STANDING_ORDER = "domestic-standing-orders";
    public static final String INTERNATIONAL_PAYMENT = "international-payments";

    // FAPI interaction id header, and the fixed id returned when the caller does not supply one.
    public static final String HEADER_FAPI_INTERACTION_ID = "x-fapi-interaction-id";
    public static final String DEFAULT_INTERACTION_ID = "93bac548-d2de-4546-b106-880a5018460d";

    // Base URL the mock responses use to build their Links.Self values.
    public static final String SELF_LINK_BASE_URL = "https://api.alphabank.com/open-banking/v1.0";

}
