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
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.model.Error;
import org.wso2.non.regulated.ob.extensions.model.Response200ForErrorMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Rewrites an accelerator error into the error format the client APIs publish.
 */
public class ErrorMappingUtil {

    /**
     * The message reported to the client for each accelerator operation.
     */
    private static final Map<String, String> OPERATION_MESSAGES;

    static {
        Map<String, String> messages = new HashMap<>();
        messages.put("consent_default", ErrorConstants.CONSENT_DEFAULT_ERROR);
        messages.put("consent_create", ErrorConstants.CONSENT_CREATE_ERROR);
        messages.put("consent_retrieve", ErrorConstants.CONSENT_RETRIEVE_ERROR);
        messages.put("consent_delete", ErrorConstants.CONSENT_DELETE_ERROR);
        messages.put("consent_update", ErrorConstants.CONSENT_UPDATE_ERROR);
        messages.put("consent_partial_update", ErrorConstants.CONSENT_PARTIAL_UPDATE_ERROR);
        messages.put("consent_file_upload", ErrorConstants.CONSENT_FILE_UPLOAD_ERROR);
        messages.put("consent_file_retrieval", ErrorConstants.CONSENT_FILE_RETRIEVAL_ERROR);
        OPERATION_MESSAGES = Collections.unmodifiableMap(messages);
    }

    /**
     * Maps an accelerator error to the error response published by the APIs.
     *
     * @param error     the accelerator's error, or null if it supplied none
     * @param requestId the accelerator's request ID, echoed back in the response
     * @return the mapped error response
     */
    public static JSONObject mapAcceleratorError(Error error, String requestId) {

        String operation = error == null ? null : error.getOperation();
        String description = StringUtils.defaultIfBlank(error == null ? null : error.getDescription(),
                ErrorConstants.UNEXPECTED_ERROR_MESSAGE);
        Integer httpStatus = parseHttpStatus(error == null ? null : error.getCode());

        Response200ForErrorMapper response = new Response200ForErrorMapper();
        response.setResponseId(requestId);
        response.setErrorCode(httpStatus);
        response.setData(FailedResponseUtil.buildFormattedError(httpStatus, OPERATION_MESSAGES
                        .getOrDefault(operation, ErrorConstants.UNEXPECTED_ERROR_MESSAGE),
                resolveErrorCode(description), description));

        return new JSONObject(response);
    }

    /**
     * Classifies an error from its description, the only account of the failure the accelerator gives.
     *
     * @param description the accelerator's error description
     * @return the error code
     */
    private static String resolveErrorCode(String description) {

        boolean reportsSomethingAbsent = StringUtils.containsIgnoreCase(description, "missing")
                || StringUtils.containsIgnoreCase(description, "not found");

        return reportsSomethingAbsent ? ErrorConstants.FIELD_MISSING : ErrorConstants.FIELD_INVALID;
    }

    /**
     * Reads the accelerator's error code as an HTTP status.
     *
     * @param code the accelerator's error code
     * @return the HTTP status, or null if the code is not a number
     */
    private static Integer parseHttpStatus(String code) {

        if (!StringUtils.isNumeric(code)) {
            return null;
        }
        return Integer.valueOf(code);
    }
}
