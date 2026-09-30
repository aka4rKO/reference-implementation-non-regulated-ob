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

package org.wso2.non.regulated.ob.extensions.constants;

/**
 * Error-message constants.
 */
public class ErrorConstants {

    // Internal JSON-path fragments used only to build the messages below.
    private static final String PATH_QUERY_PARAM = "Url.QueryParameters";
    private static final String PATH_MANDATE_RELATED_INFORMATION = "Data.Initiation.MandateRelatedInformation";
    private static final String PATH_FINAL_PAYMENT_DATE_TIME_API =
            "Data.Initiation.MandateRelatedInformation.FinalPaymentDateTime";
    private static final String PATH_RECURRING_PAYMENT_DATE_TIME_API =
            "Data.Initiation.MandateRelatedInformation.RecurringPaymentDateTime";
    private static final String PATH_DEBTOR_ACCOUNT = "Data.Initiation.DebtorAccount";
    private static final String PATH_CREDIT_AGENT = "Data.Initiation.CreditorAgent";
    private static final String PATH_FIRST_PAYMENT_AMOUNT = "Data.Initiation.FirstPaymentAmount";
    private static final String PATH_FIRST_PAYMENT_AMOUNT_AMOUNT = "Data.Initiation.FirstPaymentAmount.Amount";
    private static final String PATH_RECURRING_PAYMENT_AMOUNT = "Data.Initiation.RecurringPaymentAmount";
    private static final String PATH_RECURRING_PAYMENT_AMOUNT_AMOUNT =
            "Data.Initiation.RecurringPaymentAmount.Amount";
    private static final String PATH_EXCHANGE_RATE_INFO = "Data.Initiation.ExchangeRateInformation";
    private static final String PATH_LOCAL_INSTRUMENT = "Data.Initiation.LocalInstrument";
    private static final String PATH_REMITTANCE_INFO = "Data.Initiation.RemittanceInformation";
    private static final String PATH_CREDIT_ACCOUNT = "Data.Initiation.CreditorAccount";
    private static final String PATH_CREDIT_AGENT_NAME = "Data.Initiation.CreditorAgent.SchemeName";
    private static final String PATH_CREDIT_AGENT_IDENTIFICATION = "Data.Initiation.CreditorAgent.Identification";
    private static final String PATH_REFERENCE = "Data.Initiation.Reference";
    private static final String PATH_INSTRUCTION_PRIORITY = "Data.Initiation.InstructionPriority";
    private static final String PATH_EXTENDED_PURPOSE = "Data.Initiation.ExtendedPurpose";
    private static final String PATH_PURPOSE = "Data.Initiation.Purpose";
    private static final String PATH_FIRST_PAYMENT_CURRENCY = "Data.Initiation.FirstPaymentAmount.Currency";
    private static final String PATH_FINAL_PAYMENT_AMOUNT = "Data.Initiation.FinalPaymentAmount";
    private static final String PATH_FINAL_PAYMENT_CURRENCY = "Data.Initiation.FinalPaymentAmount.Currency";
    private static final String PATH_RECURRING_PAYMENT_CURRENCY = "Data.Initiation.RecurringPaymentAmount.Currency";
    private static final String PATH_EXCHANGE_RATE_UNIT_CURRENCY =
            "Data.Initiation.ExchangeRateInformation.UnitCurrency";
    private static final String PATH_EXCHANGE_RATE = "Data.Initiation.ExchangeRateInformation.ExchangeRate";
    private static final String PATH_EXCHANGE_RATE_TYPE = "Data.Initiation.ExchangeRateInformation.RateType";
    private static final String PATH_EXCHANGE_RATE_IDENTIFICATION =
            "Data.Initiation.ExchangeRateInformation.ContractIdentification";
    private static final String PATH_CREDITOR = "Data.Initiation.Creditor";
    private static final String PATH_CREDITOR_NAME = "Data.Initiation.Creditor.Name";
    private static final String PATH_CREDITOR_ADRESS = "Data.Initiation.Creditor.DeliveryAddress";
    private static final String PATH_ULTIMATE_CREDITOR = "Data.Initiation.UltimateCreditor";
    private static final String PATH_ULTIMATE_DEBTOR = "Data.Initiation.UltimateDebtor";
    private static final String PATH_REGULATORY_REPORTING = "Data.Initiation.RegulatoryReporting";
    private static final String PATH_INSTRUCTED_AMOUNT = "Data.Initiation.InstructedAmount";
    private static final String PATH_EXECUTION_DATE = "Data.Initiation.RequestedExecutionDateTime";
    private static final String PATH_FIRST_PAYMENT_DATE_TIME = "Data.Initiation.FirstPaymentDateTime";
    private static final String PATH_RISK_ADDRESS = "Risk.DeliveryAddress";
    private static final String PATH_DEBTOR_ACCOUNT_SCHEME = "Data.Initiation.DebtorAccount.SchemeName";
    private static final String PATH_DEBTOR_ACCOUNT_SECOND_IDENTIFICATION =
            "Data.Initiation.DebtorAccount.SecondaryIdentification";
    private static final String PATH_DEBTOR_ACCOUNT_IDENTIFICATION = "Data.Initiation.DebtorAccount.Identification";
    private static final String PATH_DEBTOR_ACCOUNT_NAME = "Data.Initiation.DebtorAccount.Name";
    private static final String PATH_REMITTANCE_INFO_REFERENCE = "Data.Initiation.RemittanceInformation.Reference";
    private static final String PATH_REMITTANCE_INFO_UNSTRUCTURED =
            "Data.Initiation.RemittanceInformation.Unstructured";
    private static final String PATH_INSTRUCTION_IDENTIFICATION = "Data.Initiation.InstructionIdentification";
    private static final String PATH_ENDTOEND_IDENTIFICATION = "Data.Initiation.EndToEndIdentification";
    private static final String PATH_INSTRUCTED_AMOUNT_AMOUNT = "Data.Initiation.InstructedAmount.Amount";
    private static final String PATH_INSTRUCTED_AMOUNT_CURRENCY = "Data.Initiation.InstructedAmount.Currency";
    private static final String PATH_FREQUENCY_API = "Data.Initiation.MandateRelatedInformation.Frequency";
    private static final String PATH_FREQUENCY = "Data.Initiation.Frequency";
    private static final String PATH_FIRST_PAYMENT_DATE_TIME_API =
            "Data.Initiation.MandateRelatedInformation.FirstPaymentDateTime";
    private static final String PATH_CURRENCY_OF_TRANSFER = "Data.Initiation.CurrencyOfTransfer";
    private static final String PATH_CHARGE_BEARER = "Data.Initiation.ChargeBearer";
    private static final String PATH_RISK_PAYMENT_CONTEXT = "Risk.PaymentContextCode";
    private static final String PATH_RISK_MERCHANT_CATEGORY = "Risk.MerchantCategoryCode";
    private static final String PATH_RISK_MERCHANT_CUSTOMER = "Risk.MerchantCustomerIdentification";
    private static final String PATH_RISK_ADDRESS_STREET_NAME = "Risk.DeliveryAddress.StreetName";
    private static final String PATH_RISK_ADDRESS_BUILDING_NO = "Risk.DeliveryAddress.BuildingNumber";
    private static final String PATH_RISK_ADDRESS_POST_CODE = "Risk.DeliveryAddress.PostCode";
    private static final String PATH_RISK_ADDRESS_TOWN_NAME = "Risk.DeliveryAddress.TownName";
    private static final String PATH_RISK_ADDRESS_COUNTRY_SUB_DIVISION = "Risk.DeliveryAddress.CountrySubDivision";
    private static final String PATH_RISK_ADDRESS_LINE = "Risk.DeliveryAddress.AddressLine";
    private static final String PATH_RISK_COUNTRY = "Risk.DeliveryAddress.Country";
    private static final String PATH_MANDATE_IDENTIFICATION =
            "Data.Initiation.MandateRelatedInformation.MandateIdentification";
    private static final String PATH_CATEGORY_PURPOSE_CODE =
            "Data.Initiation.MandateRelatedInformation.CategoryPurposeCode";
    private static final String PATH_CLASSIFICATION = "Data.Initiation.MandateRelatedInformation.Classification";
    private static final String PATH_REASON = "Data.Initiation.MandateRelatedInformation.Reason";
    private static final String PATH_CREDIT_ACCOUNT_SCHEME = "Data.Initiation.CreditorAccount.SchemeName";
    private static final String PATH_DESTINATION_COUNTRY_CODE = "Data.Initiation.DestinationCountryCode";
    private static final String PATH_DEBTOR_ACCOUNT_PROXY_IDENTIFICATION = "Data.Initiation.DebtorAccount.Proxy" +
            ".Identification";
    private static final String PATH_DEBTOR_ACCOUNT_PROXY_CODE = "Data.Initiation.DebtorAccount.Proxy.Code";
    private static final String PATH_DEBTOR_ACCOUNT_PROXY_TYPE = "Data.Initiation.DebtorAccount.Proxy.Type";
    private static final String PATH_DEBTOR_ACCOUNT_PROXY = "Data.Initiation.DebtorAccount.Proxy";
    private static final String PATH_CREDIT_ACCOUNT_NAME = "Data.Initiation.CreditorAccount.Name";
    private static final String PATH_CREDIT_ACCOUNT_PROXY_IDENTIFICATION = "Data.Initiation.CreditorAccount.Proxy" +
            ".Identification";
    private static final String PATH_CREDIT_ACCOUNT_PROXY_CODE = "Data.Initiation.CreditorAccount.Proxy.Code";
    private static final String PATH_CREDIT_ACCOUNT_PROXY_TYPE = "Data.Initiation.CreditorAccount.Proxy.Type";
    private static final String PATH_CREDIT_ACCOUNT_IDENTIFICATION = "Data.Initiation.CreditorAccount.Identification";
    private static final String PATH_CREDIT_ACCOUNT_SEC_IDENTIFICATION = "Data.Initiation.CreditorAccount" +
            ".SecondaryIdentification";
    private static final String PATH_CREDIT_ACCOUNT_PROXY = "Data.Initiation.CreditorAccount.Proxy";
    private static final String PATH_ULTIMATE_CREDITOR_SCHEME_NAME = "Data.Initiation.UltimateCreditor.SchemeName";
    private static final String PATH_ULTIMATE_CREDITOR_IDENTIFICATION =
            "Data.Initiation.UltimateCreditor.Identification";
    private static final String PATH_ULTIMATE_CREDITOR_NAME = "Data.Initiation.UltimateCreditor.Name";
    private static final String PATH_ULTIMATE_CREDITOR_LEI = "Data.Initiation.UltimateCreditor.LEI";
    private static final String PATH_ULTIMATE_CREDITOR_POSTAL_ADDRESS =
            "Data.Initiation.UltimateCreditor.PostalAddress";
    private static final String PATH_ULTIMATE_DEBTOR_SCHEME_NAME = "Data.Initiation.UltimateDebtor.SchemeName";
    private static final String PATH_ULTIMATE_DEBTOR_IDENTIFICATION =
            "Data.Initiation.UltimateDebtor.Identification";
    private static final String PATH_ULTIMATE_DEBTOR_NAME = "Data.Initiation.UltimateDebtor.Name";
    private static final String PATH_ULTIMATE_DEBTOR_LEI = "Data.Initiation.UltimateDebtor.LEI";
    private static final String PATH_ULTIMATE_DEBTOR_POSTAL_ADDRESS = "Data.Initiation.UltimateDebtor.PostalAddress";
    private static final String PATH_URL = "Data.Url";
    private static final String PATH_PERMISSIONS = "Data.Permissions";
    private static final String PATH_EXPIRATION_DATE = "Data.ExpirationDate";
    private static final String PATH_STATUS = "Payload.Status";
    private static final String PATH_ACCOUNT_ID = "Payload.AccountId";
    private static final String PATH_CONSENT_ID = "Data.Initiation.ConsentId";
    private static final String PATH_DATA = "Data";
    private static final String PATH_INITIATION = "Data.Initiation";
    private static final String PATH_RISK = "Risk";
    private static final String PATH_REVOKED_CONSENT_ID = "ConsentId";

