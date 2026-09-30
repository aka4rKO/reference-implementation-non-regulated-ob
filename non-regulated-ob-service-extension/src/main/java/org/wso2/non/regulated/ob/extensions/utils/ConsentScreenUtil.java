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

import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.non.regulated.ob.extensions.configurations.ConfigurableProperties;
import org.wso2.non.regulated.ob.extensions.constants.AuthorizeScreenLabelConstants;
import org.wso2.non.regulated.ob.extensions.constants.CommonConstants;
import org.wso2.non.regulated.ob.extensions.constants.FieldNameConstants;
import org.wso2.non.regulated.ob.extensions.enums.ConsentStatus;
import org.wso2.non.regulated.ob.extensions.exceptions.ConsentException;
import org.wso2.non.regulated.ob.extensions.model.Account;
import org.wso2.non.regulated.ob.extensions.model.PopulateConsentAuthorizeScreenRequestBody;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreenDataConsentData;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData;
import org.wso2.non.regulated.ob.extensions.model.SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility methods for reading, validating, and building consent data from OAuth authorization_details.
 */
public class ConsentScreenUtil {

    /**
     * Validates authorization_details business rules.
     *
     * @param authorizationDetails the authorization_details entry
     * @param authDetailsType      the RAR type
     * @throws ConsentException on a rule violation
     */
    public static void validateAuthorizationDetails(JSONObject authorizationDetails, String authDetailsType)
            throws ConsentException {

        if (CommonConstants.ACCOUNT_INFORMATION.equals(authDetailsType)) {
            validateAccountAuthorizationDetails(authorizationDetails);
        } else if (CommonUtil.isPaymentType(authDetailsType)) {
            validatePaymentAuthorizationDetails(authorizationDetails, authDetailsType);
        }
        // Any other type carries no extra business rules beyond its RAR JSON schema.
    }

    /**
     * Builds the consent data shown on the authorize screen for the given authorization_details entry.
     *
     * @param authorizationDetails the authorization_details entry (already validated)
     * @param authDetailsType      the RAR "type" discriminator
     * @return the consent data
     */
    public static SuccessResponsePopulateConsentAuthorizeScreenDataConsentData getConsentData(
            JSONObject authorizationDetails, String authDetailsType) {

        if (CommonConstants.ACCOUNT_INFORMATION.equals(authDetailsType)) {
            return getAccountsConsentData(authorizationDetails);
        }
        if (CommonUtil.isPaymentType(authDetailsType)) {
            return getPaymentsConsentData(authorizationDetails);
        }
        return new SuccessResponsePopulateConsentAuthorizeScreenDataConsentData();
    }

    /**
     * Fetches the end user's accounts from the banking backend and, for payment types, checks that any debtor
     * account named in the request belongs to the end user.
     *
     * @param requestBody          the original request, needed for the user ID
     * @param authorizationDetails the authorization_details entry (already validated)
     * @param authDetailsType      the RAR "type" discriminator
     * @return the consumer data
     * @throws ConsentException if the debtor account is not acceptable, or the accounts fetch fails
     */
    public static SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData getConsumerData(
            PopulateConsentAuthorizeScreenRequestBody requestBody, JSONObject authorizationDetails,
            String authDetailsType) throws ConsentException {

        SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData consumerData =
                new SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData();

        String accountsURL = getAccountURL(authDetailsType);
        if (accountsURL == null) {
            return consumerData;
        }

        JSONArray accountsJSON = fetchAccounts(accountsURL, requestBody.getData().getUserId());

        if (CommonUtil.isPaymentType(authDetailsType) && isDebtorAccExists(authorizationDetails)) {
            validateDebtorAccountOwnership(authorizationDetails, accountsJSON);
        } else {
            consumerData.setAccounts(setDisplayNames(accountsJSON));
        }
        return consumerData;
    }

