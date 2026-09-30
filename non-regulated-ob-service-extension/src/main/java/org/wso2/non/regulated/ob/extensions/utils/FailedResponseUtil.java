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
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.exceptions.ConsentException;
import org.wso2.non.regulated.ob.extensions.model.FailedResponse;
import org.wso2.non.regulated.ob.extensions.model.FailedResponseInConsentAuthorize;
import org.wso2.non.regulated.ob.extensions.model.FailedResponseInConsentAuthorizeData;

import java.util.UUID;

import javax.ws.rs.core.Response;

/**
 * Builds the error arm of a service extension's 200 response. A request that was understood but broke a
 * business rule is reported this way: the accelerator expects HTTP 200 with the failure in the body.
 * {@link ErrorResponseUtil} covers the HTTP 400 and 500 cases instead.
 *
 * The error body itself comes in two shapes, and picking the wrong one is the easiest mistake here:
 * {@link #buildErrorData} where the accelerator rewrites the failure before the client sees it, and
 * {@link #buildFormattedError} where the body reaches the client unchanged.
 */
public class FailedResponseUtil {

    /**
     * Builds a failed response around a prepared error body. Used by Validate Consent Access and Pre
     * Process Consent Revoke, the two extensions whose 200 response has a `FailedResponse` arm.
     *
     * @param errorCode the HTTP error code to return
     * @param data      the error body, from {@link #buildErrorData} or {@link #buildFormattedError}
     * @return the failed response
     */
    public static JSONObject getFailedResponse(Integer errorCode, JSONObject data) {

        FailedResponse failedResponse = new FailedResponse();
        failedResponse.setStatus(FailedResponse.StatusEnum.ERROR);
        failedResponse.setErrorCode(errorCode);
        failedResponse.setData(data);

        return new JSONObject(failedResponse);
    }

    /**
     * Builds a failed response for a consent failure raised during the authorize flow. Used by Populate
     * Consent Authorize Screen and Persist Authorized Consent, whose error body can also carry the
     * status to move the consent to.
     *
     * @param ex        the exception
     * @param requestId the request ID
     * @return the response
     */
    public static Response getConsentExceptionResponse(ConsentException ex, String requestId) {

        FailedResponseInConsentAuthorizeData data = new FailedResponseInConsentAuthorizeData();
        data.setErrorMessage(ex.getMessage());
        // A rule that both fails the request and rejects the consent carries the status to move it to.
        if (StringUtils.isNotBlank(ex.getNewConsentStatus())) {
            data.setNewConsentStatus(ex.getNewConsentStatus());
        }

        FailedResponseInConsentAuthorize failedResponse = new FailedResponseInConsentAuthorize();
        failedResponse.setResponseId(requestId);
        failedResponse.setStatus(FailedResponseInConsentAuthorize.StatusEnum.ERROR);
        failedResponse.setData(data);

        return Response.status(Response.Status.OK).entity(new JSONObject(failedResponse).toString()).build();
    }

    /**
     * Builds an error body in the format the APIs publish. Use it where the body reaches the client
     * unchanged: Pre Process Consent Revoke, and the accelerator errors the error mapper rewrites.
     *
     * @param httpStatus  the HTTP status, omitted if null or not a known status
     * @param message     the high level message categorising the failure, omitted if blank
     * @param errorCode   the low level error code, omitted if blank
     * @param description the error description, optionally suffixed with {@code :<json path>}
     * @return the error body
     */
    public static JSONObject buildFormattedError(Integer httpStatus, String message, String errorCode,
                                                 String description) {

        // The error messages carry their JSON path after a colon. A tail containing whitespace is prose
        // rather than a path, so it is left in the message.
        int pathSeparator = description == null ? -1 : description.lastIndexOf(':');
        String path = pathSeparator < 0 || description.substring(pathSeparator + 1).matches(".*\\s.*")
                ? null : description.substring(pathSeparator + 1);

        JSONObject errorDetail = new JSONObject();
        putIfPresent(errorDetail, "ErrorCode", errorCode);
        putIfPresent(errorDetail, "Message", path == null ? description : description.substring(0, pathSeparator));
        putIfPresent(errorDetail, "Path", path);
        if (errorDetail.length() == 0) {
            errorDetail.put("Message", ErrorConstants.UNEXPECTED_ERROR_MESSAGE);
        }

        Response.Status status = httpStatus == null ? null : Response.Status.fromStatusCode(httpStatus);

        JSONObject formattedError = new JSONObject();
        formattedError.put("Id", UUID.randomUUID().toString());
        if (status != null) {
            putIfPresent(formattedError, "Code",
                    status.getStatusCode() + " " + status.getReasonPhrase().replace(" ", ""));
        }
        putIfPresent(formattedError, "Message", message);
        formattedError.put("Errors", new JSONArray().put(errorDetail));

        return formattedError;
    }

    /**
     * Builds an error body in the two keys the accelerator reads. Use it where the accelerator rewrites
     * the failure before the client sees it: Validate Consent Access, whose error becomes the gateway's
     * consent enforcement error. Renaming these two keys breaks that rewrite.
     *
     * @param errorMessage     the error message
     * @param errorDescription the error description
     * @return the error body
     */
    public static JSONObject buildErrorData(String errorMessage, String errorDescription) {

        JSONObject data = new JSONObject();
        data.put("errorMessage", errorMessage);
        data.put("errorDescription", errorDescription);

        return data;
    }

    /**
     * Adds a value to an object only when it has one, since every field here is optional.
     *
     * @param target the object to add to
     * @param field  the field name
     * @param value  the value, added only if it is not blank
     */
    private static void putIfPresent(JSONObject target, String field, String value) {

        if (StringUtils.isNotBlank(value)) {
            target.put(field, value);
        }
    }
}