    // Error messages.
    public static final String INVALID_QUERY_PARAMS = "Transaction time validation failed. Invalid query parameters " +
            "found in the request. :" + PATH_QUERY_PARAM;
    public static final String MANDATE_RELATED_INFORMATION_MISMATCH = "Mandate Related Information does not match.:" +
            PATH_MANDATE_RELATED_INFORMATION;
    public static final String FINAL_PAYMENT_DATE_MISMATCH_API = "Final Payment Date Time does not match:" +
            PATH_FINAL_PAYMENT_DATE_TIME_API;
    public static final String RECURRING_PAYMENT_DATE_MISMATCH_API = "Recurring Payment Date Time does not match:" +
            PATH_RECURRING_PAYMENT_DATE_TIME_API;
    public static final String MISSING_INITIATION_OBJECT = "Missing initiation object";
    public static final String LOCAL_INSTRUMENT_MISMATCH = "Local Instrument does not match:" +
            PATH_LOCAL_INSTRUMENT;
    public static final String LOCAL_INSTRUMENT_NOT_FOUND = "Local Instrument isn't present in the " +
            "request or in the consent:" + PATH_LOCAL_INSTRUMENT;
    public static final String REFERENCE_NOT_FOUND = "Reference isn't present in the " +
            "request or in the consent:" + PATH_LOCAL_INSTRUMENT;
    public static final String DEBTOR_ACC_MISMATCH = "Debtor Account isn't present in the request " +
            "or in the consent:" + PATH_DEBTOR_ACCOUNT;
    public static final String CREDITOR_ACC_NOT_FOUND = "Creditor Account isn't present in the request.:" +
            PATH_CREDIT_ACCOUNT;
    public static final String FIRST_PAYMENT_AMOUNT_MISMATCH = "First Payment Amount does not match:" +
            PATH_FIRST_PAYMENT_AMOUNT_AMOUNT;
    public static final String CREDITOR_AGENT_MISMATCH = "Creditor Agent isn't present in the request or in the " +
            "consent:" + PATH_CREDIT_AGENT;
    public static final String CREDITOR_AGENT_SCHEME_NAME_MISMATCH = "Creditor Agent Scheme name does not match:" +
            PATH_CREDIT_AGENT_NAME;
    public static final String CREDITOR_AGENT_IDENTIFICATION_MISMATCH = "Creditor Agent Identification does not match:"
            + PATH_CREDIT_AGENT_IDENTIFICATION;
    public static final String REMMITANCE_INFO_MISMATCH = "Remittance Information isn't present in the " +
            "request or in the consent:" + PATH_REMITTANCE_INFO;
    public static final String REFERENCE_MISMATCH = "Reference does not match.:" + PATH_REFERENCE;
    public static final String INSTRUCTION_PRIORITY_MISSING = "Instruction Priority isn't present in the request or" +
            " in the consent:" + PATH_INSTRUCTION_PRIORITY;
    public static final String INSTRUCTION_PRIORITY_MISMATCH = "Instruction Priority does not match:" +
            PATH_INSTRUCTION_PRIORITY;
    public static final String PURPOSE_MISSING = "Purpose isn't present in the request or" +
            " in the consent:" + PATH_PURPOSE;
    public static final String PURPOSE_MISMATCH = "Purpose does not match:" + PATH_PURPOSE;
    public static final String EXTENDED_PURPOSE_MISSING = "Extended Purpose isn't present in the request or" +
            " in the consent:" + PATH_EXTENDED_PURPOSE;
    public static final String EXTENDED_PURPOSE_MISMATCH = "Extended Purpose does not match:" +
            PATH_EXTENDED_PURPOSE;
    public static final String FIRST_PAYMENT_AMOUNT_NOT_FOUND = "First Payment Amount isn't present in the " +
            "request or in the consent:" + PATH_FIRST_PAYMENT_AMOUNT;
    public static final String FIRST_PAYMENT_CURRENCY_MISMATCH = "First Payment Currency does not match:" +
            PATH_FIRST_PAYMENT_CURRENCY;
    public static final String FINAL_PAYMENT_AMOUNT_NOT_FOUND = "Final Payment Amount isn't present in the" +
            " request or in the consent:" + PATH_FINAL_PAYMENT_AMOUNT;
    public static final String FINAL_PAYMENT_AMOUNT_MISMATCH = "Final Payment Amount does not match:" +
            PATH_FINAL_PAYMENT_AMOUNT;
    public static final String FINAL_PAYMENT_CURRENCY_MISMATCH = "Final Payment Currency does not match:" +
            PATH_FINAL_PAYMENT_CURRENCY;
    public static final String RECURRING_PAYMENT_AMOUNT_NOT_FOUND = "Recurring Payment Amount isn't present in the" +
            " request or in the consent:" + PATH_RECURRING_PAYMENT_AMOUNT;
    public static final String RECURRING_PAYMENT_AMOUNT_MISMATCH = "Recurring Payment Amount does not match:" +
            PATH_RECURRING_PAYMENT_AMOUNT_AMOUNT;
    public static final String RECURRING_PAYMENT_CURRENCY_MISMATCH = "Recurring Payment Currency does not match:" +
            PATH_RECURRING_PAYMENT_CURRENCY;
    public static final String EXCHANGE_RATE_INFO_MISMATCH = "Exchange Rate Information isn't present in " +
            "the request or in the consent:" + PATH_EXCHANGE_RATE_INFO;
    public static final String EXCHNAGE_RATE_UNIT_CURRENCY_MISMATCH = "Exchange Rate Unit Currency does not match:" +
            PATH_EXCHANGE_RATE_UNIT_CURRENCY;
    public static final String EXCHANGE_RATE_UNIT_CURRENCY_NOT_FOUND = "Exchange Rate Unit Currency isn't present in " +
            "the request or in the consent:" + PATH_EXCHANGE_RATE_UNIT_CURRENCY;
    public static final String  EXCHANGE_RATE_NOT_FOUND = "Exchange Rate isn't present in the request or in the" +
            " consent:" + PATH_EXCHANGE_RATE;
    public static final String  EXCHANGE_RATE_TYPE_NOT_FOUND = "Exchange Rate Type isn't present in the request or" +
            " in the consent:" + PATH_EXCHANGE_RATE_TYPE;
    public static final String EXCHANGE_RATE_TYPE_MISMATCH = "Exchange Rate Type does not match:" +
            PATH_EXCHANGE_RATE_TYPE;
    public static final String EXCHANGE_RATE_MISMATCH = "Exchange Rate does not match:" +
            PATH_EXCHANGE_RATE;
    public static final String  CONTRACT_IDENTIFICATION_NOT_FOUND = "Exchange Rate Contract Identification isn't " +
            "present in the request or in the consent:" + PATH_EXCHANGE_RATE_IDENTIFICATION;
    public static final String CONTRACT_IDENTIFICATION_MISMATCH = "Exchange Rate Contract Identification" +
            " does not match:" + PATH_EXCHANGE_RATE_IDENTIFICATION;
    public static final String CREDITOR_MISMATCH = "Creditor aren't present in the request or in the consent:" +
            PATH_CREDITOR;
    public static final String CREDITOR_NAME_MISMATCH = "Creditor Name is not matching:" +
            PATH_CREDITOR_NAME;
    public static final String POSTAL_ADDRESS_MISMATCH = "Postal Address does not match:" +
            PATH_CREDITOR_ADRESS;
    public static final String ULTIMATE_CREDITOR_MISMATCH = "UltimateCreditor does not match:" +
            PATH_ULTIMATE_CREDITOR;
    public static final String ULTIMATE_DEBTOR_MISMATCH = "UltimateDebtor does not match:" +
            PATH_ULTIMATE_DEBTOR;
    public static final String REGULATORY_REPORTING_MISMATCH = "RegulatoryReporting does not match:"
            + PATH_REGULATORY_REPORTING;
    public static final String WRONG_DATE_FORMAT_QUERY = "Wrongly formatted date. Use valid ISO 8601 date/date-time " +
            "format. :" + PATH_QUERY_PARAM;
    public static final String DEBTOR_ACC_SCHEME_NAME_MISMATCH = "Debtor Account Scheme name does not " +
            "match:" + PATH_DEBTOR_ACCOUNT_SCHEME;
    public static final String DEBTOR_ACC_SCHEME_NAME_NOT_FOUND = "Debtor Account Scheme name isn't present in the " +
            "request or in the consent:" + PATH_DEBTOR_ACCOUNT_SCHEME;
    public static final String DEBTOR_ACC_IDENTIFICATION_MISMATCH = "Debtor Account Identification does " +
            "not match:" + PATH_DEBTOR_ACCOUNT_IDENTIFICATION;
    public static final String DEBTOR_ACC_IDENTIFICATION_NOT_FOUND = "Debtor Account Identification isn't present " +
            "in the request or in the consent:" + PATH_DEBTOR_ACCOUNT_IDENTIFICATION;
    public static final String DEBTOR_ACC_NAME_MISMATCH = "Debtor Account Name does not match:" +
            PATH_DEBTOR_ACCOUNT_NAME;
    public static final String DEBTOR_ACC_SEC_IDENTIFICATION_MISMATCH = "Debtor Account Secondary Identification" +
            " does not match:" + PATH_DEBTOR_ACCOUNT_SECOND_IDENTIFICATION;
    public static final String REMMITANCE_REFERENCE_NOT_FOUND = "Remittance Information Reference isn't present in " +
            "the request or in the consent:" + PATH_REMITTANCE_INFO_REFERENCE;
    public static final String REMMITANCE_REFERENCE_MISMATCH = "Remittance Information Reference does not match:" +
            PATH_REMITTANCE_INFO_REFERENCE;
    public static final String REMMITANCE_UNSTRUCTURED_NOT_FOUND = "Remittance Information Unstructured isn't present" +
            " in the request or in the consent:" + PATH_REMITTANCE_INFO_UNSTRUCTURED;
    public static final String REMMITANCE_UNSTRUCTURED_MISMATCH = "Remittance Information Unstructured does not " +
            "match:" + PATH_REMITTANCE_INFO_UNSTRUCTURED;
    public static final String REQUESTED_EXECUTION_DATE_MISMATCH = "Requested Execution Date Time does not match.:" +
            PATH_EXECUTION_DATE;
    public static final String INSTRUCTION_IDENTIFICATION_MISMATCH = "Instruction Identification does not match:"
            + PATH_INSTRUCTION_IDENTIFICATION;
    public static final String INSTRUCTION_IDENTIFICATION_NOT_FOUND = "Instruction Identification isn't present in " +
            "the request:" + PATH_INSTRUCTION_IDENTIFICATION;
    public static final String END_TO_END_IDENTIFICATION_NOT_FOUND = "End to End Identification isn't present in " +
            "the request or in the consent:" + PATH_ENDTOEND_IDENTIFICATION;
    public static final String END_TO_END_IDENTIFICATION_MISMATCH = "End to End Identification does not match:"
            + PATH_ENDTOEND_IDENTIFICATION;
    public static final String INSTRUCTED_AMOUNT_CURRENCY_NOT_FOUND = "Instructed Amount Currency isn't present in " +
            "the payload:" + PATH_INSTRUCTED_AMOUNT;
    public static final String INSTRUCTED_AMOUNT_AMOUNT_MISMATCH = "Instructed Amount Amount does not match the " +
            "initiated amount:" + PATH_INSTRUCTED_AMOUNT_AMOUNT;
    public static final String INSTRUCTED_AMOUNT_AMOUNT_NOT_FOUND = "Instructed Amount  Amount isn't present in the " +
            "request:" + PATH_INSTRUCTED_AMOUNT;
    public static final String INSTRUCTED_AMOUNT_CURRENCY_MISMATCH = "Instructed Amount currency does not match the " +
            "initiated amount or currency:" + PATH_INSTRUCTED_AMOUNT_CURRENCY;
    public static final String INSTRUCTED_AMOUNT_NOT_FOUND = "Instructed Amount isn't present in the request:" +
            PATH_INSTRUCTED_AMOUNT;
    public static final String REQUESTED_EXECUTION_DATE_NOT_FOUND = "Requested Execution Date Time isn't present in" +
            " the request or in the consent.:" + PATH_EXECUTION_DATE;
    public static final String FREQUENCY_MISMATCH_API = "Frequency does not match.:" +
            PATH_FREQUENCY_API;
    public static final String FREQUENCY_NOT_FOUND_API = "Frequency isn't present in the request or in the consent.:"
            + PATH_FREQUENCY_API;
    public static final String FREQUENCY_MISMATCH = "Frequency does not match.:" + PATH_FREQUENCY;
    public static final String FREQUENCY_NOT_FOUND = "Frequency isn't present in the request or in the consent.:"
            + PATH_FREQUENCY;
    public static final String FIRST_PAYMENT_DATE_MISMATCH = "First Payment Date Time does not match.:" +
            PATH_FIRST_PAYMENT_DATE_TIME;
    public static final String FIRST_PAYMENT_DATE_MISMATCH_API = "First Payment Date Time does not match.:" +
            PATH_FIRST_PAYMENT_DATE_TIME_API;
    public static final String FIRST_PAYMENT_DATE_NOT_FOUND = "First Payment Date Time does not found in the request" +
            " or in the consent.:" + PATH_FIRST_PAYMENT_DATE_TIME;
    public static final String CURRENCY_TRANSFER_MISMATCH = "Currency Of Transfer does not match.:" +
            PATH_CURRENCY_OF_TRANSFER;
    public static final String CURRENCY_TRANSFER_NOT_FOUND = "Currency Of Transfer isn't present in the request or" +
            " in the consent.:" + PATH_CURRENCY_OF_TRANSFER;
    public static final String CHARGE_BEARER_MISMATCH = "Charge Bearer does not match.:" +
            PATH_CHARGE_BEARER;
    public static final String CHARGE_BEARER_NOT_FOUND = "Charge Bearer isn't present in the request or" +
            " in the consent.:" + PATH_CHARGE_BEARER;
    public static final String DESTINATION_COUNTRY_CODE_MISMATCH = "Charge Bearer does not match.:" +
            PATH_DESTINATION_COUNTRY_CODE;
    public static final String DESTINATION_COUNTRY_CODE_NOT_FOUND = "Charge Bearer isn't present in the request or" +
            " in the consent.:" + PATH_DESTINATION_COUNTRY_CODE;
    public static final String PAYMENT_CONTEXT_CODE_MISMATCH = "Risk Payment Context Code does not match:" +
            PATH_RISK_PAYMENT_CONTEXT;
    public static final String MERCHANT_CATEGORY_CODE_MISMATCH = "Merchant Category Code does not match:" +
            PATH_RISK_MERCHANT_CATEGORY;
    public static final String MERCHANT_CUSTOMER_IDENTIFICATION_MISMATCH = "Risk Merchant Customer Identification" +
            " does not match:" + PATH_RISK_MERCHANT_CUSTOMER;
    public static final String DELIVERY_ADDRESS_MISMATCH = "Risk Delivery Addresses isn't present in the " +
            "request or in the consent:" + PATH_RISK_ADDRESS;
    public static final String STREET_NAME_MISMATCH = "Risk Street Name does not match:" +
            PATH_RISK_ADDRESS_STREET_NAME;
    public static final String BUILDING_NUMBER_MISMATCH = "Risk Building Number does not match:" +
            PATH_RISK_ADDRESS_BUILDING_NO;
    public static final String POST_CODE_MISMATCH = "Risk Post Code does not match:" +
            PATH_RISK_ADDRESS_POST_CODE;
    public static final String TOWN_NAME_MISMATCH = "Risk Town Name does not match:" +
            PATH_RISK_ADDRESS_TOWN_NAME;
    public static final String COUNTRY_MISMATCH = "Risk Country does not match:" +
            PATH_RISK_COUNTRY;
    public static final String ADDRESS_LINE_NOT_FOUND = "Risk Address Lines aren't present in the request or consent:" +
            PATH_RISK_ADDRESS_LINE;
    public static final String ADDRESS_LINE_MISMATCH = "Risk Address Line does not match:" +
            PATH_RISK_ADDRESS_LINE;
    public static final String COUNTRY_SUB_DIVISION_NOT_FOUND = "Risk Country Sub Division isn't present either in" +
            " the request or consent:" + PATH_RISK_ADDRESS_COUNTRY_SUB_DIVISION;
    public static final String COUNTRY_SUB_DIVISION_MISMATCH = "Country Sub Division does not match:" +
            PATH_RISK_ADDRESS_COUNTRY_SUB_DIVISION;
    public static final String MANDATE_IDENTIFICATION_MISMATCH = "MandateIdentification does not match.:" +
            PATH_MANDATE_IDENTIFICATION;
    public static final String CATEGORY_PURPOSE_CODE_MISMATCH = "CategoryPurposeCode does not match.:" +
            PATH_CATEGORY_PURPOSE_CODE;
    public static final String CLASSIFICATION_MISMATCH = "Classification does not match.:" +
            PATH_CLASSIFICATION;
    public static final String REASON_MISMATCH = "Reason does not match.:" +
            PATH_REASON;
    public static final String CREDITOR_ACC_SCHEME_NAME_MISMATCH = "Creditor Accounts Scheme does not match:" +
            PATH_CREDIT_ACCOUNT_SCHEME;
    public static final String CREDITOR_ACC_SCHEME_NAME_NOT_FOUND = "Creditor Accounts Scheme isn't present in the" +
            " request or in the consent.:" + PATH_CREDIT_ACCOUNT_SCHEME;
    public static final String CREDITOR_ACC_PROXY_IDENTIFICATION_MISMATCH = "Creditor Accounts Proxy Identification " +
            "does not match:" + PATH_CREDIT_ACCOUNT_PROXY_IDENTIFICATION;
    public static final String CREDITOR_ACC_PROXY_IDENTIFICATION_NOT_FOUND = "Creditor Accounts Proxy Identification " +
            "isn't present in the request or in the consent.:"
            + PATH_CREDIT_ACCOUNT_PROXY_IDENTIFICATION;
    public static final String CREDITOR_ACC_PROXY_CODE_MISMATCH = "Creditor Accounts Proxy Code " +
            "does not match:" + PATH_CREDIT_ACCOUNT_PROXY_CODE;
    public static final String CREDITOR_ACC_PROXY_CODE_NOT_FOUND = "Creditor Accounts Proxy Code " +
            "isn't present in the request or in the consent.:"
            + PATH_CREDIT_ACCOUNT_PROXY_CODE;
    public static final String CREDITOR_ACC_PROXY_TYPE_MISMATCH = "Creditor Accounts Proxy Type " +
            "does not match:" + PATH_CREDIT_ACCOUNT_PROXY_TYPE;
    public static final String CREDITOR_ACC_IDENTIFICATION_MISMATCH = "Creditor Account Identification does not match:"
            + PATH_CREDIT_ACCOUNT_IDENTIFICATION;
    public static final String CREDITOR_ACC_IDENTIFICATION_NOT_FOUND = "Creditor Account Identification isn't " +
            "present in the request or in the consent.:" + PATH_CREDIT_ACCOUNT_IDENTIFICATION;
    public static final String CREDITOR_ACC_NAME_MISMATCH = "Creditor Account Name does not match:" +
            PATH_CREDIT_ACCOUNT_NAME;
    public static final String CREDITOR_ACC_SEC_IDENTIFICATION_MISMATCH = "Creditor Account Secondary Identification" +
            " does not match:" + PATH_CREDIT_ACCOUNT_SEC_IDENTIFICATION;
    public static final String CREDITOR_ACC_PROXY_MISMATCH = "Creditor Account Proxy does not match:"
            + PATH_CREDIT_ACCOUNT_PROXY;
    public static final String DEBTOR_ACC_PROXY_MISMATCH = "Debtor Account Proxy does not match:"
            + PATH_DEBTOR_ACCOUNT_PROXY;
    public static final String DEBTOR_ACC_PROXY_IDENTIFICATION_MISMATCH = "Debtor Accounts Proxy Identification " +
            "does not match:" + PATH_DEBTOR_ACCOUNT_PROXY_IDENTIFICATION;
    public static final String DEBTOR_ACC_PROXY_IDENTIFICATION_NOT_FOUND = "Debtor Accounts Proxy Identification " +
            "isn't present in the request or in the consent.:"
            + PATH_DEBTOR_ACCOUNT_PROXY_IDENTIFICATION;
    public static final String DEBTOR_ACC_PROXY_CODE_MISMATCH = "Debtor Accounts Proxy Code " +
            "does not match:" + PATH_DEBTOR_ACCOUNT_PROXY_CODE;
    public static final String DEBTOR_ACC_PROXY_CODE_NOT_FOUND = "Debtor Accounts Proxy Code " +
            "isn't present in the request or in the consent.:"
            + PATH_DEBTOR_ACCOUNT_PROXY_CODE;
    public static final String DEBTOR_ACC_PROXY_TYPE_MISMATCH = "Debtor Accounts Proxy Type " +
            "does not match:" + PATH_DEBTOR_ACCOUNT_PROXY_TYPE;
    public static final String ULTIMATE_CREDITOR_NAME_MISMATCH = "UltimateCreditors Name does not match:" +
            PATH_ULTIMATE_CREDITOR_NAME;
    public static final String ULTIMATE_CREDITOR_IDENTIFICATION_MISMATCH = "UltimateCreditors Identification does " +
            "not match:" + PATH_ULTIMATE_CREDITOR_IDENTIFICATION;
    public static final String ULTIMATE_CREDITOR_LEI_MISMATCH = "UltimateCreditors LEI does not match:" +
            PATH_ULTIMATE_CREDITOR_LEI;
    public static final String ULTIMATE_CREDITOR_SCHEME_NAME_MISMATCH = "UltimateCreditors SchemeName does not match:" +
            PATH_ULTIMATE_CREDITOR_SCHEME_NAME;
    public static final String ULTIMATE_CREDITOR_POSTAL_ADDRESS_MISMATCH = "UltimateCreditors PostalAddress does not " +
            "match:" + PATH_ULTIMATE_CREDITOR_POSTAL_ADDRESS;
    public static final String ULTIMATE_DEBTOR_NAME_MISMATCH = "UltimateDebtors Name does not match:" +
            PATH_ULTIMATE_DEBTOR_NAME;
    public static final String ULTIMATE_DEBTOR_IDENTIFICATION_MISMATCH = "UltimateDebtors Identification does " +
            "not match:" + PATH_ULTIMATE_DEBTOR_IDENTIFICATION;
    public static final String ULTIMATE_DEBTOR_LEI_MISMATCH = "UltimateDebtors LEI does not match:" +
            PATH_ULTIMATE_DEBTOR_LEI;
    public static final String ULTIMATE_DEBTOR_SCHEME_NAME_MISMATCH = "UltimateDebtors SchemeName does not match:" +
            PATH_ULTIMATE_DEBTOR_SCHEME_NAME;
    public static final String ULTIMATE_DEBTOR_POSTAL_ADDRESS_MISMATCH = "UltimateDebtors PostalAddress does not " +
            "match:" + PATH_ULTIMATE_DEBTOR_POSTAL_ADDRESS;