    /**
     * Builds consent data for an account_information consent.
     *
     * @param authorizationDetails the authorization_details entry
     * @return the consent data
     */
    private static SuccessResponsePopulateConsentAuthorizeScreenDataConsentData getAccountsConsentData(
            JSONObject authorizationDetails) {

        SuccessResponsePopulateConsentAuthorizeScreenDataConsentData consentData =
                new SuccessResponsePopulateConsentAuthorizeScreenDataConsentData();

        if (authorizationDetails != null) {
            Map<String, List<String>> basicConsentData = new HashMap<>();
            putIfPresent(basicConsentData, AuthorizeScreenLabelConstants.EXPIRATION_DATE_TITLE,
                    authorizationDetails, FieldNameConstants.EXPIRATION_DATE);
            putIfPresent(basicConsentData, AuthorizeScreenLabelConstants.TRANSACTION_FROM_DATE_TITLE,
                    authorizationDetails, FieldNameConstants.TRANSACTION_FROM_DATE);
            putIfPresent(basicConsentData, AuthorizeScreenLabelConstants.TRANSACTION_TO_DATE_TITLE,
                    authorizationDetails, FieldNameConstants.TRANSACTION_TO_DATE);
            consentData.setBasicConsentData(basicConsentData);

            SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner consentDataPermissions =
                    new SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner();
            if (authorizationDetails.has(FieldNameConstants.PERMISSIONS)) {
                consentDataPermissions.setDisplayValues(
                        toStringList(authorizationDetails.getJSONArray(FieldNameConstants.PERMISSIONS)));
                consentDataPermissions.setUid("1");
            }

            consentData.setPermissions(Collections.singletonList(consentDataPermissions));
            consentData.setType(authorizationDetails.getString(CommonConstants.AUTHORIZATION_DETAILS_TYPE));
            consentData.setAllowMultipleAccounts(true);
        }

        return consentData;
    }

    /**
     * Builds consent data for a payment consent.
     *
     * @param authorizationDetails the authorization_details entry
     * @return the consent data
     */
    private static SuccessResponsePopulateConsentAuthorizeScreenDataConsentData getPaymentsConsentData(
            JSONObject authorizationDetails) {

        SuccessResponsePopulateConsentAuthorizeScreenDataConsentData consentData =
                new SuccessResponsePopulateConsentAuthorizeScreenDataConsentData();

        JSONObject initiation = authorizationDetails.optJSONObject(FieldNameConstants.INITIATION);

        if (initiation != null) {
            consentData.setBasicConsentData(populatePaymentDataForType(authorizationDetails, initiation));
            addDebtorAccountData(consentData, initiation);

            // CreditorAccount is mandatory for every payment type.
            consentData.getBasicConsentData().put(AuthorizeScreenLabelConstants.CREDITOR_ACC_TITLE,
                    populateAccountDisplayData(initiation.getJSONObject(FieldNameConstants.CREDITOR_ACC)));
        }

        consentData.setType(authorizationDetails.getString(CommonConstants.AUTHORIZATION_DETAILS_TYPE));
        consentData.setIsReauthorization(false);
        consentData.setAllowMultipleAccounts(false);

        return consentData;
    }

    /**
     * Whether a debtor account is named.
     *
     * @param authorizationDetails the authorization_details entry
     * @return true if a debtor account is present
     */
    private static boolean isDebtorAccExists(JSONObject authorizationDetails) {
        return findDebtorAccount(authorizationDetails) != null;
    }

