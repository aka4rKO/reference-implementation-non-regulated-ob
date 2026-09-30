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

package org.wso2.non.regulated.ob.extensions.exceptions;


/**
 * A consent failure raised during the authorize flow, rendered by {@link
 * org.wso2.non.regulated.ob.extensions.utils.FailedResponseUtil} as the error arm of a 200 response.
 */
public class ConsentException extends Exception {

    /**
     * Classifies a consent failure. The numeric code is informational only: consent exceptions are returned
     * to the accelerator as HTTP 200 with an error status in the body, so this value never becomes the
     * response's HTTP status.
     */
    public enum ErrorCode {

        BAD_REQUEST(400),
        INTERNAL_SERVER_ERROR(500);

        private final int code;

        /**
         * @param code the HTTP status this classification corresponds to
         */
        ErrorCode(int code) {
            this.code = code;
        }

        /**
         * @return the HTTP status this classification corresponds to
         */
        public int getCode() {
            return code;
        }
    }

    private final ErrorCode errorCode;
    private String newConsentStatus;

    /**
     * Creates an exception that leaves the consent's status unchanged.
     *
     * @param errorCode how the failure should be classified
     * @param message   the error message returned to the caller
     */
    public ConsentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception that also moves the consent to a new status — used where a business rule both
     * fails the request and rejects the consent, such as an elapsed payment cut-off time.
     *
     * @param errorCode        how the failure should be classified
     * @param message          the error message returned to the caller
     * @param newConsentStatus the status the consent should be moved to
     */
    public ConsentException(ErrorCode errorCode, String message, String newConsentStatus) {
        super(message);
        this.errorCode = errorCode;
        this.newConsentStatus = newConsentStatus;
    }

    /**
     * @return how this failure is classified
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * @return the status the consent should be moved to, or null if it stays as it is
     */
    public String getNewConsentStatus() {
        return newConsentStatus;
    }

}
