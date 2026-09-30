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

package org.wso2.non.regulated.ob.extensions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.constants.CommonConstants;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.model.PreProcessConsentRequestBody;
import org.wso2.non.regulated.ob.extensions.model.PreProcessConsentRetrievalData;
import org.wso2.non.regulated.ob.extensions.model.StoredBasicConsentResourceData;
import org.wso2.non.regulated.ob.extensions.utils.ConsentRevokeUtil;
import org.wso2.non.regulated.ob.extensions.utils.FailedResponseUtil;

import javax.ws.rs.core.Response;

/**
 * Routes a consent revocation to the rules for the consent type being revoked.
 */
public class PreProcessConsentRevokeApiImpl {

    private static final Log log = LogFactory.getLog(PreProcessConsentRevokeApiImpl.class);

    /**
     * Decides whether the consent the client asked to delete may be revoked.
     *
     * @param preProcessConsentRequestBody the accelerator's request, carrying the stored consent resource
     * @return the revocation decision, or an error response if the consent type may not be revoked
     */
    public static Response handleConsentRevocation(PreProcessConsentRequestBody preProcessConsentRequestBody) {

        String requestId = preProcessConsentRequestBody.getRequestId();
        PreProcessConsentRetrievalData data = preProcessConsentRequestBody.getData();
        StoredBasicConsentResourceData consentResource = data.getConsentResource();
        String authDetailsType = consentResource.getType();

        if (CommonConstants.ACCOUNT_INFORMATION.equals(authDetailsType)) {
            return Response.status(Response.Status.OK)
                    .entity(ConsentRevokeUtil.validateAccountConsentRevocation(data.getConsentId(),
                            consentResource, requestId).toString())
                    .build();
        }

        log.error(ErrorConstants.INVALID_CONSENT_TYPE);
        JSONObject error = FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                FailedResponseUtil.buildFormattedError(ErrorConstants.BAD_REQUEST, ErrorConstants.CONSENT_DELETE_ERROR,
                ErrorConstants.RESOURCE_CONSENT_MISMATCH, ErrorConstants.INVALID_CONSENT_TYPE));
        return Response.status(Response.Status.OK).entity(error.toString()).build();
    }

}
