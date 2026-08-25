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

package org.wso2.non.regulated.ob.demo.backend.services;

import org.wso2.non.regulated.ob.demo.backend.CommonConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Mock accounts API service. Returns static, schema-valid example responses matching
 * apis/accounts/account-info-openapi.yaml (AccountsResponse, TransactionsResponse, BalancesResponse).
 */
@Path("/accountservice/")
public class AccountService {

    private static final String DEMO_ACCOUNT_1 = "5500001122";
    private static final String DEMO_ACCOUNT_2 = "5500002233";

    /**
     * Lists the two demo accounts.
     *
     * @return 200 with an AccountsResponse payload
     */
    @GET
    @Path("/accounts")
    @Produces("application/json; charset=utf-8")
    public Response getAccounts() {
        String items = account(DEMO_ACCOUNT_1, "Bills") + ",\n" + account(DEMO_ACCOUNT_2, "Savings");
        return wrap(envelope("Account", items, "/accounts"));
    }

    /**
     * Returns the requested account, echoing back whatever account id was asked for.
     *
     * @return 200 with a single-element AccountsResponse payload
     */
    @GET
    @Path("/accounts/{AccountId}")
    @Produces("application/json; charset=utf-8")
    public Response getOneAccount(@PathParam("AccountId") String accountId) {
        return wrap(envelope("Account", account(accountId, "Bills"), "/accounts/" + accountId));
    }

    /**
     * Returns the transactions of a single account.
     *
     * @return 200 with a TransactionsResponse payload
     */
    @GET
    @Path("/accounts/{AccountId}/transactions")
    @Produces("application/json; charset=utf-8")
    public Response getAccountTransactions(@PathParam("AccountId") String accountId) {
        return wrap(envelope("Transaction", transaction(accountId), "/accounts/" + accountId + "/transactions"));
    }

    /**
     * Returns the transactions of every demo account in one call.
     *
     * @return 200 with a TransactionsResponse payload
     */
    @GET
    @Path("/transactions")
    @Produces("application/json; charset=utf-8")
    public Response getBulkTransactions() {
        String items = transaction(DEMO_ACCOUNT_1) + ",\n" + transaction(DEMO_ACCOUNT_2);
        return wrap(envelope("Transaction", items, "/transactions"));
    }

    /**
     * Returns the balance of a single account.
     *
     * @return 200 with a BalancesResponse payload
     */
    @GET
    @Path("/accounts/{AccountId}/balances")
    @Produces("application/json; charset=utf-8")
    public Response getAccountBalance(@PathParam("AccountId") String accountId) {
        return wrap(envelope("Balance", balance(accountId), "/accounts/" + accountId + "/balances"));
    }

    /**
     * Returns the balance of every demo account in one call.
     *
     * @return 200 with a BalancesResponse payload
     */
    @GET
    @Path("/balances")
    @Produces("application/json; charset=utf-8")
    public Response getBulkBalance() {
        String items = balance(DEMO_ACCOUNT_1) + ",\n" + balance(DEMO_ACCOUNT_2);
        return wrap(envelope("Balance", items, "/balances"));
    }

    /**
     * Wraps the given payload in a 200 response carrying the default FAPI interaction id.
     *
     * @param entity the JSON body to return
     * @return the response to hand back to the JAX-RS runtime
     */
    private static Response wrap(String entity) {
        return Response.status(200).entity(entity)
                .header(CommonConstants.HEADER_FAPI_INTERACTION_ID, CommonConstants.DEFAULT_INTERACTION_ID)
                .build();
    }

    /**
     * Wraps a list of resource objects in the standard Data/Links/Meta response envelope.
     *
     * @param dataKey  name of the array inside Data, for example {@code Account} or {@code Balance}
     * @param items    the already rendered array elements, comma separated and without a trailing newline
     * @param selfPath path appended to the base URL to form the Links.Self value
     * @return the complete JSON document
     */
    private static String envelope(String dataKey, String items, String selfPath) {
        return "{\n" +
                "  \"Data\": {\n" +
                "    \"" + dataKey + "\": [\n" +
                items + "\n" +
                "    ]\n" +
                "  },\n" +
                "  \"Links\": {\n" +
                "    \"Self\": \"" + CommonConstants.SELF_LINK_BASE_URL + selfPath + "\"\n" +
                "  },\n" +
                "  \"Meta\": {\n" +
                "    \"TotalPages\": 1\n" +
                "  }\n" +
                "}";
    }

