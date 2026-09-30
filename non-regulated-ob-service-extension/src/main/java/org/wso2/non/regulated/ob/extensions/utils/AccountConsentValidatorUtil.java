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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.configurations.ConfigurableProperties;
import org.wso2.non.regulated.ob.extensions.constants.CommonConstants;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.constants.FieldNameConstants;
import org.wso2.non.regulated.ob.extensions.enums.ConsentStatus;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates an account information request against the consent that authorized it.
 */
public class AccountConsentValidatorUtil {

    private static final Log log = LogFactory.getLog(AccountConsentValidatorUtil.class);

    /**
     * Validates an account retrieval request against its consent.
     *
     * @param dataPayload    the incoming account request, including the elected resource and query params
     * @param consentPayload the consent resource the access token is bound to
     * @param requestId      the accelerator's request ID, echoed back in the response
     * @return a success response, or the first validation error found
     * @throws JsonProcessingException if either payload cannot be serialized to JSON
     */
    public static JSONObject validateAccountAccess(Object dataPayload, Object consentPayload, String requestId)
            throws JsonProcessingException {

        JSONObject jsonDataRequestBody = CommonUtil.convertObjectToJson(dataPayload);
        JSONObject jsonConsentRequestBody = CommonUtil.convertObjectToJson(consentPayload);

        String resourcePath = jsonDataRequestBody.getJSONObject(CommonConstants.RESOURCE_PARAMS)
                .getString(CommonConstants.RESOURCE_PATH);
        String electedResource = jsonDataRequestBody.getString(CommonConstants.ELECTED_RESOURCE);

        // The requested path must be one of the account resources this extension knows how to serve.
        if (resourcePath == null || !isAccountURIValid(electedResource)) {
            log.error(ErrorConstants.PAYLOAD_FORMAT_ERROR);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_INVALID_FORMAT,
                    ErrorConstants.INVALID_URI_ERROR));
        }

        JSONObject receiptJson = jsonConsentRequestBody.getJSONObject(CommonConstants.RECEIPT);

        // The consent must grant the permission that the requested resource requires.
        JSONArray permissions = (JSONArray) receiptJson.get(FieldNameConstants.PERMISSIONS);
        if (!validateAccountPermissions(electedResource, permissions)) {
            log.error(ErrorConstants.PERMISSION_MISMATCH_ERROR);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_INVALID,
                    ErrorConstants.PERMISSION_MISMATCH_ERROR));
        }

        if (isConsentExpired((String) receiptJson.get(FieldNameConstants.EXPIRATION_DATE))) {
            log.error(ErrorConstants.CONSENT_EXPIRED_ERROR);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_INVALID,
                    ErrorConstants.CONSENT_EXPIRED_ERROR));
        }

        String consentStatus = jsonConsentRequestBody.getString(CommonConstants.STATUS);
        if (!ConsentStatus.AUTHORISED.getValue().equals(consentStatus)) {
            log.error(ErrorConstants.ACCOUNT_CONSENT_STATE_INVALID);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_INVALID_CONSENT_STATUS,
                    ErrorConstants.ACCOUNT_CONSENT_STATE_INVALID));
        }

        // Account-ID validation is off by default; see ConfigurableProperties.VALIDATE_ACCOUNT_ID for why.
        if (Boolean.parseBoolean(ConfigurableProperties.VALIDATE_ACCOUNT_ID) &&
                !isAccountIdValid(jsonConsentRequestBody, resourcePath)) {
            log.error(ErrorConstants.ACCOUNT_ID_NOT_AVAILABLE_MSG);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_INVALID,
                    ErrorConstants.ACCOUNT_ID_NOT_AVAILABLE_MSG));
        }

        Map<String, String> resourceParams = extractQueryParams(jsonDataRequestBody);
        JSONObject queryParamValidity = checkTransactionTimePeriodValidity(resourcePath, receiptJson, resourceParams,
                requestId);
        if (CommonUtil.isError(queryParamValidity)) {
            return queryParamValidity;
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Whether the URI is an Account Information API path.
     *
     * @param uri the resource path
     * @return true if it matches
     */
    private static boolean isAccountURIValid(String uri) {
        return getAccountAPIPathRegexArray().stream().anyMatch(uri::contains);
    }

    /**
     * Validates the account permissions.
     *
     * @param requestedURL the request URL
     * @param permissions  the permissions
     * @return true if valid
     */
    private static boolean validateAccountPermissions(String requestedURL, JSONArray permissions) {

        Set<String> permissionSet = new HashSet<>();
        for (int i = 0; i < permissions.length(); i++) {
            permissionSet.add(permissions.getString(i));
        }

        if (requestedURL != null) {

            if (requestedURL.matches(CommonConstants.ACCOUNT_REGEX) ||
                    requestedURL.matches(CommonConstants.ACCOUNT_ID_REGEX)) {

                return permissionSet.contains(FieldNameConstants.READ_ACCOUNTS_BASIC) ||
                        permissionSet.contains(FieldNameConstants.READ_ACCOUNTS_DETAIL);
            } else if (requestedURL.matches(CommonConstants.BALANCES_REGEX) ||
                    requestedURL.matches(CommonConstants.BALANCES_ID_REGEX)) {
                return permissionSet.contains(FieldNameConstants.READ_BALANCES);
            } else if (requestedURL.matches(CommonConstants.TRANSACTIONS_REGEX) ||
                    requestedURL.matches(CommonConstants.TRANSACTIONS_ID_REGEX)) {
                return (permissionSet.contains(FieldNameConstants.READ_TRANSACTIONS_BASIC) ||
                        permissionSet.contains(FieldNameConstants.READ_TRANSACTIONS_DETAIL)) &&
                        (permissionSet.contains(FieldNameConstants.READ_TRANSACTIONS_CREDITS) ||
                                permissionSet.contains(FieldNameConstants.READ_TRANSACTIONS_DEBITS));
            }

        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Validation of permissions failed for resource: %s Permissions: %s",
                    requestedURL == null ? "null" : requestedURL.replaceAll("\r\n", ""),
                    permissions.toString().replaceAll("\r\n", "")));
        }
        return false;
    }

    /**
     * Whether the consent has expired.
     *
     * @param expDateVal the expiration date
     * @return true if expired
     */
    private static boolean isConsentExpired(String expDateVal) {

        if (expDateVal != null && !expDateVal.isEmpty()) {
            OffsetDateTime expDate = OffsetDateTime.parse(expDateVal);
            return OffsetDateTime.now().isAfter(expDate);
        } else {
            return false;
        }
    }

    /**
     * Extracts the request's query parameters.
     *
     * @param jsonSubmissionRequestBody the request body
     * @return the query parameters
     */
    private static Map<String, String> extractQueryParams(JSONObject jsonSubmissionRequestBody) {

        JSONObject resourceParams = jsonSubmissionRequestBody.getJSONObject(CommonConstants.RESOURCE_PARAMS);

        Map<String, String> resourceParamsMap = new HashMap<>();
        for (String key : resourceParams.keySet()) {
            resourceParamsMap.put(key, resourceParams.getString(key));
        }
        return resourceParamsMap;
    }

    /**
     * Checks the requested transaction period against the consent.
     *
     * @param resourcePath    the request path
     * @param jsonRequestBody the consent
     * @param queryParams     the query parameters
     * @param requestId       the request ID
     * @return success, or an error response
     */
    private static JSONObject checkTransactionTimePeriodValidity(String resourcePath, JSONObject jsonRequestBody,
                                                                  Map<String, String> queryParams,
                                                                  String requestId) {

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;
        LocalDateTime transactionFromDateTime = null;
        LocalDateTime transactionToDateTime = null;
        boolean isFromDateValid = true;
        boolean isToDateValid = true;

        try {
            if (resourcePath.contains(CommonConstants.TRANSACTIONS)) {

                fromDateTime = extractURIDateParameter(queryParams
                        .get(CommonConstants.FROM_BOOKING_DATE_TIME));
                toDateTime = extractURIDateParameter(queryParams
                        .get(CommonConstants.TO_BOOKING_DATE_TIME));
            }

            String transactionFromDateFromRequest = jsonRequestBody.getString(FieldNameConstants.TRANSACTION_FROM_DATE);
            String transactionToDateFromRequest = jsonRequestBody.getString(FieldNameConstants.TRANSACTION_TO_DATE);

            if (transactionFromDateFromRequest != null) {
                transactionFromDateTime = LocalDateTime.parse(transactionFromDateFromRequest,
                        DateTimeFormatter.ISO_DATE_TIME);
            }

            if (transactionToDateFromRequest != null) {
                transactionToDateTime = LocalDateTime.parse(transactionToDateFromRequest,
                        DateTimeFormatter.ISO_DATE_TIME);
            }

        } catch (DateTimeParseException e) {
            log.error(ErrorConstants.WRONG_DATE_FORMAT_QUERY);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_INVALID_DATE,
                    ErrorConstants.WRONG_DATE_FORMAT_QUERY));
        }

        if ((transactionFromDateTime != null) || (transactionToDateTime != null)) {
            if (fromDateTime != null) {
                if (transactionFromDateTime != null &&
                        fromDateTime.compareTo(transactionFromDateTime) < 0) {
                    isFromDateValid = false;
                }
                if (transactionToDateTime != null && fromDateTime.compareTo(transactionToDateTime) > 0) {
                    isFromDateValid = false;
                }
            }
            if (toDateTime != null) {
                if (transactionToDateTime != null && toDateTime.compareTo(transactionToDateTime) > 0) {
                    isToDateValid = false;
                }
                if (transactionFromDateTime != null && toDateTime.compareTo(transactionFromDateTime) < 0) {
                    isToDateValid = false;
                }
            }

            if (!(isFromDateValid && isToDateValid)) {
                log.error(ErrorConstants.INVALID_QUERY_PARAMS);
                return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                        FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_INVALID_DATE,
                        ErrorConstants.INVALID_QUERY_PARAMS));
            }
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Whether the account ID matches the consent.
     *
     * @param jsonConsentRequestBody the consent
     * @param resourcePath           the request path
     * @return true if valid
     */
    private static boolean isAccountIdValid(JSONObject jsonConsentRequestBody, String resourcePath) {

        JSONArray authorizations = jsonConsentRequestBody
                .getJSONArray(CommonConstants.AUTHORIZATIONS_KEY);
        JSONObject authorisationResource = authorizations.getJSONObject(0);

        JSONArray consentMappingResources = authorisationResource.getJSONArray(CommonConstants.RESOURCES);

        String accountID = consentMappingResources.getJSONObject(0).getString(CommonConstants.ACCOUNT_ID);

        // If no valid account IDs exist, return false
        if (accountID.isEmpty()) {
            return false;
        }

        // Check if /accounts/{accountID} is present
        Pattern pattern = Pattern.compile("/accounts/\\d+");
        Matcher matcher = pattern.matcher(resourcePath);

        if (!matcher.find()) {
            return true;
        }

        // If resourcePath does not contain "{AccountId}", return true
        if (resourcePath.contains("/accounts/" + accountID)) {
            return true;
        }

        // Check if resourcePath contains any valid account ID
        if (resourcePath.contains(accountID)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the Account Information API resource paths.
     *
     * @return the path patterns
     */
    private static List<String> getAccountAPIPathRegexArray() {

        return Arrays.asList(CommonConstants.ACCOUNT_REGEX,
                CommonConstants.BALANCES_REGEX,
                CommonConstants.TRANSACTIONS_REGEX,
                CommonConstants.ACCOUNT_ID_REGEX,
                CommonConstants.BALANCES_ID_REGEX,
                CommonConstants.TRANSACTIONS_ID_REGEX);
    }

    /**
     * Parses a date-time from a query parameter.
     *
     * @param queryParam the raw value
     * @return the parsed date-time, or null
     * @throws DateTimeParseException on an invalid format
     */
    private static LocalDateTime extractURIDateParameter(String queryParam)
            throws DateTimeParseException {

        LocalDateTime dateTime = null;
        if (queryParam != null) {
            URI uri = URI.create(queryParam.replaceAll(":", "%3A"));
            String decodedRequestedDateTime = uri.getPath();
            if (decodedRequestedDateTime != null) {
                if (decodedRequestedDateTime.charAt(4) == '-') {
                    dateTime = LocalDateTime.parse(decodedRequestedDateTime,
                            CommonConstants.EXTENDED_ISO_DATE_TIME);
                } else {
                    dateTime = LocalDateTime.parse(decodedRequestedDateTime,
                            CommonConstants.BASIC_ISO_DATE_TIME);
                }
            }
        }

        return dateTime;
    }
}
