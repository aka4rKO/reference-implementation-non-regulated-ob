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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.constants.CommonConstants;
import org.wso2.non.regulated.ob.extensions.constants.ErrorConstants;
import org.wso2.non.regulated.ob.extensions.constants.FieldNameConstants;
import org.wso2.non.regulated.ob.extensions.enums.ConsentStatus;

import java.util.HashSet;

/**
 * Validates a payment submission against the consent that authorized it.
 */
public class PaymentConsentValidatorUtil {

    private static final String[] OPTIONAL_ADDRESS_FIELDS = {
            FieldNameConstants.ADDRESS_TYPE,
            FieldNameConstants.DEPARTMENT,
            FieldNameConstants.SUB_DEPARTMENT,
            FieldNameConstants.STREET_NAME,
            FieldNameConstants.BUILDING_NUMBER,
            FieldNameConstants.BUILDING_NAME,
            FieldNameConstants.FLOOR,
            FieldNameConstants.UNIT_NUMBER,
            FieldNameConstants.ROOM,
            FieldNameConstants.POST_BOX,
            FieldNameConstants.TOWN_LOCATION_NAME,
            FieldNameConstants.DISTRICT_NAME,
            FieldNameConstants.CARE_OF,
            FieldNameConstants.POST_CODE,
            FieldNameConstants.TOWN_NAME,
            FieldNameConstants.COUNTRY,
    };

    private static final Log log = LogFactory.getLog(PaymentConsentValidatorUtil.class);

