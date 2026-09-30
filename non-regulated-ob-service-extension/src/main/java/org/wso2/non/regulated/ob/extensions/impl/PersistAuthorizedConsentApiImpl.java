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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.exceptions.ConsentException;
import org.wso2.non.regulated.ob.extensions.model.PersistAuthorizedConsentRequestBody;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePersistAuthorizedConsent;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePersistAuthorizedConsentData;
import org.wso2.non.regulated.ob.extensions.utils.CommonUtil;
import org.wso2.non.regulated.ob.extensions.utils.ConsentPersistUtil;
import org.wso2.non.regulated.ob.extensions.utils.ErrorResponseUtil;
import org.wso2.non.regulated.ob.extensions.utils.FailedResponseUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for persisting a newly-authorized consent.
 */
public class PersistAuthorizedConsentApiImpl {

    private static final Log log = LogFactory.getLog(PersistAuthorizedConsentApiImpl.class);

    /**
     * Handles the persistence of authorized consent data.
     *
     * @param persistAuthorizedConsentRequestBody the request body containing the consent data to be persisted
     * @return the persisted consent record, or an error response if the consent could not be persisted
     */
    public static Response handlePersistAuthorizedConsent(
            PersistAuthorizedConsentRequestBody persistAuthorizedConsentRequestBody) {
        try {
            SuccessResponsePersistAuthorizedConsentData data =
                    ConsentPersistUtil.persistConsent(persistAuthorizedConsentRequestBody);

            SuccessResponsePersistAuthorizedConsent response = new SuccessResponsePersistAuthorizedConsent();
            response.setResponseId(persistAuthorizedConsentRequestBody.getRequestId());
            response.setStatus(SuccessResponsePersistAuthorizedConsent.StatusEnum.SUCCESS);
            response.setData(data);

            return Response.status(Response.Status.OK)
                    .entity(CommonUtil.convertObjectToJson(response).toString()).build();
        } catch (ConsentException ex) {
            log.error("Error while persisting authorized consent", ex);
            return FailedResponseUtil.getConsentExceptionResponse(ex,
                    persistAuthorizedConsentRequestBody.getRequestId());
        } catch (JsonProcessingException | ClassCastException | JSONException e) {
            log.error("Error while processing JSON for persisting authorized consent", e);
            return ErrorResponseUtil.buildErrorResponse(Response.Status.BAD_REQUEST,
                    ErrorConstants.INVALID_REQUEST_MSG, e.getMessage());
        }
    }

}
