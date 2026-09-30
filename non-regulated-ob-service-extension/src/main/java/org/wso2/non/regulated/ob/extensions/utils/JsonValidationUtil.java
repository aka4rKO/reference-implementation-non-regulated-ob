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
import org.wso2.non.regulated.ob.extensions.constants.FieldNameConstants;

/**
 * Field-by-field comparison helpers for submissions against consents.
 */
public class JsonValidationUtil {

    /**
     * Compares two mandatory values.
     *
     * @param value        the submitted value
     * @param consentValue the consent value
     * @return whether they are equal
     */
    public static boolean compareMandatoryParameter(String value, String consentValue) {

        return value != null && StringUtils.equals(value, consentValue);
    }

    /**
     * Compares an optional field.
     *
     * @param field      the field to compare
     * @param submission the submission object
     * @param consent    the consent object
     * @return whether the values agree
     */
    public static boolean compareOptionalParameter(String field, JSONObject submission, JSONObject consent) {

        Object submissionValue = submission.has(field) ? submission.get(field) : null;
        Object consentValue = consent.has(field) ? consent.get(field) : null;

        boolean isSubmissionEmpty = submissionValue == null || "".equals(submissionValue);
        boolean isConsentEmpty = consentValue == null || "".equals(consentValue);

        if (isSubmissionEmpty || isConsentEmpty) {
            return isSubmissionEmpty && isConsentEmpty;
        }
        if (submissionValue instanceof Number && consentValue instanceof Number) {
            return ((Number) submissionValue).doubleValue() == ((Number) consentValue).doubleValue();
        }
        return submissionValue.equals(consentValue);
    }

    /**
     * Checks that a field is present on both sides or neither.
     *
     * @param submission      the submission object
     * @param consent         the consent object
     * @param field           the field to check
     * @param mismatchMessage the error message
     * @return an error, or null if they agree
     */
    public static JSONObject presenceMatchOrError(JSONObject submission, JSONObject consent, String field,
                                                  String mismatchMessage) {

        if (submission.has(field) != consent.has(field)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH, mismatchMessage));
        }
        return null;
    }

    /**
     * Compares an optional field, returning an error on mismatch.
     *
     * @param submission      the submission object
     * @param consent         the consent object
     * @param field           the field to compare
     * @param mismatchMessage the error message
     * @return an error, or null if they agree
     */
    public static JSONObject optionalMatchOrError(JSONObject submission, JSONObject consent, String field,
                                                  String mismatchMessage) {

        if (!compareOptionalParameter(field, submission, consent)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH, mismatchMessage));
        }
        return null;
    }

    /**
     * Compares a mandatory field, erroring if missing or mismatched.
     *
     * @param submission      the submission object
     * @param consent         the consent object
     * @param field           the field to compare
     * @param mismatchMessage the mismatch error message
     * @param missingMessage  the missing-field error message
     * @return an error, or null if they agree
     */
    public static JSONObject mandatoryMatchOrError(JSONObject submission, JSONObject consent, String field,
                                                  String mismatchMessage, String missingMessage) {

        if (!submission.has(field)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING, missingMessage));
        }
        return compareMandatoryOrMismatch(submission, consent, field, mismatchMessage);
    }

    /**
     * Compares a mandatory field, reporting a missing value as a mismatch.
     *
     * @param submission      the submission object
     * @param consent         the consent object
     * @param field           the field to compare
     * @param mismatchMessage the mismatch error message
     * @param missingMessage  the message used when the field is missing
     * @return an error, or null if they agree
     */
    public static JSONObject mandatoryMatchOrMismatch(JSONObject submission, JSONObject consent, String field,
                                                      String mismatchMessage, String missingMessage) {

        if (!submission.has(field)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH, missingMessage));
        }
        return compareMandatoryOrMismatch(submission, consent, field, mismatchMessage);
    }

    /**
     * Compares an Amount/Currency pair.
     *
     * @param submissionAmount        the submission's amount object
     * @param consentAmount           the consent's amount object
     * @param amountMismatchMessage   the amount error message
     * @param currencyMismatchMessage the currency error message
     * @return the first error found, or null if both agree
     */
    public static JSONObject amountMatchOrError(JSONObject submissionAmount, JSONObject consentAmount,
                                                String amountMismatchMessage, String currencyMismatchMessage) {

        JSONObject error = optionalMatchOrError(submissionAmount, consentAmount, FieldNameConstants.AMOUNT,
                amountMismatchMessage);
        if (error != null) {
            return error;
        }
        return optionalMatchOrError(submissionAmount, consentAmount, FieldNameConstants.CURRENCY,
                currencyMismatchMessage);
    }

    /**
     * Returns the first failing check, if any.
     *
     * @param results the checks' results, in the order they were run
     * @return the first error, or null if all pass
     */
    public static JSONObject firstError(JSONObject... results) {

        for (JSONObject result : results) {
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Compares a field present in the submission against the consent.
     *
     * @param submission      the submission object
     * @param consent         the consent object
     * @param field           the field to compare
     * @param mismatchMessage the error message
     * @return an error, or null if they agree
     */
    private static JSONObject compareMandatoryOrMismatch(JSONObject submission, JSONObject consent, String field,
                                                         String mismatchMessage) {

        String submissionValue = submission.optString(field, null);
        String consentValue = consent.optString(field, null);

        if (StringUtils.isEmpty(submissionValue) || !compareMandatoryParameter(submissionValue, consentValue)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH, mismatchMessage));
        }
        return null;
    }
}