    // Account and payment envelope validation messages.
    public static final String PAYLOAD_FORMAT_ERROR = "Request Payload is not in correct JSON format";
    public static final String INVALID_URI_ERROR = "Path requested is invalid. :" + PATH_URL;
    public static final String PERMISSION_MISMATCH_ERROR = "Permission mismatch. Consent does not contain " +
            "necessary permissions. :" + PATH_PERMISSIONS;
    public static final String CONSENT_EXPIRED_ERROR = "Provided consent is expired. :" + PATH_EXPIRATION_DATE;
    public static final String ACCOUNT_CONSENT_STATE_INVALID = "Account validation failed due to invalid consent" +
            " state. :" + PATH_STATUS;
    public static final String ACCOUNT_ID_NOT_AVAILABLE_MSG = "Requested Resource with the given ID is " +
            "Unavailable. :" + PATH_ACCOUNT_ID;
    public static final String MSG_INVALID_CONSENT_ID = "The requested consent-Id does not match with the " +
            "consent-Id bound to token:" + PATH_CONSENT_ID;
    public static final String PAYMENT_CONSENT_STATE_INVALID = "Payment validation failed due to invalid consent" +
            " state.:" + PATH_STATUS;
    public static final String DATA_NOT_FOUND = "Data is not found or empty in the request.:" + PATH_DATA;
    public static final String INITIATION_NOT_FOUND = "Initiation is not found or empty in the request.:" +
            PATH_INITIATION;
    public static final String RISK_MISMATCH = "Risk does not match.:" + PATH_RISK;
    public static final String RISK_NOT_FOUND = "Risk is not found or empty in the request.:" + PATH_RISK;