    /**
     * Renders an Account example, given a required AccountId and an Account.Identification (both mirrored
     * per spec).
     *
     * @param nickname value of the Nickname field
     * @return the JSON object, indented to sit inside the envelope's Data.Account array
     */
    private static String account(String accountId, String nickname) {
        return "      {\n" +
                "        \"AccountId\": \"" + accountId + "\",\n" +
                "        \"Status\": \"Enabled\",\n" +
                "        \"StatusUpdateDateTime\": \"2020-04-16T06:06:06+00:00\",\n" +
                "        \"Currency\": \"EUR\",\n" +
                "        \"AccountCategory\": \"Personal\",\n" +
                "        \"AccountTypeCode\": \"CACC\",\n" +
                "        \"Nickname\": \"" + nickname + "\",\n" +
                "        \"OpeningDate\": \"2020-01-16T06:06:06+00:00\",\n" +
                "        \"Account\": [\n" +
                "          {\n" +
                "            \"SchemeName\": \"BBAN\",\n" +
                "            \"Identification\": \"" + accountId + "\",\n" +
                "            \"Name\": \"Mr Kevin\",\n" +
                "            \"SecondaryIdentification\": \"00021\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }";
    }

    /**
     * Renders a Transaction example (required: AccountId, CreditDebitIndicator, Status, BookingDateTime,
     * Amount).
     *
     * @return the JSON object, indented to sit inside the envelope's Data.Transaction array
     */
    private static String transaction(String accountId) {
        return "      {\n" +
                "        \"AccountId\": \"" + accountId + "\",\n" +
                "        \"TransactionId\": \"TXN-0001\",\n" +
                "        \"TransactionReference\": \"Ref 1\",\n" +
                "        \"Amount\": {\n" +
                "          \"Amount\": \"10.00\",\n" +
                "          \"Currency\": \"EUR\"\n" +
                "        },\n" +
                "        \"CreditDebitIndicator\": \"Credit\",\n" +
                "        \"Status\": \"BOOK\",\n" +
                "        \"BookingDateTime\": \"2017-04-05T10:43:07+00:00\",\n" +
                "        \"ValueDateTime\": \"2017-04-05T10:45:22+00:00\",\n" +
                "        \"TransactionInformation\": \"Cash from Aubrey\",\n" +
                "        \"BankTransactionCode\": {\n" +
                "          \"Code\": \"ReceivedCreditTransfer\",\n" +
                "          \"SubCode\": \"DomesticCreditTransfer\"\n" +
                "        },\n" +
                "        \"ProprietaryBankTransactionCode\": {\n" +
                "          \"Code\": \"Transfer\",\n" +
                "          \"Issuer\": \"AlphaBank\"\n" +
                "        }\n" +
                "      }";
    }

    /**
     * Renders a BalancesResponse.Data.Balance example (required: AccountId, CreditDebitIndicator, Type,
     * DateTime, Amount).
     *
     * @return the JSON object, indented to sit inside the envelope's Data.Balance array
     */
    private static String balance(String accountId) {
        return "      {\n" +
                "        \"AccountId\": \"" + accountId + "\",\n" +
                "        \"Amount\": {\n" +
                "          \"Amount\": \"1230.00\",\n" +
                "          \"Currency\": \"EUR\"\n" +
                "        },\n" +
                "        \"CreditDebitIndicator\": \"Credit\",\n" +
                "        \"Type\": \"ITAV\",\n" +
                "        \"DateTime\": \"2017-04-05T10:43:07+00:00\"\n" +
                "      }";
    }
}
