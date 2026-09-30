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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Bank Account Service class to retrieve sharable and payable accounts.
 */
@Path("/bankaccountservice/")
public class BankAccountService {

    /**
     * The two demo accounts, named as the Accounts API names them, so that the account a customer
     * picks on the consent page reads the same as the account they later see in the client
     * application. Keep these ids and names in step with {@code AccountService}.
     */
    private static final String ACCOUNT_LIST = "{\n" +
            "    \"data\": [\n" +
            "        {\n" +
            "            \"accountId\": \"5500001122\",\n" +
            "            \"displayName\": \"Bills\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"accountId\": \"5500002233\",\n" +
            "            \"displayName\": \"Savings\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    /**
     * Lists the accounts the customer may pay from.
     *
     * @return 200 with the demo account list
     */
    @GET
    @Path("/payable-accounts")
    @Produces("application/json; charset=utf-8")
    public Response getPayableAccounts() {
        return Response.status(200).entity(ACCOUNT_LIST).build();
    }

    /**
     * Lists the accounts the customer may share with a client application.
     *
     * @return 200 with the demo account list
     */
    @GET
    @Path("/sharable-accounts")
    @Produces("application/json; charset=utf-8")
    public Response getSharableAccounts() {
        return Response.status(200).entity(ACCOUNT_LIST).build();
    }

}