    // Consent revocation messages.
    public static final String INVALID_CONSENT_ID = "Invalid Consent Id found in the request.:" +
            PATH_REVOKED_CONSENT_ID;
    public static final String INVALID_CONSENT_TYPE = "Invalid Consent Type found in the request";
    public static final String MISSING_CLIENT_ID = "Client Id is not available in the consent";
    public static final String CONSENT_REVOCATION_INVALID_STATUS = "Consent is not in a state that permits " +
            "revocation.:" + PATH_STATUS;
    public static final String PAYMENT_DELETE_NOT_SUPPORTED = "Method DELETE is not supported for payment consents";


    // Error codes returned in the errorMessage field of a failed-validation response.
    public static final String FIELD_INVALID = "Field.Invalid";
    public static final String FIELD_MISSING = "Field.Missing";
    public static final String FIELD_INVALID_DATE = "Field.InvalidDate";
    public static final String RESOURCE_CONSENT_MISMATCH = "Resource.ConsentMismatch";
    public static final String RESOURCE_INVALID_CONSENT_STATUS = "Resource.InvalidConsentStatus";
    public static final String RESOURCE_INVALID_FORMAT = "Resource.InvalidFormat";
    public static final String UNEXPECTED_ERROR = "UnexpectedError";

    // Messages returned when an accelerator error is rewritten into the published error format, one per
    // accelerator operation. The accelerator's own description is carried separately, in Errors[].Message.
    public static final String CONSENT_DEFAULT_ERROR = "An error occurred while processing the consent request";
    public static final String CONSENT_CREATE_ERROR = "Error occurred while initiating consent";
    public static final String CONSENT_RETRIEVE_ERROR = "Error occurred while retrieving consent";
    public static final String CONSENT_DELETE_ERROR = "Error occurred while deleting consent";
    public static final String CONSENT_UPDATE_ERROR = "Error occurred while updating consent";
    public static final String CONSENT_PARTIAL_UPDATE_ERROR = "Error occurred while partially updating consent";
    public static final String CONSENT_FILE_UPLOAD_ERROR = "Error occurred while uploading consent file";
    public static final String CONSENT_FILE_RETRIEVAL_ERROR = "Error occurred while retrieving consent file";
    public static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";

    // HTTP error status code used when building response bodies.
    public static final Integer BAD_REQUEST = 400;

    // Error message labels carried in a 400 or 500 error response.
    public static final String INVALID_REQUEST_MSG = "invalid_request";
    public static final String SERVER_ERROR_MSG = "server_error";

}
