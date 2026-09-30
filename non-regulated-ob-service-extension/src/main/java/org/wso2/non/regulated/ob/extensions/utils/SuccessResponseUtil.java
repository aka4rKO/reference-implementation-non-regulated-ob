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

import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponse;

/**
 * Builds the plain success arm of a service extension's 200 response.
 *
 * Used by Validate Consent Access only. The other extensions return extension-specific data alongside
 * the status.
 */
public class SuccessResponseUtil {

    /**
     * Builds a success response, returned when a request passes validation.
     *
     * @param requestId the request ID
     * @return the success response
     */
    public static JSONObject getSuccessResponse(String requestId) {

        SuccessResponse successResponse = new SuccessResponse();
        successResponse.setResponseId(requestId);
        successResponse.setStatus(SuccessResponse.StatusEnum.SUCCESS);

        return new JSONObject(successResponse);
    }
}