    /**
     * Checks debtor account ownership, throwing if invalid.
     *
     * @param authorizationDetails the authorization_details entry
     * @param payableAccountsArray the user's payable accounts
     * @throws ConsentException if the debtor account is not acceptable
     */
    private static void validateDebtorAccountOwnership(JSONObject authorizationDetails, JSONArray payableAccountsArray)
            throws ConsentException {

        String debtorAccFromRequest = getDebtorAccFromAuthorizationDetails(authorizationDetails);
        if (!validateDebtorAccount(debtorAccFromRequest, payableAccountsArray)) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "Invalid debtor account in consent, consent rejected.", ConsentStatus.REJECTED.getValue());
        }
    }

    /**
     * Builds the account display-name list.
     *
     * @param sharableAccountsArray the accounts
     * @return the display entries
     */
    private static List<SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner> setDisplayNames(
            JSONArray sharableAccountsArray) {

        List<SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner> consumerDataObject =
                new ArrayList<>();
        if (sharableAccountsArray == null) {
            return consumerDataObject;
        }

        for (int i = 0; i < sharableAccountsArray.length(); i++) {
            JSONObject accountObject = sharableAccountsArray.optJSONObject(i);
            if (accountObject == null) {
                continue;
            }

            SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner account =
                    new SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner();
            account.setDisplayName(accountObject.optString(CommonConstants.DISPLAY_NAME));
            account.setSelected(false); // Default to not selected
            consumerDataObject.add(account);
        }
        return consumerDataObject;
    }

    /**
     * Maps a RAR type to its accounts endpoint.
     *
     * @param authDetailsType the RAR type
     * @return the endpoint, or null
     */
    private static String getAccountURL(String authDetailsType) {

        if (CommonConstants.ACCOUNT_INFORMATION.equals(authDetailsType)) {
            return ConfigurableProperties.SHARABLE_ENDPOINT;
        }
        if (CommonUtil.isPaymentType(authDetailsType)) {
            return ConfigurableProperties.PAYABLE_ENDPOINT;
        }
        return null;
    }

    /**
     * Fetches the user's accounts.
     *
     * @param accountsURL the backend endpoint
     * @param userId      the user's ID
     * @return the accounts array
     * @throws ConsentException on a fetch failure
     */
    private static JSONArray fetchAccounts(String accountsURL, String userId) throws ConsentException {

        Map<String, String> parameters = new HashMap<>();
        parameters.put(CommonConstants.USER_ID_KEY_NAME, userId);

        String accountData;
        try {
            accountData = HttpClientUtil.getAccountsFromEndpoint(accountsURL, parameters, new HashMap<>());
        } catch (IOException | URISyntaxException e) {
            throw new ConsentException(ConsentException.ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (accountData == null || accountData.isEmpty()) {
            throw new ConsentException(ConsentException.ErrorCode.INTERNAL_SERVER_ERROR,
                    "Exception occurred while getting accounts data");
        }
        return new JSONObject(accountData).getJSONArray(CommonConstants.DATA_SIMPLE);
    }

    /**
     * Validates account_information business rules.
     *
     * @param authorizationDetails the authorization_details entry
     * @throws ConsentException on a rule violation
     */
    private static void validateAccountAuthorizationDetails(JSONObject authorizationDetails) throws ConsentException {

        if (authorizationDetails.has(FieldNameConstants.EXPIRATION_DATE) &&
                !isDateInFuture(authorizationDetails.getString(FieldNameConstants.EXPIRATION_DATE))) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "ExpirationDateTime has already elapsed.");
        }

        if (authorizationDetails.has(FieldNameConstants.TRANSACTION_FROM_DATE) &&
                authorizationDetails.has(FieldNameConstants.TRANSACTION_TO_DATE) &&
                !isChronologicalOrder(authorizationDetails.getString(FieldNameConstants.TRANSACTION_FROM_DATE),
                        authorizationDetails.getString(FieldNameConstants.TRANSACTION_TO_DATE))) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "TransactionFromDateTime must not be after TransactionToDateTime.");
        }

        if (authorizationDetails.has(FieldNameConstants.PERMISSIONS)) {
            validatePermissions(authorizationDetails.getJSONArray(FieldNameConstants.PERMISSIONS));
        }
    }

    /**
     * Whether a date-time is in the future.
     *
     * @param dateTimeValue the date-time
     * @return true if in the future
     */
    private static boolean isDateInFuture(String dateTimeValue) {

        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(dateTimeValue);
            return dateTime.isAfter(OffsetDateTime.now(dateTime.getOffset()));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Whether two dates are in order.
     *
     * @param earlierDateTime the earlier date
     * @param laterDateTime   the later date
     * @return true if in order
     */
    private static boolean isChronologicalOrder(String earlierDateTime, String laterDateTime) {

        try {
            return !OffsetDateTime.parse(earlierDateTime).isAfter(OffsetDateTime.parse(laterDateTime));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validates the requested permissions.
     *
     * @param permissions the permissions
     * @throws ConsentException on an invalid permission
     */
    private static void validatePermissions(JSONArray permissions) throws ConsentException {

        List<String> permissionList = toStringList(permissions);
        for (String permission : permissionList) {
            if (ConfigurableProperties.RESTRICTED_PERMISSIONS.contains(permission)) {
                throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                        "Permission " + permission + " is restricted by this deployment.");
            }
        }

        if (!isPermissionCombinationValid(permissionList)) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "Requested permission combination is invalid.");
        }
    }

    /**
     * Converts a JSONArray to a string list.
     *
     * @param array the array
     * @return the list
     */
    private static List<String> toStringList(JSONArray array) {
        return IntStream.range(0, array.length())
                .mapToObj(array::getString)
                .collect(Collectors.toList());
    }

    /**
     * Whether the permission combination is valid.
     *
     * @param permissions the permissions
     * @return true if valid
     */
    private static boolean isPermissionCombinationValid(List<String> permissions) {

        if (!permissions.contains(FieldNameConstants.READ_ACCOUNTS_BASIC) &&
                !permissions.contains(FieldNameConstants.READ_ACCOUNTS_DETAIL)) {
            return true;
        }

        boolean hasCreditsOrDebits = permissions.contains(FieldNameConstants.READ_TRANSACTIONS_CREDITS) ||
                permissions.contains(FieldNameConstants.READ_TRANSACTIONS_DEBITS);
        boolean hasBasicOrDetail = permissions.contains(FieldNameConstants.READ_TRANSACTIONS_BASIC) ||
                permissions.contains(FieldNameConstants.READ_TRANSACTIONS_DETAIL);

        if ((permissions.contains(FieldNameConstants.READ_TRANSACTIONS_BASIC) ||
                permissions.contains(FieldNameConstants.READ_TRANSACTIONS_DETAIL)) && !hasCreditsOrDebits) {
            return false;
        }
        return !((permissions.contains(FieldNameConstants.READ_TRANSACTIONS_CREDITS) ||
                permissions.contains(FieldNameConstants.READ_TRANSACTIONS_DEBITS)) && !hasBasicOrDetail);
    }

    /**
     * Validates payment business rules.
     *
     * @param authorizationDetails the authorization_details entry
     * @param authDetailsType      the RAR type
     * @throws ConsentException on a rule violation
     */
    private static void validatePaymentAuthorizationDetails(JSONObject authorizationDetails, String authDetailsType)
            throws ConsentException {

        JSONObject initiation = authorizationDetails.optJSONObject(FieldNameConstants.INITIATION);
        if (initiation == null) {
            return;
        }

        if (shouldRejectForCutOff()) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "Payment submission cut-off time has elapsed for today.", ConsentStatus.REJECTED.getValue());
        }

        JSONObject instructedAmount = initiation.optJSONObject(FieldNameConstants.INSTRUCTED_AMOUNT);
        if (instructedAmount != null && instructedAmount.has(FieldNameConstants.AMOUNT) &&
                !isWithinMaxInstructedAmount(instructedAmount.getString(FieldNameConstants.AMOUNT))) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "InstructedAmount exceeds the maximum amount permitted by this deployment.");
        }

        if (CommonConstants.DOMESTIC_SCHEDULED_PAYMENT.equals(authDetailsType) &&
                initiation.has(FieldNameConstants.REQUEST_EXECUTION_DATE)) {
            validateExecutionDate(initiation.getString(FieldNameConstants.REQUEST_EXECUTION_DATE));
        }

        if (CommonConstants.DOMESTIC_STANDING_ORDER.equals(authDetailsType) &&
                initiation.has(FieldNameConstants.MANDATE_RELATED_INFORMATION)) {
            validateStandingOrderDates(initiation);
        }
    }

    /**
     * Validates a scheduled payment's execution date.
     *
     * @param executionDate the execution date
     * @throws ConsentException on a rule violation
     */
    private static void validateExecutionDate(String executionDate) throws ConsentException {

        if (!isDateInFuture(executionDate)) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "RequestedExecutionDateTime has already elapsed.");
        }
        if (!isWithinMaxFutureDate(executionDate)) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "RequestedExecutionDateTime is further in the future than this deployment permits.");
        }
    }

    /**
     * Validates a standing order's payment dates.
     *
     * @param initiation the Initiation object
     * @throws ConsentException on a rule violation
     */
    private static void validateStandingOrderDates(JSONObject initiation) throws ConsentException {

        JSONObject mandateInfo = initiation.getJSONObject(FieldNameConstants.MANDATE_RELATED_INFORMATION);
        if (!mandateInfo.has(FieldNameConstants.FIRST_PAYMENT_DATE)) {
            return;
        }

        String firstPaymentDate = mandateInfo.getString(FieldNameConstants.FIRST_PAYMENT_DATE);
        String recurringPaymentDate = mandateInfo.optString(FieldNameConstants.RECURRING_PAYMENT_DATE, null);
        String finalPaymentDate = mandateInfo.optString(FieldNameConstants.FINAL_PAYMENT_DATE, null);

        validateStandingOrderDateOrder(firstPaymentDate, recurringPaymentDate, finalPaymentDate);
        validateRecurringPaymentDiffersFromFirst(initiation, firstPaymentDate, recurringPaymentDate);
    }

    /**
     * Validates standing-order date order.
     *
     * @param firstPaymentDate     the first payment date
     * @param recurringPaymentDate the recurring payment date, or null
     * @param finalPaymentDate     the final payment date, or null
     * @throws ConsentException on a rule violation
     */
    private static void validateStandingOrderDateOrder(String firstPaymentDate, String recurringPaymentDate,
                                                       String finalPaymentDate) throws ConsentException {

        if (!isDateInFuture(firstPaymentDate)) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "FirstPaymentDateTime has already elapsed.");
        }
        if (recurringPaymentDate != null && !isChronologicalOrder(firstPaymentDate, recurringPaymentDate)) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "FirstPaymentDateTime must not be after RecurringPaymentDateTime.");
        }
        if (finalPaymentDate == null) {
            return;
        }

        // The final payment must follow whichever date immediately precedes it in the mandate.
        String precedingDate = recurringPaymentDate != null ? recurringPaymentDate : firstPaymentDate;
        if (!isChronologicalOrder(precedingDate, finalPaymentDate)) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "FinalPaymentDateTime must not be before " +
                            (recurringPaymentDate != null ? "RecurringPaymentDateTime."
                                    : "FirstPaymentDateTime."));
        }
    }

    /**
     * Validates the recurring payment differs from the first.
     *
     * @param initiation           the Initiation object
     * @param firstPaymentDate     the first payment date
     * @param recurringPaymentDate the recurring payment date, or null
     * @throws ConsentException if they match
     */
    private static void validateRecurringPaymentDiffersFromFirst(JSONObject initiation, String firstPaymentDate,
                                                                 String recurringPaymentDate)
            throws ConsentException {

        if (recurringPaymentDate == null || !firstPaymentDate.equals(recurringPaymentDate)) {
            return;
        }

        JSONObject firstPaymentAmount = initiation.optJSONObject(FieldNameConstants.FIRST_PAYMENT_AMOUNT);
        JSONObject recurringPaymentAmount = initiation.optJSONObject(FieldNameConstants.RECURRING_AMOUNT);
        if (firstPaymentAmount == null || recurringPaymentAmount == null) {
            return;
        }

        if (firstPaymentAmount.optString(FieldNameConstants.AMOUNT, "")
                .equals(recurringPaymentAmount.optString(FieldNameConstants.AMOUNT, ""))) {
            throw new ConsentException(ConsentException.ErrorCode.BAD_REQUEST,
                    "RecurringPaymentDateTime/Amount must differ from FirstPaymentDateTime/Amount.");
        }
    }

    /**
     * Whether the cut-off policy requires rejection.
     *
     * @return true if it does
     */
    private static boolean shouldRejectForCutOff() {

        return Boolean.parseBoolean(ConfigurableProperties.CUTOFF_DATE_ENABLED) &&
                "REJECT".equals(ConfigurableProperties.CUTOFF_DATE_POLICY) &&
                hasCutOffTimeElapsed();
    }

    /**
     * Whether the daily cut-off time has passed.
     *
     * @return true if it has passed
     */
    private static boolean hasCutOffTimeElapsed() {

        OffsetTime dailyCutOffTime = OffsetTime.parse(ConfigurableProperties.DAILY_CUTOFF);
        OffsetTime currentTime = OffsetDateTime.now(dailyCutOffTime.getOffset()).toOffsetTime();
        return currentTime.isAfter(dailyCutOffTime);
    }

    /**
     * Whether a date is within the allowed future range.
     *
     * @param dateTimeValue the date-time
     * @return true if within range
     */
    private static boolean isWithinMaxFutureDate(String dateTimeValue) {

        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(dateTimeValue);
            OffsetDateTime maxFutureDate = OffsetDateTime.now(dateTime.getOffset())
                    .plusDays(Long.parseLong(ConfigurableProperties.MAX_FUTURE_PAYMENT_DAYS));
            return !dateTime.isAfter(maxFutureDate);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Whether an amount is within the allowed maximum.
     *
     * @param instructedAmount the amount
     * @return true if within range
     * @throws NumberFormatException on an invalid number
     */
    private static boolean isWithinMaxInstructedAmount(String instructedAmount) {

        return Double.parseDouble(instructedAmount) <= Double.parseDouble(ConfigurableProperties.MAX_INSTRUCTED_AMOUNT);
    }

    /**
     * Builds consent data for the payment's RAR type.
     *
     * @param authorizationDetails the authorization_details entry
     * @param initiation           the Initiation object
     * @return the display data
     */
    private static Map<String, List<String>> populatePaymentDataForType(JSONObject authorizationDetails,
                                                                          JSONObject initiation) {

        String authDetailsType = authorizationDetails.getString(CommonConstants.AUTHORIZATION_DETAILS_TYPE);

        if (CommonConstants.DOMESTIC_PAYMENT.equals(authDetailsType) ||
                CommonConstants.INTERNATIONAL_PAYMENT.equals(authDetailsType)) {
            return populateSinglePaymentData(authorizationDetails, initiation);
        }
        if (CommonConstants.DOMESTIC_SCHEDULED_PAYMENT.equals(authDetailsType)) {
            return populateScheduledPaymentData(authorizationDetails, initiation);
        }
        if (CommonConstants.DOMESTIC_STANDING_ORDER.equals(authDetailsType)) {
            return populateStandingOrderPaymentData(authorizationDetails, initiation);
        }

        return new HashMap<>();
    }

    /**
     * Adds the debtor account's display data.
     *
     * @param consentData the consent data being built
     * @param initiation  the Initiation object
     */
    private static void addDebtorAccountData(
            SuccessResponsePopulateConsentAuthorizeScreenDataConsentData consentData, JSONObject initiation) {

        if (!initiation.has(FieldNameConstants.DEBTOR_ACC)) {
            return;
        }

        JSONObject debtorAcc = initiation.getJSONObject(FieldNameConstants.DEBTOR_ACC);
        consentData.getBasicConsentData().put(AuthorizeScreenLabelConstants.DEBTOR_ACC_TITLE,
                populateAccountDisplayData(debtorAcc));

        Account account = new Account();
        account.setDisplayName(debtorAcc.getString(FieldNameConstants.IDENTIFICATION));
        consentData.setInitiatedAccountsForConsent(Collections.singletonList(account));
    }

    /**
     * Formats an account into display lines.
     *
     * @param account the account object
     * @return the display lines
     */
    private static List<String> populateAccountDisplayData(JSONObject account) {

        List<String> accountData = new ArrayList<>();
        addIfNotEmpty(accountData, AuthorizeScreenLabelConstants.SCHEME_NAME_TITLE,
                account.optString(FieldNameConstants.SCHEME_NAME, ""));
        addIfNotEmpty(accountData, AuthorizeScreenLabelConstants.IDENTIFICATION_TITLE,
                account.optString(FieldNameConstants.IDENTIFICATION, ""));
        addIfNotEmpty(accountData, AuthorizeScreenLabelConstants.NAME_TITLE,
                account.optString(FieldNameConstants.NAME, ""));
        addIfNotEmpty(accountData, AuthorizeScreenLabelConstants.SECONDARY_IDENTIFICATION_TITLE,
                account.optString(FieldNameConstants.SECONDARY_IDENTIFICATION, ""));
        return accountData;
    }

    /**
     * Appends a display line if the value is non-empty.
     *
     * @param target the display lines
     * @param title  the label
     * @param value  the value
     */
    private static void addIfNotEmpty(List<String> target, String title, String value) {
        if (!value.isEmpty()) {
            target.add(title + " : " + value);
        }
    }

    /**
     * Adds a field to the map if present.
     *
     * @param basicConsentData the map
     * @param title            the label
     * @param json             the source object
     * @param key              the field name
     */
    private static void putIfPresent(Map<String, List<String>> basicConsentData, String title, JSONObject json,
                                      String key) {
        if (json.has(key)) {
            basicConsentData.put(title, Collections.singletonList(json.getString(key)));
        }
    }

    /**
     * Adds an amount's value and currency if present.
     *
     * @param basicConsentData the map
     * @param title            the label
     * @param parent           the source object
     * @param amountKey        the amount field name
     */
    private static void putAmountIfPresent(Map<String, List<String>> basicConsentData, String title,
                                            JSONObject parent, String amountKey) {
        JSONObject amount = parent.optJSONObject(amountKey);
        if (amount == null) {
            return;
        }
        List<String> amountList = new ArrayList<>();
        if (amount.has(FieldNameConstants.AMOUNT)) {
            amountList.add(AuthorizeScreenLabelConstants.AMOUNT_TITLE + " : " +
                    amount.optString(FieldNameConstants.AMOUNT));
        }
        if (amount.has(FieldNameConstants.CURRENCY)) {
            amountList.add(AuthorizeScreenLabelConstants.CURRENCY_TITLE + " : " +
                    amount.optString(FieldNameConstants.CURRENCY));
        }
        basicConsentData.put(title, amountList);
    }

    /**
     * Sets the payment-type label and currency.
     *
     * @param basicConsentData    the map
     * @param initiation          the Initiation object
     * @param domesticLabel       the domestic label
     * @param internationalLabel  the international label
     */
    private static void putPaymentTypeAndCurrency(Map<String, List<String>> basicConsentData,
                                                    JSONObject initiation, String domesticLabel,
                                                    String internationalLabel) {
        boolean isInternational = initiation.has(FieldNameConstants.CURRENCY_OF_TRANSFER);
        basicConsentData.put(AuthorizeScreenLabelConstants.PAYMENT_TYPE_TITLE,
                Collections.singletonList(isInternational ? internationalLabel : domesticLabel));
        if (isInternational) {
            basicConsentData.put(AuthorizeScreenLabelConstants.CURRENCY_OF_TRANSFER_TITLE,
                    Collections.singletonList(initiation.getString(FieldNameConstants.CURRENCY_OF_TRANSFER)));
        }
    }

    /**
     * Sets the Permission and ReadRefundAccount labels.
     *
     * @param basicConsentData     the map
     * @param authorizationDetails the authorization_details entry
     */
    private static void putPermissionAndReadRefund(Map<String, List<String>> basicConsentData,
                                                     JSONObject authorizationDetails) {
        basicConsentData.put(AuthorizeScreenLabelConstants.PERMISSION_TITLE,
                Collections.singletonList(authorizationDetails.getString(FieldNameConstants.PERMISSION)));
        putIfPresent(basicConsentData, AuthorizeScreenLabelConstants.READ_REFUND_TITLE, authorizationDetails,
                FieldNameConstants.READ_REFUND);
    }

    /**
     * Builds display data for a single payment.
     *
     * @param authorizationDetails the authorization_details entry
     * @param initiation           the Initiation object
     * @return the display data
     */
    private static Map<String, List<String>> populateSinglePaymentData(JSONObject authorizationDetails,
                                                                         JSONObject initiation) {

        Map<String, List<String>> basicConsentData = new HashMap<>();

        putPaymentTypeAndCurrency(basicConsentData, initiation, AuthorizeScreenLabelConstants.DOMESTIC_PAYMENTS,
                AuthorizeScreenLabelConstants.INTERNATIONAL_PAYMENTS);
        putIfPresent(basicConsentData, AuthorizeScreenLabelConstants.READ_REFUND_TITLE, authorizationDetails,
                FieldNameConstants.READ_REFUND);

        // InstructionIdentification and EndToEndIdentification are mandatory for this payment type.
        basicConsentData.put(AuthorizeScreenLabelConstants.INSTRUCTION_IDENTIFICATION_TITLE,
                Collections.singletonList(initiation.getString(FieldNameConstants.INSTRUCTION_IDENTIFICATION)));
        basicConsentData.put(AuthorizeScreenLabelConstants.END_TO_END_IDENTIFICATION_TITLE,
                Collections.singletonList(initiation.getString(FieldNameConstants.END_TO_END_IDENTIFICATION)));

        putAmountIfPresent(basicConsentData, AuthorizeScreenLabelConstants.INSTRUCTED_AMOUNT_TITLE, initiation,
                FieldNameConstants.INSTRUCTED_AMOUNT);
        return basicConsentData;
    }

    /**
     * Builds display data for a scheduled payment.
     *
     * @param authorizationDetails the authorization_details entry
     * @param initiation           the Initiation object
     * @return the display data
     */
    private static Map<String, List<String>> populateScheduledPaymentData(JSONObject authorizationDetails,
                                                                            JSONObject initiation) {

        Map<String, List<String>> basicConsentData = new HashMap<>();

        basicConsentData.put(AuthorizeScreenLabelConstants.PAYMENT_TYPE_TITLE,
                Collections.singletonList(AuthorizeScreenLabelConstants.DOMESTIC_SCHEDULED_PAYMENTS));

        // Permission is mandatory for this payment type.
        putPermissionAndReadRefund(basicConsentData, authorizationDetails);

        // InstructionIdentification is mandatory; EndToEndIdentification is optional for this payment type.
        basicConsentData.put(AuthorizeScreenLabelConstants.INSTRUCTION_IDENTIFICATION_TITLE,
                Collections.singletonList(initiation.getString(FieldNameConstants.INSTRUCTION_IDENTIFICATION)));
        putIfPresent(basicConsentData, AuthorizeScreenLabelConstants.END_TO_END_IDENTIFICATION_TITLE, initiation,
                FieldNameConstants.END_TO_END_IDENTIFICATION);

        basicConsentData.put(AuthorizeScreenLabelConstants.REQUESTED_EXECUTION_DATE_TIME_TITLE,
                Collections.singletonList(initiation.getString(FieldNameConstants.REQUEST_EXECUTION_DATE)));
        putAmountIfPresent(basicConsentData, AuthorizeScreenLabelConstants.INSTRUCTED_AMOUNT_TITLE, initiation,
                FieldNameConstants.INSTRUCTED_AMOUNT);

        return basicConsentData;
    }

    /**
     * Builds display data for a standing order.
     *
     * @param authorizationDetails the authorization_details entry
     * @param initiation           the Initiation object
     * @return the display data
     */
    private static Map<String, List<String>> populateStandingOrderPaymentData(JSONObject authorizationDetails,
                                                                                JSONObject initiation) {

        Map<String, List<String>> basicConsentData = new HashMap<>();

        basicConsentData.put(AuthorizeScreenLabelConstants.PAYMENT_TYPE_TITLE,
                Collections.singletonList(AuthorizeScreenLabelConstants.DOMESTIC_STANDING_ORDER_PAYMENTS));

        putAmountIfPresent(basicConsentData, AuthorizeScreenLabelConstants.FIRST_PAYMENT_AMOUNT_TITLE, initiation,
                FieldNameConstants.FIRST_PAYMENT_AMOUNT);
        if (initiation.has(FieldNameConstants.RECURRING_AMOUNT)) {
            putAmountIfPresent(basicConsentData, AuthorizeScreenLabelConstants.RECURRING_PAYMENT_AMOUNT_TITLE,
                    initiation, FieldNameConstants.RECURRING_AMOUNT);
        }
        if (initiation.has(FieldNameConstants.FINAL_PAYMENT_AMOUNT)) {
            putAmountIfPresent(basicConsentData, AuthorizeScreenLabelConstants.FINAL_PAYMENT_AMOUNT_TITLE, initiation,
                    FieldNameConstants.FINAL_PAYMENT_AMOUNT);
        }

        // Permission is mandatory for this payment type.
        putPermissionAndReadRefund(basicConsentData, authorizationDetails);

        return basicConsentData;
    }

    /**
     * Locates the DebtorAccount object.
     *
     * @param authorizationDetails the authorization_details entry
     * @return the DebtorAccount, or null
     */
    private static JSONObject findDebtorAccount(JSONObject authorizationDetails) {

        if (authorizationDetails.has(FieldNameConstants.DEBTOR_ACC)) {
            return authorizationDetails.getJSONObject(FieldNameConstants.DEBTOR_ACC);
        }
        if (authorizationDetails.has(FieldNameConstants.INITIATION)) {
            JSONObject initiation = authorizationDetails.getJSONObject(FieldNameConstants.INITIATION);
            if (initiation.has(FieldNameConstants.DEBTOR_ACC)) {
                return initiation.getJSONObject(FieldNameConstants.DEBTOR_ACC);
            }
        }
        return null;
    }

    /**
     * Reads the debtor account's identification.
     *
     * @param authorizationDetails the authorization_details entry
     * @return the identification, or null
     */
    private static String getDebtorAccFromAuthorizationDetails(JSONObject authorizationDetails) {

        JSONObject debtorAccount = findDebtorAccount(authorizationDetails);
        return debtorAccount == null ? null : debtorAccount.optString(FieldNameConstants.IDENTIFICATION, null);
    }

    /**
     * Checks that the debtor account belongs to the user.
     *
     * @param debtorAccount        the debtor account identification
     * @param payableAccountsArray the user's payable accounts
     * @return true if acceptable
     */
    private static boolean validateDebtorAccount(String debtorAccount, JSONArray payableAccountsArray) {

        if (debtorAccount == null || payableAccountsArray == null) {
            return false;
        }

        boolean skipOwnershipCheck = !Boolean.parseBoolean(ConfigurableProperties.VALIDATE_DEBTOR_ACC);
        if (skipOwnershipCheck && !payableAccountsArray.isEmpty()) {
            return true;
        }

        for (int i = 0; i < payableAccountsArray.length(); i++) {
            JSONObject accountObj = payableAccountsArray.optJSONObject(i);
            if (accountObj != null &&
                    accountObj.optString(CommonConstants.ACCOUNT_ID, "").equals(debtorAccount)) {
                return true;
            }
        }
        return false;
    }
}
