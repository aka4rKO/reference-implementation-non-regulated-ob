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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.enums.ConsentStatus;
import org.wso2.non.regulated.ob.extensions.model.StoredBasicConsentResourceData;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponseConsentRevocation;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponseConsentRevocationData;

/**
 * Decides whether an account information consent may be revoked, and what revoking it should do.
 */
public class ConsentRevokeUtil {

    private static final Log log = LogFactory.getLog(ConsentRevokeUtil.class);

    /**
     * Decides whether the account information consent the client asked to delete may be revoked.
     *
     * @param consentId       the consent ID taken from the request path
     * @param consentResource the stored consent resource
     * @param requestId       the accelerator's request ID, echoed back in the response
     * @return the revocation success response, or an error response if the consent may not be revoked
     */
    public static JSONObject validateAccountConsentRevocation(String consentId,
                                                              StoredBasicConsentResourceData consentResource,
                                                              String requestId) {

        if (StringUtils.isBlank(consentId)) {
            log.error(ErrorConstants.INVALID_CONSENT_ID);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildFormattedError(ErrorConstants.BAD_REQUEST,
                    ErrorConstants.CONSENT_DELETE_ERROR, ErrorConstants.FIELD_INVALID,
                    ErrorConstants.INVALID_CONSENT_ID));
        }

        // The consent must belong to a client; without one there is nothing the accelerator can bind the
        // revocation to.
        if (StringUtils.isBlank(consentResource.getClientId())) {
            log.error(ErrorConstants.MISSING_CLIENT_ID);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildFormattedError(ErrorConstants.BAD_REQUEST,
                    ErrorConstants.CONSENT_DELETE_ERROR, ErrorConstants.FIELD_MISSING,
                    ErrorConstants.MISSING_CLIENT_ID));
        }

        String consentStatus = consentResource.getStatus();

        // Revoked and Rejected are terminal: there is no authorization left to withdraw.
        if (ConsentStatus.REVOKED.getValue().equals(consentStatus) ||
                ConsentStatus.REJECTED.getValue().equals(consentStatus)) {
            log.error(ErrorConstants.CONSENT_REVOCATION_INVALID_STATUS);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildFormattedError(ErrorConstants.BAD_REQUEST,
                    ErrorConstants.CONSENT_DELETE_ERROR, ErrorConstants.RESOURCE_INVALID_CONSENT_STATUS,
                    ErrorConstants.CONSENT_REVOCATION_INVALID_STATUS));
        }

        SuccessResponseConsentRevocationData data = new SuccessResponseConsentRevocationData();
        data.setRevocationStatusName(ConsentStatus.REVOKED.getValue());
        data.setRequireTokenRevocation(String.valueOf(true));

        SuccessResponseConsentRevocation revocationResponse = new SuccessResponseConsentRevocation();
        revocationResponse.setResponseId(requestId);
        revocationResponse.setStatus(SuccessResponseConsentRevocation.StatusEnum.SUCCESS);
        revocationResponse.setData(data);

        return new JSONObject(revocationResponse);
    }
}
