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
import org.wso2.non.regulated.ob.extensions.model.ErrorResponse;

import javax.ws.rs.core.Response;

/**
 * Builds the HTTP 400 and 500 responses. All five extensions declare the same `ErrorResponse` for both,
 * so this is the only place in the module that returns a non-200.
 *
 * Use it when the extension could not process the request at all — a malformed payload, a dependency it
 * could not reach. A request that was understood but broke a business rule is not one of these: that is
 * reported as a 200 by {@link FailedResponseUtil}.
 */
public class ErrorResponseUtil {

    /**
     * Builds a 400 or 500 error response from the two keys the accelerator reads.
     *
     * @param httpStatus       the HTTP status
     * @param errorMessage     the error message label
     * @param errorDescription the error description
     * @return the response
     */
    public static Response buildErrorResponse(Response.Status httpStatus, String errorMessage,
                                              String errorDescription) {

        // Built here rather than through FailedResponseUtil so the HTTP error path stands on its own.
        JSONObject data = new JSONObject();
        data.put("errorMessage", errorMessage);
        data.put("errorDescription", errorDescription);

        ErrorResponse errorResponse = new ErrorResponse().status(ErrorResponse.StatusEnum.ERROR).data(data);

        return Response.status(httpStatus).entity(new JSONObject(errorResponse).toString()).build();
    }
}