    /**
     * Validates a payment API call against its consent, dispatching on the resource being called.
     *
     * @param submissionPayload the incoming payment request
     * @param consentPayload    the consent resource the access token is bound to
     * @param requestId         the accelerator's request ID, echoed back in the response
     * @return a success response, or the first validation error found
     * @throws JsonProcessingException if either payload cannot be serialized to JSON
     */
    public static JSONObject validatePaymentAccess(Object submissionPayload, Object consentPayload,
                                                   String requestId) throws JsonProcessingException {

        JSONObject jsonSubmissionRequestBody = CommonUtil.convertObjectToJson(submissionPayload);
        JSONObject jsonConsentRequestBody = CommonUtil.convertObjectToJson(consentPayload);

        String resourcePath = jsonSubmissionRequestBody.getString(CommonConstants.ELECTED_RESOURCE);
        String initiationConsentId = jsonConsentRequestBody.getString(CommonConstants.CONSENT_RESOURCE_ID);
        String validationConsentId = jsonSubmissionRequestBody.getString(CommonConstants.CONSENT_ID_KEY_NAME);

        // The consent the access token is bound to must be the consent being submitted against.
        if (validationConsentId == null || !validationConsentId.equals(initiationConsentId)) {
            log.error(ErrorConstants.MSG_INVALID_CONSENT_ID);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    ErrorConstants.MSG_INVALID_CONSENT_ID));
        }

        JSONObject consentStatusError = validateConsentStatus(jsonConsentRequestBody);
        if (consentStatusError != null) {
            return consentStatusError;
        }

        JSONObject submissionJson = (JSONObject) jsonSubmissionRequestBody.get(CommonConstants.BODY);
        JSONObject receiptObj = jsonConsentRequestBody.getJSONObject(CommonConstants.RECEIPT);
        JSONObject riskJsonPayload = receiptObj.optJSONObject(FieldNameConstants.RISK);
        JSONObject consentInitiation = receiptObj.getJSONObject(FieldNameConstants.INITIATION);

        if (!(submissionJson.has(FieldNameConstants.DATA) &&
                submissionJson.get(FieldNameConstants.DATA) instanceof JSONObject)) {
            log.error(ErrorConstants.DATA_NOT_FOUND);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING, ErrorConstants.DATA_NOT_FOUND));
        }
        JSONObject submissionData = submissionJson.getJSONObject(FieldNameConstants.DATA);

        if (!(submissionData.has(FieldNameConstants.INITIATION) &&
                submissionData.get(FieldNameConstants.INITIATION) instanceof JSONObject)) {
            log.error(ErrorConstants.INITIATION_NOT_FOUND);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING,
                    ErrorConstants.INITIATION_NOT_FOUND));
        }
        JSONObject submissionInitiation = submissionData.getJSONObject(FieldNameConstants.INITIATION);

        // The consent ID is checked a second time here, inside the request body rather than on the envelope.
        if (!submissionData.has(FieldNameConstants.CONSENT_ID) ||
                submissionData.get(FieldNameConstants.CONSENT_ID) == null ||
                !submissionData.get(FieldNameConstants.CONSENT_ID).equals(initiationConsentId)) {
            log.error(ErrorConstants.MSG_INVALID_CONSENT_ID);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    ErrorConstants.MSG_INVALID_CONSENT_ID));
        }

        JSONObject initiationError = validateInitiation(resourcePath, submissionInitiation, consentInitiation,
                requestId);
        if (CommonUtil.isError(initiationError)) {
            return initiationError;
        }

        return validateRiskBlock(submissionJson, riskJsonPayload, requestId);
    }

    /**
     * Checks that the consent is in a state that still permits a submission.
     *
     * @param jsonConsentRequestBody the consent resource
     * @return an error response if the status forbids a submission, or null if the status is acceptable
     */
    private static JSONObject validateConsentStatus(JSONObject jsonConsentRequestBody) {

        String consentStatus = jsonConsentRequestBody.getString(CommonConstants.STATUS);

        // Only an authorised consent may be submitted against. A consumed consent has already had its
        // payment executed, so accepting a second submission against it would let the same payment go out
        // twice.
        if (ConsentStatus.AUTHORISED.getValue().equals(consentStatus)) {
            return null;
        }

        log.error(ErrorConstants.PAYMENT_CONSENT_STATE_INVALID);
        return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_INVALID_CONSENT_STATUS,
                ErrorConstants.PAYMENT_CONSENT_STATE_INVALID));
    }

    /**
     * Resolves the submission's Risk block and compares it against the consent's.
     *
     * @param submissionJson  the submission body
     * @param riskJsonPayload the consent's risk block, or null if it carried none
     * @param requestId       the accelerator's request ID, echoed back in the response
     * @return a success response, or the validation error found
     */
    private static JSONObject validateRiskBlock(JSONObject submissionJson, JSONObject riskJsonPayload,
                                                String requestId) {

        JSONObject submissionRisk;
        if (submissionJson.has(FieldNameConstants.RISK) &&
                submissionJson.get(FieldNameConstants.RISK) instanceof JSONObject) {
            submissionRisk = (JSONObject) submissionJson.get(FieldNameConstants.RISK);
        // Risk sent as a string only passes when the consent carried no risk data at all, in which case an
        // empty block is compared instead.
        } else if (submissionJson.has(FieldNameConstants.RISK) &&
                submissionJson.get(FieldNameConstants.RISK) instanceof String) {
            if (riskJsonPayload == null || !riskJsonPayload.isEmpty()) {
                log.error(ErrorConstants.RISK_MISMATCH);
                return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                        FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING, ErrorConstants.RISK_MISMATCH));
            }
            submissionRisk = new JSONObject();
        } else {
            log.error(ErrorConstants.RISK_NOT_FOUND);
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING, ErrorConstants.RISK_NOT_FOUND));
        }

        JSONObject riskValidationError = validateRisk(submissionRisk, riskJsonPayload, requestId);
        if (CommonUtil.isError(riskValidationError)) {
            return riskValidationError;
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates a payment submission's Initiation section against the Initiation the consent was authorized
     * with.
     *
     * @param requestPath          resource path of the submission request; the individual field validators use
     *                             it to decide which payment type's rules apply
     * @param submissionInitiation Initiation object taken from the submission request
     * @param consentInitiation    Initiation object taken from the authorized consent
     * @param requestId            the original request's ID, echoed back in the response
     * @return a success response if the submission agrees with the consent, otherwise the first error response
     *         produced by the checks below
     */
    private static JSONObject validateInitiation(String requestPath, JSONObject submissionInitiation,
                                                 JSONObject consentInitiation, String requestId) {

        if (submissionInitiation == null || consentInitiation == null) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_INVALID,
                    ErrorConstants.MISSING_INITIATION_OBJECT));
        }

        // Identifiers, instructed amount and requested execution date.
        JSONObject error = firstFailure(
                validateInstructionIdentification(requestPath, submissionInitiation, consentInitiation, requestId),
                validateEndToEndIdentification(requestPath, submissionInitiation, consentInitiation, requestId),
                validateInstructedAmount(requestPath, submissionInitiation, consentInitiation, requestId),
                validateRequestedExecutionDate(requestPath, submissionInitiation, consentInitiation, requestId));
        if (error != null) {
            return error;
        }

        // MandateRelatedInformation sits between the requested execution date and the frequency: it must stay
        // between the two runs of consent-validator checks so that failures keep surfacing in the same order.
        error = validateOptionalNestedObject(submissionInitiation, consentInitiation,
                FieldNameConstants.MANDATE_RELATED_INFORMATION, ErrorConstants.MANDATE_RELATED_INFORMATION_MISMATCH,
                PaymentConsentValidatorUtil::validateMandateRelatedInformation, requestId);
        if (error != null) {
            return error;
        }

        // Schedule, currency and routing scalars.
        error = firstFailure(
                validateFrequency(requestPath, submissionInitiation, consentInitiation, requestId),
                validateFirstPaymentDate(requestPath, submissionInitiation, consentInitiation, requestId),
                validateCurrencyOfTransfer(requestPath, submissionInitiation, consentInitiation, requestId),
                validateChargeBearer(requestPath, submissionInitiation, consentInitiation, requestId),
                validateDestinationCountryCode(requestPath, submissionInitiation, consentInitiation, requestId));
        if (error != null) {
            return error;
        }

        error = JsonValidationUtil.firstError(
                validateAccounts(submissionInitiation, consentInitiation, requestId),
                validateAdditionalPartyDetails(submissionInitiation, consentInitiation, requestId),
                validateAmount(submissionInitiation, consentInitiation, FieldNameConstants.FIRST_PAYMENT_AMOUNT,
                        ErrorConstants.FIRST_PAYMENT_AMOUNT_NOT_FOUND,
                        ErrorConstants.FIRST_PAYMENT_AMOUNT_MISMATCH,
                        ErrorConstants.FIRST_PAYMENT_CURRENCY_MISMATCH),
                validateCreditorAgent(submissionInitiation, consentInitiation),
                validateOptionalNestedObject(submissionInitiation, consentInitiation,
                        FieldNameConstants.REMITTANCE_INFO, ErrorConstants.REMMITANCE_INFO_MISMATCH,
                        PaymentConsentValidatorUtil::validateRemittanceInfo, requestId),
                validateOptionalScalarFields(submissionInitiation, consentInitiation),
                validateAmount(submissionInitiation, consentInitiation, FieldNameConstants.FINAL_PAYMENT_AMOUNT,
                        ErrorConstants.FINAL_PAYMENT_AMOUNT_NOT_FOUND,
                        ErrorConstants.FINAL_PAYMENT_AMOUNT_MISMATCH,
                        ErrorConstants.FINAL_PAYMENT_CURRENCY_MISMATCH),
                validateAmount(submissionInitiation, consentInitiation, FieldNameConstants.RECURRING_AMOUNT,
                        ErrorConstants.RECURRING_PAYMENT_AMOUNT_NOT_FOUND,
                        ErrorConstants.RECURRING_PAYMENT_AMOUNT_MISMATCH,
                        ErrorConstants.RECURRING_PAYMENT_CURRENCY_MISMATCH),
                validateExchangeRateInformation(submissionInitiation, consentInitiation),
                validateCreditor(submissionInitiation, consentInitiation));
        if (error != null) {
            return error;
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Returns the first response carrying an error status, in the given order.
     *
     * @param responses the checks' responses, in the order their failures should be reported
     * @return the first response carrying an error status, or null if every one passed
     */
    private static JSONObject firstFailure(JSONObject... responses) {

        for (JSONObject response : responses) {
            if (CommonUtil.isError(response)) {
                return response;
            }
        }
        return null;
    }

    /**
     * Validates the CreditorAccount, which every payment type requires, followed by the optional
     * DebtorAccount.
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return an error response, or null if both accounts agree with the consent
     */
    private static JSONObject validateAccounts(JSONObject submissionInitiation, JSONObject consentInitiation,
                                               String requestId) {

        if (!submissionInitiation.has(FieldNameConstants.CREDITOR_ACC) ||
                !consentInitiation.has(FieldNameConstants.CREDITOR_ACC)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING,
                    ErrorConstants.CREDITOR_ACC_NOT_FOUND));
        }

        JSONObject response = validateCreditorAcc(
                submissionInitiation.getJSONObject(FieldNameConstants.CREDITOR_ACC),
                consentInitiation.getJSONObject(FieldNameConstants.CREDITOR_ACC), requestId);
        if (CommonUtil.isError(response)) {
            return response;
        }

        return validateOptionalNestedObject(submissionInitiation, consentInitiation, FieldNameConstants.DEBTOR_ACC,
                ErrorConstants.DEBTOR_ACC_MISMATCH, PaymentConsentValidatorUtil::validateDebtorAcc, requestId);
    }

    /**
     * Validates the optional UltimateCreditor and UltimateDebtor parties, then checks that RegulatoryReporting
     * was sent exactly when the consent carried it (its contents are not compared).
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return an error response, or null if all three agree with the consent
     */
    private static JSONObject validateAdditionalPartyDetails(JSONObject submissionInitiation,
                                                             JSONObject consentInitiation, String requestId) {

        return JsonValidationUtil.firstError(
                validateOptionalNestedObject(submissionInitiation, consentInitiation,
                        FieldNameConstants.ULTIMATE_CREDITOR,
                        ErrorConstants.ULTIMATE_CREDITOR_MISMATCH,
                        PaymentConsentValidatorUtil::validateUltimateCreditor, requestId),
                validateOptionalNestedObject(submissionInitiation, consentInitiation,
                        FieldNameConstants.ULTIMATE_DEBTOR,
                        ErrorConstants.ULTIMATE_DEBTOR_MISMATCH,
                        PaymentConsentValidatorUtil::validateUltimateDebtor, requestId),
                JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                        FieldNameConstants.REGULATORY_REPORTING,
                        ErrorConstants.REGULATORY_REPORTING_MISMATCH));
    }

    /**
     * Validates an optional nested object: it must be present in the submission exactly when the consent
     * carried it, and is compared field by field by {@code check} when both sides carry it.
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param field                name of the nested object within the Initiation payload
     * @param mismatchMessage      description to report if only one of the two sides carries the object
     * @param check                the field-by-field comparison to apply when both sides carry the object
     * @param requestId            the request ID
     * @return an error response, or null if the object agrees with the consent or neither side carries it
     */
    private static JSONObject validateOptionalNestedObject(JSONObject submissionInitiation,
                                                          JSONObject consentInitiation, String field,
                                                          String mismatchMessage, NestedObjectCheck check,
                                                          String requestId) {

        JSONObject presenceError = JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                field, mismatchMessage);
        if (presenceError != null) {
            return presenceError;
        }
        if (!submissionInitiation.has(field)) {
            return null;
        }

        JSONObject response = check.validate(submissionInitiation.getJSONObject(field),
                consentInitiation.getJSONObject(field), requestId);
        return CommonUtil.isError(response) ? response : null;
    }

    /**
     * Validates one optional {Amount, Currency} pair — FirstPaymentAmount, FinalPaymentAmount or
     * RecurringPaymentAmount.
     *
     * @param submissionInitiation    the submission Initiation
     * @param consentInitiation       the consent Initiation
     * @param field                   name of the amount object within the Initiation payload
     * @param notFoundMessage         description to report if only one of the two sides carries the object
     * @param amountMismatchMessage   description to report if the amounts disagree
     * @param currencyMismatchMessage description to report if the currencies disagree
     * @return an error response, or null if the amount agrees with the consent or neither side carries it
     */
    private static JSONObject validateAmount(JSONObject submissionInitiation, JSONObject consentInitiation,
                                             String field, String notFoundMessage, String amountMismatchMessage,
                                             String currencyMismatchMessage) {

        JSONObject presenceError = JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                field, notFoundMessage);
        if (presenceError != null) {
            return presenceError;
        }
        if (!submissionInitiation.has(field)) {
            return null;
        }

        return JsonValidationUtil.amountMatchOrError(submissionInitiation.getJSONObject(field),
                consentInitiation.getJSONObject(field), amountMismatchMessage, currencyMismatchMessage);
    }

    /**
     * Validates the optional CreditorAgent: sent exactly when the consent carried it, and with a SchemeName
     * and Identification matching the authorized values.
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @return an error response, or null if the creditor agent agrees with the consent
     */
    private static JSONObject validateCreditorAgent(JSONObject submissionInitiation, JSONObject consentInitiation) {

        JSONObject presenceError = JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                FieldNameConstants.CREDITOR_AGENT, ErrorConstants.CREDITOR_AGENT_MISMATCH);
        if (presenceError != null) {
            return presenceError;
        }
        if (!submissionInitiation.has(FieldNameConstants.CREDITOR_AGENT)) {
            return null;
        }

        JSONObject submissionAgent = submissionInitiation.getJSONObject(FieldNameConstants.CREDITOR_AGENT);
        JSONObject consentAgent = consentInitiation.getJSONObject(FieldNameConstants.CREDITOR_AGENT);

        return JsonValidationUtil.firstError(
                JsonValidationUtil.optionalMatchOrError(submissionAgent, consentAgent,
                        FieldNameConstants.SCHEME_NAME,
                        ErrorConstants.CREDITOR_AGENT_SCHEME_NAME_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(submissionAgent, consentAgent,
                        FieldNameConstants.IDENTIFICATION,
                        ErrorConstants.CREDITOR_AGENT_IDENTIFICATION_MISMATCH));
    }

    /**
     * Validates the remittance information.
     *
     * @param remittanceInformationSubmission the submission
     * @param remittanceInformationInitiation the consent
     * @param requestId                       the request ID
     * @return success, or an error response
     */
    private static JSONObject validateRemittanceInfo(JSONObject remittanceInformationSubmission,
                                                     JSONObject remittanceInformationInitiation,
                                                     String requestId) {

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.presenceMatchOrError(remittanceInformationSubmission,
                        remittanceInformationInitiation, FieldNameConstants.REFERENCE,
                        ErrorConstants.REMMITANCE_REFERENCE_NOT_FOUND),
                JsonValidationUtil.optionalMatchOrError(remittanceInformationSubmission,
                        remittanceInformationInitiation, FieldNameConstants.REFERENCE,
                        ErrorConstants.REMMITANCE_REFERENCE_MISMATCH),
                JsonValidationUtil.presenceMatchOrError(remittanceInformationSubmission,
                        remittanceInformationInitiation, FieldNameConstants.UNSTRUCTURED,
                        ErrorConstants.REMMITANCE_UNSTRUCTURED_NOT_FOUND));
        if (error != null) {
            return error;
        }

        // Unstructured is an array, so it is compared as a whole rather than as a single optional value.
        if (remittanceInformationSubmission.has(FieldNameConstants.UNSTRUCTURED)) {

            JSONArray remittanceInformationUnstructuredSub = (JSONArray) remittanceInformationSubmission
                    .get(FieldNameConstants.UNSTRUCTURED);
            JSONArray remittanceInformationUnstructuredInit = (JSONArray) remittanceInformationInitiation
                    .get(FieldNameConstants.UNSTRUCTURED);

            // org.json's JSONArray does not override equals() (it is reference identity), so the two arrays
            // are compared via their List views instead.
            if (!remittanceInformationUnstructuredSub.toList().equals(remittanceInformationUnstructuredInit.toList())) {
                return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                        FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                        ErrorConstants.REMMITANCE_UNSTRUCTURED_MISMATCH));
            }
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates the optional scalar Initiation fields, each of which is only checked for presence and value.
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @return an error response, or null if every one of them agrees with the consent
     */
    private static JSONObject validateOptionalScalarFields(JSONObject submissionInitiation,
                                                           JSONObject consentInitiation) {

        return JsonValidationUtil.firstError(
                presenceAndValueMatch(submissionInitiation, consentInitiation,
                        FieldNameConstants.LOCAL_INSTRUMENT,
                        ErrorConstants.LOCAL_INSTRUMENT_NOT_FOUND,
                        ErrorConstants.LOCAL_INSTRUMENT_MISMATCH),
                presenceAndValueMatch(submissionInitiation, consentInitiation, FieldNameConstants.REFERENCE,
                        ErrorConstants.REFERENCE_NOT_FOUND,
                        ErrorConstants.REFERENCE_MISMATCH),
                presenceAndValueMatch(submissionInitiation, consentInitiation,
                        FieldNameConstants.INSTRUCTION_PRIORITY,
                        ErrorConstants.INSTRUCTION_PRIORITY_MISSING,
                        ErrorConstants.INSTRUCTION_PRIORITY_MISMATCH),
                presenceAndValueMatch(submissionInitiation, consentInitiation,
                        FieldNameConstants.PURPOSE, ErrorConstants.PURPOSE_MISSING,
                        ErrorConstants.PURPOSE_MISMATCH),
                presenceAndValueMatch(submissionInitiation, consentInitiation,
                        FieldNameConstants.EXTENDED_PURPOSE,
                        ErrorConstants.EXTENDED_PURPOSE_MISSING,
                        ErrorConstants.EXTENDED_PURPOSE_MISMATCH));
    }

    /**
     * Validates an optional scalar field: present in the submission exactly when the consent carried it, and
     * equal to the authorized value.
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param field                name of the field within the Initiation payload
     * @param notFoundMessage      description to report if only one of the two sides carries the field
     * @param mismatchMessage      description to report if the values disagree
     * @return a mismatch error, or null if the field agrees with the consent
     */
    private static JSONObject presenceAndValueMatch(JSONObject submissionInitiation, JSONObject consentInitiation,
                                                    String field, String notFoundMessage,
                                                    String mismatchMessage) {

        JSONObject presenceError = JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                field, notFoundMessage);
        if (presenceError != null) {
            return presenceError;
        }
        return JsonValidationUtil.optionalMatchOrError(submissionInitiation, consentInitiation, field,
                mismatchMessage);
    }

    /**
     * Validates the optional ExchangeRateInformation block. UnitCurrency and RateType are mandatory on both
     * sides once the block itself is present; ExchangeRate and ContractIdentification are optional but must
     * be sent exactly when the consent carried them.
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @return an error response, or null if the block agrees with the consent or neither side carries it
     */
    private static JSONObject validateExchangeRateInformation(JSONObject submissionInitiation,
                                                              JSONObject consentInitiation) {

        JSONObject presenceError = JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                FieldNameConstants.EXCHANGE_RATE_INFO,
                ErrorConstants.EXCHANGE_RATE_INFO_MISMATCH);
        if (presenceError != null) {
            return presenceError;
        }
        if (!submissionInitiation.has(FieldNameConstants.EXCHANGE_RATE_INFO)) {
            return null;
        }

        JSONObject submissionInfo = submissionInitiation
                .getJSONObject(FieldNameConstants.EXCHANGE_RATE_INFO);
        JSONObject consentInfo = consentInitiation.getJSONObject(FieldNameConstants.EXCHANGE_RATE_INFO);

        return JsonValidationUtil.firstError(
                requireOnBothSides(submissionInfo, consentInfo, FieldNameConstants.UNIT_CURRENCY,
                        ErrorConstants.EXCHANGE_RATE_UNIT_CURRENCY_NOT_FOUND),
                JsonValidationUtil.optionalMatchOrError(submissionInfo, consentInfo,
                        FieldNameConstants.UNIT_CURRENCY,
                        ErrorConstants.EXCHNAGE_RATE_UNIT_CURRENCY_MISMATCH),
                JsonValidationUtil.presenceMatchOrError(submissionInfo, consentInfo,
                        FieldNameConstants.EXCHANGE_RATE,
                        ErrorConstants.EXCHANGE_RATE_NOT_FOUND),
                validateExchangeRate(submissionInfo, consentInfo),
                requireOnBothSides(submissionInfo, consentInfo, FieldNameConstants.RATE_TYPE,
                        ErrorConstants.EXCHANGE_RATE_TYPE_NOT_FOUND),
                JsonValidationUtil.optionalMatchOrError(submissionInfo, consentInfo,
                        FieldNameConstants.RATE_TYPE,
                        ErrorConstants.EXCHANGE_RATE_TYPE_MISMATCH),
                JsonValidationUtil.presenceMatchOrError(submissionInfo, consentInfo,
                        FieldNameConstants.CONTRACT_IDENTIFICATION,
                        ErrorConstants.CONTRACT_IDENTIFICATION_NOT_FOUND),
                JsonValidationUtil.optionalMatchOrError(submissionInfo, consentInfo,
                        FieldNameConstants.CONTRACT_IDENTIFICATION,
                        ErrorConstants.CONTRACT_IDENTIFICATION_MISMATCH));
    }

    /**
     * Compares the ExchangeRate, read as a double rather than a string so that numerically equal values
     * written with different precision are not reported as a mismatch.
     *
     * @param submissionInfo the submission ExchangeRateInformation
     * @param consentInfo    the consent ExchangeRateInformation
     * @return a mismatch error, or null if the rates agree or neither side carries one
     */
    private static JSONObject validateExchangeRate(JSONObject submissionInfo, JSONObject consentInfo) {

        if (submissionInfo.has(FieldNameConstants.EXCHANGE_RATE) &&
                consentInfo.has(FieldNameConstants.EXCHANGE_RATE) &&
                submissionInfo.getDouble(FieldNameConstants.EXCHANGE_RATE) !=
                        consentInfo.getDouble(FieldNameConstants.EXCHANGE_RATE)) {

            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    ErrorConstants.EXCHANGE_RATE_MISMATCH));
        }
        return null;
    }

    /**
     * Reports a consent mismatch unless the given field is carried by both the submission and the consent.
     *
     * @param submission      the submission object
     * @param consent         the consent object
     * @param field           name of the field to require
     * @param notFoundMessage description to report if either side omits the field
     * @return a mismatch error, or null if both sides carry the field
     */
    private static JSONObject requireOnBothSides(JSONObject submission, JSONObject consent, String field,
                                                 String notFoundMessage) {

        if (!submission.has(field) || !consent.has(field)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH, notFoundMessage));
        }
        return null;
    }

    /**
     * Validates the optional Creditor: sent exactly when the consent carried it, with a matching Name and
     * PostalAddress.
     *
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @return an error response, or null if the creditor agrees with the consent
     */
    private static JSONObject validateCreditor(JSONObject submissionInitiation, JSONObject consentInitiation) {

        JSONObject presenceError = JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                FieldNameConstants.CREDITOR, ErrorConstants.CREDITOR_MISMATCH);
        if (presenceError != null) {
            return presenceError;
        }
        if (!submissionInitiation.has(FieldNameConstants.CREDITOR)) {
            return null;
        }

        JSONObject submissionCreditor = submissionInitiation.getJSONObject(FieldNameConstants.CREDITOR);
        JSONObject consentCreditor = consentInitiation.getJSONObject(FieldNameConstants.CREDITOR);

        if (submissionCreditor.has(FieldNameConstants.NAME) && consentCreditor.has(FieldNameConstants.NAME) &&
                !JsonValidationUtil.compareMandatoryParameter(submissionCreditor.getString(FieldNameConstants.NAME),
                        consentCreditor.getString(FieldNameConstants.NAME))) {

            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    ErrorConstants.CREDITOR_NAME_MISMATCH));
        }

        // PostalAddress is optional in the RAR schema, so it is read with optJSONObject: isValidAddress treats
        // two absent addresses as a match and a one-sided absence as a mismatch.
        if (!isValidAddress(submissionCreditor.optJSONObject(FieldNameConstants.POSTAL_ADDRESS),
                consentCreditor.optJSONObject(FieldNameConstants.POSTAL_ADDRESS))) {

            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    ErrorConstants.POSTAL_ADDRESS_MISMATCH));
        }
        return null;
    }

    /**
     * Validates InstructionIdentification.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateInstructionIdentification(String requestPath, JSONObject submissionInitiation,
                                                               JSONObject consentInitiation, String requestId) {

        if (requestPath.contains(CommonConstants.STANDING_ORDERS_PATH)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        JSONObject error = JsonValidationUtil.mandatoryMatchOrError(submissionInitiation, consentInitiation,
                FieldNameConstants.INSTRUCTION_IDENTIFICATION,
                ErrorConstants.INSTRUCTION_IDENTIFICATION_MISMATCH,
                ErrorConstants.INSTRUCTION_IDENTIFICATION_NOT_FOUND);
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates EndToEndIdentification.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateEndToEndIdentification(String requestPath, JSONObject submissionInitiation,
                                                            JSONObject consentInitiation, String requestId) {

        if (StringUtils.containsAny(requestPath, CommonConstants.DOMESTIC_PAYMENTS_PATH,
                CommonConstants.INTERNATIONAL_PAYMENTS_PATH)) {

            JSONObject error = JsonValidationUtil.mandatoryMatchOrError(submissionInitiation, consentInitiation,
                    FieldNameConstants.END_TO_END_IDENTIFICATION,
                    ErrorConstants.END_TO_END_IDENTIFICATION_MISMATCH,
                    ErrorConstants.END_TO_END_IDENTIFICATION_NOT_FOUND);
            if (error != null) {
                return error;
            }
        } else if (requestPath.contains(CommonConstants.SCHEDULED_PAYMENTS_PATH)) {

            boolean inSubmission = submissionInitiation.has(FieldNameConstants.END_TO_END_IDENTIFICATION);
            boolean inConsent = consentInitiation.has(FieldNameConstants.END_TO_END_IDENTIFICATION);

            if (inSubmission && inConsent) {
                JSONObject error = JsonValidationUtil.optionalMatchOrError(submissionInitiation, consentInitiation,
                        FieldNameConstants.END_TO_END_IDENTIFICATION,
                        ErrorConstants.END_TO_END_IDENTIFICATION_MISMATCH);
                if (error != null) {
                    return error;
                }
            } else if (inSubmission != inConsent) {
                return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                        FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                        ErrorConstants.END_TO_END_IDENTIFICATION_NOT_FOUND));
            }
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates InstructedAmount.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateInstructedAmount(String requestPath, JSONObject submissionInitiation,
                                                      JSONObject consentInitiation, String requestId) {

        if (requestPath.contains(CommonConstants.DOMESTIC_STANDING_ORDERS_PATH)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }
        if (!submissionInitiation.has(FieldNameConstants.INSTRUCTED_AMOUNT)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING,
                    ErrorConstants.INSTRUCTED_AMOUNT_NOT_FOUND));
        }

        JSONObject subInstrAmount = (JSONObject) submissionInitiation.get(FieldNameConstants.INSTRUCTED_AMOUNT);
        JSONObject initInstrAmount = (JSONObject) consentInitiation.get(FieldNameConstants.INSTRUCTED_AMOUNT);

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.mandatoryMatchOrError(subInstrAmount, initInstrAmount,
                        FieldNameConstants.AMOUNT,
                        ErrorConstants.INSTRUCTED_AMOUNT_AMOUNT_MISMATCH,
                        ErrorConstants.INSTRUCTED_AMOUNT_AMOUNT_NOT_FOUND),
                JsonValidationUtil.mandatoryMatchOrError(subInstrAmount, initInstrAmount,
                        FieldNameConstants.CURRENCY,
                        ErrorConstants.INSTRUCTED_AMOUNT_CURRENCY_MISMATCH,
                        ErrorConstants.INSTRUCTED_AMOUNT_CURRENCY_NOT_FOUND));
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates RequestedExecutionDate.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateRequestedExecutionDate(String requestPath, JSONObject submissionInitiation,
                                                            JSONObject consentInitiation, String requestId) {

        if (!requestPath.contains(CommonConstants.SCHEDULED_PAYMENTS_PATH)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        // An omitted date is reported as a consent mismatch rather than a missing field, as it always has been.
        JSONObject error = JsonValidationUtil.mandatoryMatchOrMismatch(submissionInitiation, consentInitiation,
                FieldNameConstants.REQUEST_EXECUTION_DATE,
                ErrorConstants.REQUESTED_EXECUTION_DATE_MISMATCH,
                ErrorConstants.REQUESTED_EXECUTION_DATE_NOT_FOUND);
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates the standing order's Frequency.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateFrequency(String requestPath, JSONObject submissionInitiation,
                                               JSONObject consentInitiation, String requestId) {

        if (!requestPath.contains(CommonConstants.STANDING_ORDERS_PATH) ||
                !submissionInitiation.has(FieldNameConstants.MANDATE_RELATED_INFORMATION)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        JSONObject submissionMandateRelatedInfo = (JSONObject) submissionInitiation.get(
                FieldNameConstants.MANDATE_RELATED_INFORMATION);
        if (submissionMandateRelatedInfo == null ||
                !submissionMandateRelatedInfo.has(FieldNameConstants.FREQUENCY)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.FIELD_MISSING,
                    ErrorConstants.FREQUENCY_NOT_FOUND_API));
        }

        JSONObject initiationMandateRelatedInfo = consentInitiation.optJSONObject(
                FieldNameConstants.MANDATE_RELATED_INFORMATION);
        if (initiationMandateRelatedInfo == null ||
                !initiationMandateRelatedInfo.has(FieldNameConstants.FREQUENCY)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    ErrorConstants.MANDATE_RELATED_INFORMATION_MISMATCH));
        }

        JSONObject initiationFrequency = (JSONObject) initiationMandateRelatedInfo.get(
                FieldNameConstants.FREQUENCY);
        JSONObject submissionFrequency = (JSONObject) submissionMandateRelatedInfo.get(
                FieldNameConstants.FREQUENCY);

        // All three halves of Frequency report the same description, so the caller cannot tell them apart.
        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.mandatoryMatchOrMismatch(submissionFrequency, initiationFrequency,
                        FieldNameConstants.TYPE,
                        ErrorConstants.FREQUENCY_MISMATCH_API,
                        ErrorConstants.FREQUENCY_MISMATCH_API),
                JsonValidationUtil.optionalMatchOrError(submissionFrequency, initiationFrequency,
                        FieldNameConstants.COUNT_PER_PERIOD,
                        ErrorConstants.FREQUENCY_MISMATCH_API),
                JsonValidationUtil.optionalMatchOrError(submissionFrequency, initiationFrequency,
                        FieldNameConstants.POINT_IN_TIME,
                        ErrorConstants.FREQUENCY_MISMATCH_API));
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates the standing order's FirstPaymentDate.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateFirstPaymentDate(String requestPath, JSONObject submissionInitiation,
                                                      JSONObject consentInitiation, String requestId) {

        if (!requestPath.contains(CommonConstants.STANDING_ORDERS_PATH) ||
                !submissionInitiation.has(FieldNameConstants.MANDATE_RELATED_INFORMATION)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        JSONObject submissionMandateRelatedInfo = (JSONObject) submissionInitiation.get(
                FieldNameConstants.MANDATE_RELATED_INFORMATION);
        if (submissionMandateRelatedInfo == null ||
                !submissionMandateRelatedInfo.has(FieldNameConstants.FIRST_PAYMENT_DATE)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        JSONObject initiationMandateRelatedInfo = consentInitiation.optJSONObject(
                FieldNameConstants.MANDATE_RELATED_INFORMATION);
        if (initiationMandateRelatedInfo == null) {
            initiationMandateRelatedInfo = new JSONObject();
        }

        JSONObject error = JsonValidationUtil.optionalMatchOrError(submissionMandateRelatedInfo,
                initiationMandateRelatedInfo, FieldNameConstants.FIRST_PAYMENT_DATE,
                ErrorConstants.FIRST_PAYMENT_DATE_MISMATCH_API);
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates CurrencyOfTransfer.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateCurrencyOfTransfer(String requestPath, JSONObject submissionInitiation,
                                                        JSONObject consentInitiation, String requestId) {

        if (!requestPath.contains(CommonConstants.INTERNATIONAL_PATH)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        // An omitted currency is reported as a consent mismatch rather than a missing field, as it always has
        // been.
        JSONObject error = JsonValidationUtil.mandatoryMatchOrMismatch(submissionInitiation, consentInitiation,
                FieldNameConstants.CURRENCY_OF_TRANSFER,
                ErrorConstants.CURRENCY_TRANSFER_MISMATCH,
                ErrorConstants.CURRENCY_TRANSFER_NOT_FOUND);
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates ChargeBearer.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateChargeBearer(String requestPath, JSONObject submissionInitiation,
                                                  JSONObject consentInitiation, String requestId) {

        if (!requestPath.contains(CommonConstants.INTERNATIONAL_PATH)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                        FieldNameConstants.CHARGE_BEARER,
                        ErrorConstants.CHARGE_BEARER_NOT_FOUND),
                JsonValidationUtil.optionalMatchOrError(submissionInitiation, consentInitiation,
                        FieldNameConstants.CHARGE_BEARER,
                        ErrorConstants.CHARGE_BEARER_MISMATCH));
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates DestinationCountryCode.
     *
     * @param requestPath          the request path
     * @param submissionInitiation the submission Initiation
     * @param consentInitiation    the consent Initiation
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateDestinationCountryCode(String requestPath, JSONObject submissionInitiation,
                                                            JSONObject consentInitiation, String requestId) {

        if (!requestPath.contains(CommonConstants.INTERNATIONAL_PATH)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.presenceMatchOrError(submissionInitiation, consentInitiation,
                        FieldNameConstants.DESTINATION_COUNTRY_CODE,
                        ErrorConstants.DESTINATION_COUNTRY_CODE_NOT_FOUND),
                JsonValidationUtil.optionalMatchOrError(submissionInitiation, consentInitiation,
                        FieldNameConstants.DESTINATION_COUNTRY_CODE,
                        ErrorConstants.DESTINATION_COUNTRY_CODE_MISMATCH));
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates the Risk block.
     *
     * @param submissionRisk the submission Risk
     * @param initiationRisk the consent Risk
     * @param requestId      the request ID
     * @return success, or an error response
     */
    private static JSONObject validateRisk(JSONObject submissionRisk, JSONObject initiationRisk,
                                          String requestId) {

        // Nothing to compare unless both sides carry a Risk block.
        if (submissionRisk == null || initiationRisk == null) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.optionalMatchOrError(submissionRisk, initiationRisk,
                        FieldNameConstants.CONTEXT_CODE,
                        ErrorConstants.PAYMENT_CONTEXT_CODE_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(submissionRisk, initiationRisk,
                        FieldNameConstants.MERCHANT_CATEGORY_CODE,
                        ErrorConstants.MERCHANT_CATEGORY_CODE_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(submissionRisk, initiationRisk,
                        FieldNameConstants.MERCHANT_IDENTIFICATION,
                        ErrorConstants.MERCHANT_CUSTOMER_IDENTIFICATION_MISMATCH),
                JsonValidationUtil.presenceMatchOrError(submissionRisk, initiationRisk,
                        FieldNameConstants.DELIVERY_ADDRESS,
                        ErrorConstants.DELIVERY_ADDRESS_MISMATCH));
        if (error != null) {
            return error;
        }

        if (!submissionRisk.has(FieldNameConstants.DELIVERY_ADDRESS)) {
            return SuccessResponseUtil.getSuccessResponse(requestId);
        }

        error = validateDeliveryAddress(
                (JSONObject) submissionRisk.get(FieldNameConstants.DELIVERY_ADDRESS),
                (JSONObject) initiationRisk.get(FieldNameConstants.DELIVERY_ADDRESS));
        if (error != null) {
            return error;
        }
        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates MandateRelatedInformation.
     *
     * @param subMandateRelatedInformation  the submission
     * @param initMandateRelatedInformation the consent
     * @param requestId                     the request ID
     * @return success, or an error response
     */
    private static JSONObject validateMandateRelatedInformation(JSONObject subMandateRelatedInformation,
                                                               JSONObject initMandateRelatedInformation,
                                                               String requestId) {

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.optionalMatchOrError(subMandateRelatedInformation,
                        initMandateRelatedInformation, FieldNameConstants.MANDATE_IDENTIFICATION,
                        ErrorConstants.MANDATE_IDENTIFICATION_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(subMandateRelatedInformation,
                        initMandateRelatedInformation, FieldNameConstants.CLASSIFICATION,
                        ErrorConstants.CLASSIFICATION_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(subMandateRelatedInformation,
                        initMandateRelatedInformation, FieldNameConstants.CATEGORY_PURPOSE_CODE,
                        ErrorConstants.CATEGORY_PURPOSE_CODE_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(subMandateRelatedInformation,
                        initMandateRelatedInformation, FieldNameConstants.REASON,
                        ErrorConstants.REASON_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(subMandateRelatedInformation,
                        initMandateRelatedInformation, FieldNameConstants.FIRST_PAYMENT_DATE,
                        ErrorConstants.FIRST_PAYMENT_DATE_MISMATCH_API),
                JsonValidationUtil.optionalMatchOrError(subMandateRelatedInformation,
                        initMandateRelatedInformation, FieldNameConstants.FINAL_PAYMENT_DATE,
                        ErrorConstants.FINAL_PAYMENT_DATE_MISMATCH_API),
                JsonValidationUtil.optionalMatchOrError(subMandateRelatedInformation,
                        initMandateRelatedInformation, FieldNameConstants.RECURRING_PAYMENT_DATE,
                        ErrorConstants.RECURRING_PAYMENT_DATE_MISMATCH_API));
        if (error != null) {
            return error;
        }

        // Frequency is mandatory on both sides for a standing order.
        if (!subMandateRelatedInformation.has(FieldNameConstants.FREQUENCY) ||
                !initMandateRelatedInformation.has(FieldNameConstants.FREQUENCY)) {
            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    ErrorConstants.FREQUENCY_NOT_FOUND_API));
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Validates the creditor account.
     *
     * @param subCreditorAccount  the submission
     * @param initCreditorAccount the consent
     * @param requestId           the request ID
     * @return success, or an error response
     */
    private static JSONObject validateCreditorAcc(JSONObject subCreditorAccount, JSONObject initCreditorAccount,
                                                 String requestId) {

        return validateAccount(subCreditorAccount, initCreditorAccount, requestId, CREDITOR_ACCOUNT_MESSAGES,
                CREDITOR_ACCOUNT_PROXY_MESSAGES);
    }

    /**
     * Validates the debtor account.
     *
     * @param subDebtorAccount  the submission
     * @param initDebtorAccount the consent
     * @param requestId         the request ID
     * @return success, or an error response
     */
    private static JSONObject validateDebtorAcc(JSONObject subDebtorAccount, JSONObject initDebtorAccount,
                                               String requestId) {

        return validateAccount(subDebtorAccount, initDebtorAccount, requestId, DEBTOR_ACCOUNT_MESSAGES,
                DEBTOR_ACCOUNT_PROXY_MESSAGES);
    }

    /**
     * Validates UltimateCreditor.
     *
     * @param subUltimateCreditor  the submission
     * @param initUltimateCreditor the consent
     * @param requestId            the request ID
     * @return success, or an error response
     */
    private static JSONObject validateUltimateCreditor(JSONObject subUltimateCreditor, JSONObject initUltimateCreditor,
                                                      String requestId) {

        return validateParty(subUltimateCreditor, initUltimateCreditor, requestId, ULTIMATE_CREDITOR_MESSAGES);
    }

    /**
     * Validates UltimateDebtor.
     *
     * @param subUltimateDebtor  the submission
     * @param initUltimateDebtor the consent
     * @param requestId          the request ID
     * @return success, or an error response
     */
    private static JSONObject validateUltimateDebtor(JSONObject subUltimateDebtor, JSONObject initUltimateDebtor,
                                                    String requestId) {

        return validateParty(subUltimateDebtor, initUltimateDebtor, requestId, ULTIMATE_DEBTOR_MESSAGES);
    }

    /**
     * Whether a PostalAddress agrees with the consent.
     *
     * @param submissionAddress the submission address, or null
     * @param initiationAddress the consent address, or null
     * @return true if they agree
     */
    private static boolean isValidAddress(JSONObject submissionAddress, JSONObject initiationAddress) {

        // An address on one side only is a mismatch; an address on neither side is nothing to compare.
        if (submissionAddress == null || initiationAddress == null) {
            return submissionAddress == null && initiationAddress == null;
        }

        for (String field : OPTIONAL_ADDRESS_FIELDS) {
            if (!JsonValidationUtil.compareOptionalParameter(field, submissionAddress, initiationAddress)) {
                return false;
            }
        }

        if (submissionAddress.has(FieldNameConstants.ADDRESS_LINE) !=
                initiationAddress.has(FieldNameConstants.ADDRESS_LINE)) {
            return false;

        } else if (submissionAddress.has(FieldNameConstants.ADDRESS_LINE)) {

            // Guarded on presence because AddressLine is optional and getJSONArray throws when it is absent.
            JSONArray subAddressLine = submissionAddress.getJSONArray(FieldNameConstants.ADDRESS_LINE);
            JSONArray initiationAddressLine =
                    initiationAddress.getJSONArray(FieldNameConstants.ADDRESS_LINE);

            if (!new HashSet<>(subAddressLine.toList()).containsAll(initiationAddressLine.toList())) {
                return false;
            }
        }

        return submissionAddress.has(FieldNameConstants.COUNTRY_SUB_DIVISION) ==
                initiationAddress.has(FieldNameConstants.COUNTRY_SUB_DIVISION) &&
                JsonValidationUtil.compareOptionalParameter(FieldNameConstants.COUNTRY_SUB_DIVISION,
                        submissionAddress, initiationAddress);
    }

    /**
     * Validates the Risk DeliveryAddress.
     *
     * @param subAddress  the submission address
     * @param initAddress the consent address
     * @return an error, or null if they agree
     */
    private static JSONObject validateDeliveryAddress(JSONObject subAddress, JSONObject initAddress) {

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.optionalMatchOrError(subAddress, initAddress,
                        FieldNameConstants.STREET_NAME,
                        ErrorConstants.STREET_NAME_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(subAddress, initAddress,
                        FieldNameConstants.BUILDING_NUMBER,
                        ErrorConstants.BUILDING_NUMBER_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(subAddress, initAddress,
                        FieldNameConstants.POST_CODE,
                        ErrorConstants.POST_CODE_MISMATCH),
                JsonValidationUtil.optionalMatchOrError(subAddress, initAddress,
                        FieldNameConstants.TOWN_NAME,
                        ErrorConstants.TOWN_NAME_MISMATCH),
                // Country is the one delivery-address field a submission may not omit.
                JsonValidationUtil.mandatoryMatchOrError(subAddress, initAddress,
                        FieldNameConstants.COUNTRY,
                        ErrorConstants.COUNTRY_MISMATCH,
                        ErrorConstants.COUNTRY_MISMATCH),
                JsonValidationUtil.presenceMatchOrError(subAddress, initAddress,
                        FieldNameConstants.ADDRESS_LINE,
                        ErrorConstants.ADDRESS_LINE_NOT_FOUND));
        if (error != null) {
            return error;
        }

        // AddressLine is an array: every line the consent authorized must still be present in the submission.
        if (subAddress.has(FieldNameConstants.ADDRESS_LINE)) {

            JSONArray subAddressLine = subAddress.getJSONArray(FieldNameConstants.ADDRESS_LINE);
            JSONArray initiationAddressLine = initAddress.getJSONArray(FieldNameConstants.ADDRESS_LINE);

            if (!new HashSet<>(subAddressLine.toList()).containsAll(initiationAddressLine.toList())) {
                return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                        FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                        ErrorConstants.ADDRESS_LINE_MISMATCH));
            }
        }

        return JsonValidationUtil.firstError(
                JsonValidationUtil.presenceMatchOrError(subAddress, initAddress,
                        FieldNameConstants.COUNTRY_SUB_DIVISION,
                        ErrorConstants.COUNTRY_SUB_DIVISION_NOT_FOUND),
                JsonValidationUtil.optionalMatchOrError(subAddress, initAddress,
                        FieldNameConstants.COUNTRY_SUB_DIVISION,
                        ErrorConstants.COUNTRY_SUB_DIVISION_MISMATCH));
    }

    /**
     * Compares an account against the consent.
     *
     * @param subAccount    the submission account
     * @param initAccount   the consent account
     * @param requestId     the request ID
     * @param messages      the field messages
     * @param proxyMessages the Proxy field messages
     * @return success, or an error response
     */
    private static JSONObject validateAccount(JSONObject subAccount, JSONObject initAccount, String requestId,
                                              AccountMessages messages, AccountProxyMessages proxyMessages) {

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.mandatoryMatchOrError(subAccount, initAccount,
                        FieldNameConstants.SCHEME_NAME,
                        messages.schemeNameMismatch, messages.schemeNameNotFound),
                JsonValidationUtil.mandatoryMatchOrError(subAccount, initAccount,
                        FieldNameConstants.IDENTIFICATION,
                        messages.identificationMismatch, messages.identificationNotFound),
                JsonValidationUtil.optionalMatchOrError(subAccount, initAccount,
                        FieldNameConstants.NAME, messages.nameMismatch),
                JsonValidationUtil.optionalMatchOrError(subAccount, initAccount,
                        FieldNameConstants.SECONDARY_IDENTIFICATION, messages.secondaryIdentificationMismatch),
                JsonValidationUtil.presenceMatchOrError(subAccount, initAccount,
                        FieldNameConstants.PROXY, messages.proxyMismatch));
        if (error != null) {
            return error;
        }

        if (subAccount.has(FieldNameConstants.PROXY) && initAccount.has(FieldNameConstants.PROXY)) {

            JSONObject validationResponse = validateAccountProxy(
                    (JSONObject) subAccount.get(FieldNameConstants.PROXY),
                    (JSONObject) initAccount.get(FieldNameConstants.PROXY), requestId, proxyMessages);
            if (CommonUtil.isError(validationResponse)) {
                return validationResponse;
            }
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Compares an account Proxy against the consent.
     *
     * @param subProxy  the submission Proxy
     * @param initProxy the consent Proxy
     * @param requestId the request ID
     * @param messages  the field messages
     * @return success, or an error response
     */
    private static JSONObject validateAccountProxy(JSONObject subProxy, JSONObject initProxy, String requestId,
                                                   AccountProxyMessages messages) {

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.mandatoryMatchOrError(subProxy, initProxy,
                        FieldNameConstants.IDENTIFICATION,
                        messages.identificationMismatch, messages.identificationNotFound),
                JsonValidationUtil.mandatoryMatchOrError(subProxy, initProxy,
                        FieldNameConstants.CODE,
                        messages.codeMismatch, messages.codeNotFound),
                JsonValidationUtil.optionalMatchOrError(subProxy, initProxy,
                        FieldNameConstants.TYPE, messages.typeMismatch));
        if (error != null) {
            return error;
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * Compares a party against the consent.
     *
     * @param subParty  the submission party
     * @param initParty the consent party
     * @param requestId the request ID
     * @param messages  the field messages
     * @return success, or an error response
     */
    private static JSONObject validateParty(JSONObject subParty, JSONObject initParty, String requestId,
                                            PartyMessages messages) {

        JSONObject error = JsonValidationUtil.firstError(
                JsonValidationUtil.optionalMatchOrError(subParty, initParty,
                        FieldNameConstants.NAME, messages.nameMismatch),
                JsonValidationUtil.optionalMatchOrError(subParty, initParty,
                        FieldNameConstants.IDENTIFICATION, messages.identificationMismatch),
                JsonValidationUtil.optionalMatchOrError(subParty, initParty,
                        FieldNameConstants.LEI, messages.leiMismatch),
                JsonValidationUtil.optionalMatchOrError(subParty, initParty,
                        FieldNameConstants.SCHEME_NAME, messages.schemeNameMismatch));
        if (error != null) {
            return error;
        }

        // PostalAddress is optional, so read it with optJSONObject: get() would throw when it is absent, and
        // isValidAddress is written to handle a null on either side.
        if (!isValidAddress(subParty.optJSONObject(FieldNameConstants.POSTAL_ADDRESS),
                initParty.optJSONObject(FieldNameConstants.POSTAL_ADDRESS))) {

            return FailedResponseUtil.getFailedResponse(ErrorConstants.BAD_REQUEST,
                    FailedResponseUtil.buildErrorData(ErrorConstants.RESOURCE_CONSENT_MISMATCH,
                    messages.postalAddressMismatch));
        }

        return SuccessResponseUtil.getSuccessResponse(requestId);
    }

    /**
     * A consent-validator check comparing one nested object of the submission against its consent counterpart.
     */
    @FunctionalInterface
    private interface NestedObjectCheck {

        /**
         * Runs the check.
         *
         * @return the validator's own response: a success response on pass, an error response on failure
         */
        JSONObject validate(JSONObject submission, JSONObject consent, String requestId);
    }

    /**
     * Field messages for the account Proxy check.
     */
    private static final class AccountProxyMessages {

        private final String identificationMismatch;
        private final String identificationNotFound;
        private final String codeMismatch;
        private final String codeNotFound;
        private final String typeMismatch;

        /**
         * Creates a message set.
         *
         * @param identificationMismatch the Identification mismatch message
         * @param identificationNotFound the Identification missing message
         * @param codeMismatch           the Code mismatch message
         * @param codeNotFound           the Code missing message
         * @param typeMismatch           the Type mismatch message
         */
        private AccountProxyMessages(String identificationMismatch, String identificationNotFound,
                                     String codeMismatch, String codeNotFound, String typeMismatch) {
            this.identificationMismatch = identificationMismatch;
            this.identificationNotFound = identificationNotFound;
            this.codeMismatch = codeMismatch;
            this.codeNotFound = codeNotFound;
            this.typeMismatch = typeMismatch;
        }
    }

    /**
     * Field messages for the account check.
     */
    private static final class AccountMessages {

        private final String schemeNameMismatch;
        private final String schemeNameNotFound;
        private final String identificationMismatch;
        private final String identificationNotFound;
        private final String nameMismatch;
        private final String secondaryIdentificationMismatch;
        private final String proxyMismatch;

        /**
         * Creates a message set.
         *
         * @param schemeNameMismatch              the SchemeName mismatch message
         * @param schemeNameNotFound              the SchemeName missing message
         * @param identificationMismatch          the Identification mismatch message
         * @param identificationNotFound          the Identification missing message
         * @param nameMismatch                    the Name mismatch message
         * @param secondaryIdentificationMismatch the SecondaryIdentification mismatch message
         * @param proxyMismatch                   the Proxy mismatch message
         */
        private AccountMessages(String schemeNameMismatch, String schemeNameNotFound,
                                String identificationMismatch, String identificationNotFound,
                                String nameMismatch, String secondaryIdentificationMismatch,
                                String proxyMismatch) {
            this.schemeNameMismatch = schemeNameMismatch;
            this.schemeNameNotFound = schemeNameNotFound;
            this.identificationMismatch = identificationMismatch;
            this.identificationNotFound = identificationNotFound;
            this.nameMismatch = nameMismatch;
            this.secondaryIdentificationMismatch = secondaryIdentificationMismatch;
            this.proxyMismatch = proxyMismatch;
        }
    }

    /**
     * Field messages for the party check.
     */
    private static final class PartyMessages {

        private final String nameMismatch;
        private final String identificationMismatch;
        private final String leiMismatch;
        private final String schemeNameMismatch;
        private final String postalAddressMismatch;

        /**
         * Creates a message set.
         *
         * @param nameMismatch           the Name mismatch message
         * @param identificationMismatch the Identification mismatch message
         * @param leiMismatch            the LEI mismatch message
         * @param schemeNameMismatch     the SchemeName mismatch message
         * @param postalAddressMismatch  the PostalAddress mismatch message
         */
        private PartyMessages(String nameMismatch, String identificationMismatch, String leiMismatch,
                              String schemeNameMismatch, String postalAddressMismatch) {
            this.nameMismatch = nameMismatch;
            this.identificationMismatch = identificationMismatch;
            this.leiMismatch = leiMismatch;
            this.schemeNameMismatch = schemeNameMismatch;
            this.postalAddressMismatch = postalAddressMismatch;
        }
    }

    private static final AccountProxyMessages CREDITOR_ACCOUNT_PROXY_MESSAGES = new AccountProxyMessages(
            ErrorConstants.CREDITOR_ACC_PROXY_IDENTIFICATION_MISMATCH,
            ErrorConstants.CREDITOR_ACC_PROXY_IDENTIFICATION_NOT_FOUND,
            ErrorConstants.CREDITOR_ACC_PROXY_CODE_MISMATCH,
            ErrorConstants.CREDITOR_ACC_PROXY_CODE_NOT_FOUND,
            ErrorConstants.CREDITOR_ACC_PROXY_TYPE_MISMATCH);

    private static final AccountProxyMessages DEBTOR_ACCOUNT_PROXY_MESSAGES = new AccountProxyMessages(
            ErrorConstants.DEBTOR_ACC_PROXY_IDENTIFICATION_MISMATCH,
            ErrorConstants.DEBTOR_ACC_PROXY_IDENTIFICATION_NOT_FOUND,
            ErrorConstants.DEBTOR_ACC_PROXY_CODE_MISMATCH,
            ErrorConstants.DEBTOR_ACC_PROXY_CODE_NOT_FOUND,
            ErrorConstants.DEBTOR_ACC_PROXY_TYPE_MISMATCH);

    private static final AccountMessages CREDITOR_ACCOUNT_MESSAGES = new AccountMessages(
            ErrorConstants.CREDITOR_ACC_SCHEME_NAME_MISMATCH,
            ErrorConstants.CREDITOR_ACC_SCHEME_NAME_NOT_FOUND,
            ErrorConstants.CREDITOR_ACC_IDENTIFICATION_MISMATCH,
            ErrorConstants.CREDITOR_ACC_IDENTIFICATION_NOT_FOUND,
            ErrorConstants.CREDITOR_ACC_NAME_MISMATCH,
            ErrorConstants.CREDITOR_ACC_SEC_IDENTIFICATION_MISMATCH,
            ErrorConstants.CREDITOR_ACC_PROXY_MISMATCH);

    private static final AccountMessages DEBTOR_ACCOUNT_MESSAGES = new AccountMessages(
            ErrorConstants.DEBTOR_ACC_SCHEME_NAME_MISMATCH,
            ErrorConstants.DEBTOR_ACC_SCHEME_NAME_NOT_FOUND,
            ErrorConstants.DEBTOR_ACC_IDENTIFICATION_MISMATCH,
            ErrorConstants.DEBTOR_ACC_IDENTIFICATION_NOT_FOUND,
            ErrorConstants.DEBTOR_ACC_NAME_MISMATCH,
            ErrorConstants.DEBTOR_ACC_SEC_IDENTIFICATION_MISMATCH,
            ErrorConstants.DEBTOR_ACC_PROXY_MISMATCH);

    private static final PartyMessages ULTIMATE_CREDITOR_MESSAGES = new PartyMessages(
            ErrorConstants.ULTIMATE_CREDITOR_NAME_MISMATCH,
            ErrorConstants.ULTIMATE_CREDITOR_IDENTIFICATION_MISMATCH,
            ErrorConstants.ULTIMATE_CREDITOR_LEI_MISMATCH,
            ErrorConstants.ULTIMATE_CREDITOR_SCHEME_NAME_MISMATCH,
            ErrorConstants.ULTIMATE_CREDITOR_POSTAL_ADDRESS_MISMATCH);

    private static final PartyMessages ULTIMATE_DEBTOR_MESSAGES = new PartyMessages(
            ErrorConstants.ULTIMATE_DEBTOR_NAME_MISMATCH,
            ErrorConstants.ULTIMATE_DEBTOR_IDENTIFICATION_MISMATCH,
            ErrorConstants.ULTIMATE_DEBTOR_LEI_MISMATCH,
            ErrorConstants.ULTIMATE_DEBTOR_SCHEME_NAME_MISMATCH,
            ErrorConstants.ULTIMATE_DEBTOR_POSTAL_ADDRESS_MISMATCH);

}
