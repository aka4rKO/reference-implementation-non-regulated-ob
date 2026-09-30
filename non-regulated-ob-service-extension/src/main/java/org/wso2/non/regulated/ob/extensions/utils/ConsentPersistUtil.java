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
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.configurations.ConfigurableProperties;
import org.wso2.non.regulated.ob.extensions.constants.CommonConstants;
import org.wso2.non.regulated.ob.extensions.constants.FieldNameConstants;
import org.wso2.non.regulated.ob.extensions.enums.ConsentStatus;
import org.wso2.non.regulated.ob.extensions.exceptions.ConsentException;
import org.wso2.non.regulated.ob.extensions.model.Account;
import org.wso2.non.regulated.ob.extensions.model.Authorization;
import org.wso2.non.regulated.ob.extensions.model.AuthorizedResourcesAuthorizedDataInner;
import org.wso2.non.regulated.ob.extensions.model.DetailedConsentResourceDataWithAmendments;
import org.wso2.non.regulated.ob.extensions.model.PersistAuthorizedConsent;
import org.wso2.non.regulated.ob.extensions.model.PersistAuthorizedConsentRequestBody;
import org.wso2.non.regulated.ob.extensions.model.Resource;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePersistAuthorizedConsentData;
import org.wso2.non.regulated.ob.extensions.model.UserGrantedData;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the consent record persisted once a customer authorizes or rejects a consent.
 */
public class ConsentPersistUtil {

    /**
     * Builds the consent record for the authorization of a consent.
     *
     * @param requestBody the request body containing the approval decision and the authorization_details
     * @return the consent record to persist
     * @throws ConsentException        if the request is missing required data
     * @throws JsonProcessingException on a JSON processing failure
     */
    public static SuccessResponsePersistAuthorizedConsentData persistConsent(
            PersistAuthorizedConsentRequestBody requestBody) throws ConsentException, JsonProcessingException {

        PersistAuthorizedConsent requestData = requestBody.getData();

        if (requestData.getConsentId() == null) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "Consent ID not available in consent data");
        }

        UserGrantedData userGrantedData = requestData.getUserGrantedData();
        JSONObject authorizationDetails = CommonUtil.getAuthorizationDetails(userGrantedData);
        String authDetailsType = authorizationDetails.getString(CommonConstants.AUTHORIZATION_DETAILS_TYPE);

        boolean isApproved = Boolean.TRUE.equals(requestData.getIsApproved());
        String status = isApproved
                ? ConsentStatus.AUTHORISED.getValue()
                : ConsentStatus.REJECTED.getValue();

        DetailedConsentResourceDataWithAmendments consentResource = new DetailedConsentResourceDataWithAmendments();
        consentResource.setType(authDetailsType);
        consentResource.setStatus(status);
        consentResource.setReceipt(authorizationDetails.toMap());
        consentResource.setValidityTime(getExpirationDateTime(authorizationDetails));
        consentResource.setFrequency(0);
        consentResource.setRecurringIndicator(false);
        consentResource.setAuthorizations(Collections.singletonList(
                buildAuthorization(authDetailsType, status, userGrantedData)));

        SuccessResponsePersistAuthorizedConsentData response = new SuccessResponsePersistAuthorizedConsentData();
        response.setConsentResource(consentResource);
        return response;
    }

    /**
     * Builds the sole authorization resource from the accounts the customer selected on the authorize screen.
     *
     * @param authDetailsType the RAR "type" discriminator
     * @param status          the consent status
     * @param userGrantedData the accounts the customer approved on the authorize screen
     * @return the authorization resource
     */
    private static Authorization buildAuthorization(String authDetailsType, String status,
                                                     UserGrantedData userGrantedData) {

        Authorization authorization = new Authorization();
        authorization.setUserId(userGrantedData.getUserId());
        authorization.setType("authorisation");
        authorization.setStatus(status);
        authorization.setResources(buildResources(authDetailsType, userGrantedData));
        return authorization;
    }

    /**
     * Builds one {@code Resource} per account the customer selected.
     *
     * @param authDetailsType the RAR "type" discriminator, which decides the resource permission
     * @param userGrantedData the accounts the customer approved on the authorize screen
     * @return one resource per approved account
     */
    private static List<Resource> buildResources(String authDetailsType, UserGrantedData userGrantedData) {

        String permission = CommonUtil.isPaymentType(authDetailsType) ? "primary" : "n/a";

        List<Resource> resources = new ArrayList<>();
        for (AuthorizedResourcesAuthorizedDataInner authorizedDataInner :
                userGrantedData.getAuthorizedResources().getAuthorizedData()) {
            for (Account account : authorizedDataInner.getAccounts()) {
                Resource resource = new Resource();
                resource.setAccountId(account.getDisplayName());
                resource.setPermission(permission);
                resource.setStatus("active");
                resources.add(resource);
            }
        }
        return resources;
    }

    /**
     * Calculates the expiration date/time for the consent based on the provided authorization details.
     *
     * @param authorizationDetails the JSON object containing the authorization details
     * @return the expiration date/time in epoch seconds
     */
    private static long getExpirationDateTime(JSONObject authorizationDetails) {

        long epochSeconds;

        if (authorizationDetails.has(FieldNameConstants.EXPIRATION_DATE)) {
            String expirationDateTime = authorizationDetails.getString(FieldNameConstants.EXPIRATION_DATE);

            // Convert to Instant
            Instant instant = OffsetDateTime.parse(expirationDateTime).toInstant();

            // Get epoch value In seconds
            epochSeconds = instant.getEpochSecond();
        } else {

            long days = Long.parseLong(ConfigurableProperties.ACCOUNTS_CONSENT_VALIDITY_PERIOD);

            // Get epoch seconds by adding days to epoch start (1970-01-01T00:00:00Z)
            epochSeconds = Instant.EPOCH.plus(days, ChronoUnit.DAYS).getEpochSecond();
        }

        return epochSeconds;
    }
}
