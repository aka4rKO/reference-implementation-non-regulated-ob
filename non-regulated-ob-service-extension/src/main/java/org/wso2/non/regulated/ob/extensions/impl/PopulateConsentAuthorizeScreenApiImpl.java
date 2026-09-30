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
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.constants.CommonConstants;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.exceptions.ConsentException;
import org.wso2.non.regulated.ob.extensions.model.PopulateConsentAuthorizeScreenRequestBody;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreen;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreenData;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreenDataConsentData;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData;
import org.wso2.non.regulated.ob.extensions.utils.CommonUtil;
import org.wso2.non.regulated.ob.extensions.utils.ConsentScreenUtil;
import org.wso2.non.regulated.ob.extensions.utils.ErrorResponseUtil;
import org.wso2.non.regulated.ob.extensions.utils.FailedResponseUtil;

import javax.ws.rs.core.Response;

/**
 * Implementation class for handling the population of consent authorize screen data.
 */
public class PopulateConsentAuthorizeScreenApiImpl {

    private static final Log log = LogFactory.getLog(PopulateConsentAuthorizeScreenApiImpl.class);

    /**
     * Handles the population of consent authorize screen data.
     *
     * @param requestBody  the request body containing necessary parameters for consent screen population
     * @return the consent and account data for the authorize screen, or an error response if the request is
     *         not valid
     */
    public static Response handlePopulateConsentAuthorizeScreen(PopulateConsentAuthorizeScreenRequestBody requestBody) {

        try {

            JSONObject authorizationDetails = CommonUtil.getAuthorizationDetails(requestBody);
            String authDetailsType = authorizationDetails.getString(CommonConstants.AUTHORIZATION_DETAILS_TYPE);

            // Business rules the RAR JSON schema can't express.
            ConsentScreenUtil.validateAuthorizationDetails(authorizationDetails, authDetailsType);

            SuccessResponsePopulateConsentAuthorizeScreenDataConsentData
                    consentData = ConsentScreenUtil.getConsentData(authorizationDetails, authDetailsType);

            SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData
                    consumerData = ConsentScreenUtil.getConsumerData(requestBody, authorizationDetails,
                            authDetailsType);

            SuccessResponsePopulateConsentAuthorizeScreenData
                    data = new SuccessResponsePopulateConsentAuthorizeScreenData();
            data.setConsentData(consentData);
            data.setConsumerData(consumerData);

            SuccessResponsePopulateConsentAuthorizeScreen
                    response = new SuccessResponsePopulateConsentAuthorizeScreen();
            response.setResponseId(requestBody.getRequestId());
            response.setStatus(SuccessResponsePopulateConsentAuthorizeScreen.StatusEnum.SUCCESS);
            response.setData(data);

            return Response.status(Response.Status.OK).entity(new JSONObject(response).toString()).build();

        } catch (ConsentException ex) {
            log.error("Error while retrieving consent and account data for authorize screen", ex);
            return FailedResponseUtil.getConsentExceptionResponse(ex, requestBody.getRequestId());
        } catch (JSONException e) {
            log.error("Error while processing JSON for consent authorize screen", e);
            return ErrorResponseUtil.buildErrorResponse(Response.Status.BAD_REQUEST,
                    ErrorConstants.INVALID_REQUEST_MSG, e.getMessage());
        }
    }

}
