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

package org.wso2.non.regulated.ob.extensions.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.constants.CommonConstants;
import org.wso2.non.regulated.ob.extensions.model.PopulateConsentAuthorizeScreenRequestBody;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponse;
import org.wso2.non.regulated.ob.extensions.model.UserGrantedData;

import java.util.Map;

/**
 * Class for common utility methods.
 */
public class CommonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Extracts authorization_details from a populate-consent-authorize-screen request.
     *
     * @param requestBody the request
     * @return the authorization_details entry
     */
    public static JSONObject getAuthorizationDetails(PopulateConsentAuthorizeScreenRequestBody requestBody) {
        return extractAuthorizationDetails(requestBody.getData().getRequestParameters());
    }

    /**
     * Extracts authorization_details from a persist-authorized-consent request.
     *
     * @param userGrantedData the granted data
     * @return the authorization_details entry
     */
    public static JSONObject getAuthorizationDetails(UserGrantedData userGrantedData) {
        return extractAuthorizationDetails(userGrantedData.getRequestParameters());
    }

    /**
     * Whether the type is one of the payment RAR types.
     *
     * @param authDetailsType the RAR type
     * @return true if it is a payment type
     */
    public static boolean isPaymentType(String authDetailsType) {

        return CommonConstants.DOMESTIC_PAYMENT.equals(authDetailsType) ||
                CommonConstants.DOMESTIC_SCHEDULED_PAYMENT.equals(authDetailsType) ||
                CommonConstants.DOMESTIC_STANDING_ORDER.equals(authDetailsType) ||
                CommonConstants.INTERNATIONAL_PAYMENT.equals(authDetailsType);
    }

    /**
     * Whether a response represents a failure.
     *
     * @param validationResponse the response to check
     * @return true if it is not a success
     */
    public static boolean isError(JSONObject validationResponse) {
        return validationResponse != null && !SuccessResponse.StatusEnum.SUCCESS
                .equals(validationResponse.opt(CommonConstants.STATUS));
    }

    /**
     * Serializes an object to JSON.
     *
     * @param object the object to serialize
     * @return the JSON object
     * @throws JsonProcessingException on a serialization failure
     */
    public static JSONObject convertObjectToJson(Object object) throws JsonProcessingException {

        String jsonString = OBJECT_MAPPER.writeValueAsString(object);
        return new JSONObject(jsonString);
    }

    /**
     * Reads the first authorization_details entry from request parameters.
     *
     * @param requestParameters the request parameters
     * @return the authorization_details entry
     */
    @SuppressWarnings("unchecked")
    private static JSONObject extractAuthorizationDetails(Object requestParameters) {

        JSONObject requestParams = new JSONObject((Map<String, Object>) requestParameters);
        return requestParams.getJSONArray(CommonConstants.AUTHORIZATION_DETAILS).getJSONObject(0);
    }

}
