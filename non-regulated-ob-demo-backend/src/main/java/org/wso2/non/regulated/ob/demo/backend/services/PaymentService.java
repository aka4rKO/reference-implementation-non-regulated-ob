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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Mock payment initiation API service. Returns static, schema-valid example responses matching
 * apis/payments/payment-initiation-openapi.yaml (DomesticPaymentResponse, DomesticScheduledPaymentResponse,
 * DomesticStandingOrderResponse, InternationalPaymentResponse, FundsConfirmationResponse).
 */
@Path("/paymentservice/")
public class PaymentService {

    private static final String PLACEHOLDER_JWS_SIGNATURE = "placeholder-jws-signature";

    /**
     * Accepts a payment submission of any supported type and returns the created payment resource.
     *
     * @param paymentType payment type path segment, for example {@code domestic-payments}
     * @param fid         interaction id sent by the caller; a default is used when absent
     * @return 201 with the payment response for the given type
     */
    @POST
    @Path("/{paymentType}")
    @Produces("application/json; charset=utf-8")
    public Response paymentSubmission(@PathParam("paymentType") String paymentType,
            @HeaderParam(CommonConstants.HEADER_FAPI_INTERACTION_ID) String fid) {
        String response = buildPaymentResponse(paymentType, "PMT-0001");
        String interactionId = (fid == null || fid.isEmpty()) ? CommonConstants.DEFAULT_INTERACTION_ID : fid;
        return wrap(201, response, interactionId);
    }

    /**
     * Retrieves a previously submitted payment, echoing back whatever payment id was asked for.
     *
     * @param paymentType payment type path segment, for example {@code domestic-payments}
     * @return 200 with the payment response for the given type
     */
    @GET
    @Path("/{paymentType}/{paymentId}")
    @Produces("application/json; charset=utf-8")
    public Response getPaymentTypePayment(@PathParam("paymentType") String paymentType,
            @PathParam("paymentId") String paymentId) {
        String response = buildPaymentResponse(paymentType, paymentId);
        return wrap(200, response, CommonConstants.DEFAULT_INTERACTION_ID);
    }

    /**
     * Confirms that funds are available for a payment; the mock always answers yes.
     *
     * @return 200 with a FundsConfirmationResponse payload
     */
    @GET
    @Path("/{paymentType}/{paymentId}/funds-confirmation")
    @Produces("application/json; charset=utf-8")
    public Response getFundsConfirmation(@PathParam("paymentId") String paymentId) {
        return wrap(200, fundsConfirmationResponse(paymentId), CommonConstants.DEFAULT_INTERACTION_ID);
    }

    /**
     * Wraps the given payload in a response carrying the FAPI interaction id and a placeholder signature.
     *
     * @param status        HTTP status code to return
     * @param entity        the JSON body to return
     * @param interactionId value for the FAPI interaction id header
     * @return the response to hand back to the JAX-RS runtime
     */
    private static Response wrap(int status, String entity, String interactionId) {
        return Response.status(status).entity(entity)
                .header(CommonConstants.HEADER_FAPI_INTERACTION_ID, interactionId)
                .header("x-jws-signature", PLACEHOLDER_JWS_SIGNATURE)
                .build();
    }

    /**
     * Renders the trailing Links/Meta section that closes every payment response.
     *
     * @param selfPath path appended to the base URL to form the Links.Self value
     * @return the JSON fragment from {@code "Links"} through the document's closing brace
     */
    private static String linksAndMeta(String selfPath) {
        return "  \"Links\": {\n" +
                "    \"Self\": \"" + CommonConstants.SELF_LINK_BASE_URL + selfPath + "\"\n" +
                "  },\n" +
                "  \"Meta\": {}\n" +
                "}";
    }

    /**
     * Builds a payment response example for the given payment type, filling in the required
     * {PaymentTypeId, ConsentId, CreationDateTime, Status, StatusUpdateDateTime, Initiation} fields.
     * Any unrecognised payment type falls back to the domestic payment example.
     *
     * @param paymentType payment type path segment, for example {@code domestic-payments}
     * @param paymentId   id echoed into both the type-specific id field and the ConsentId
     * @return the complete JSON document
     */
    private static String buildPaymentResponse(String paymentType, String paymentId) {
        String idField;
        String initiation;
        String status;

        if (CommonConstants.DOMESTIC_SCHEDULED_PAYMENT.equals(paymentType)) {
            idField = "DomesticScheduledPaymentId";
            initiation = domesticScheduledInitiation();
            status = "RCVD";
        } else if (CommonConstants.DOMESTIC_STANDING_ORDER.equals(paymentType)) {
            idField = "DomesticStandingOrderId";
            initiation = domesticStandingOrderInitiation();
            status = "RCVD";
        } else if (CommonConstants.INTERNATIONAL_PAYMENT.equals(paymentType)) {
            idField = "InternationalPaymentId";
            initiation = internationalInitiation();
            status = "ACSP";
        } else {
            idField = "DomesticPaymentId";
            initiation = domesticInitiation();
            status = "ACSP";
        }

        return "{\n" +
                "  \"Data\": {\n" +
                "    \"" + idField + "\": \"" + paymentId + "\",\n" +
                "    \"ConsentId\": \"" + paymentId + "-consent\",\n" +
                "    \"CreationDateTime\": \"2026-08-04T10:43:07+00:00\",\n" +
                "    \"Status\": \"" + status + "\",\n" +
                "    \"StatusUpdateDateTime\": \"2026-08-04T10:43:07+00:00\",\n" +
                "    \"Initiation\": {\n" +
                initiation +
                "    }\n" +
                "  },\n" +
                linksAndMeta("/" + paymentType + "/" + paymentId);
    }

    /**
     * Renders a FundsConfirmationResponse example (required: FundsAvailableDateTime, FundsAvailable).
     *
     * @param paymentId path appended to the base URL to form the Links.Self value
     * @return the complete JSON document
     */
    private static String fundsConfirmationResponse(String paymentId) {
        return "{\n" +
                "  \"Data\": {\n" +
                "    \"FundsAvailableResult\": {\n" +
                "      \"FundsAvailableDateTime\": \"2026-08-04T10:43:07+00:00\",\n" +
                "      \"FundsAvailable\": true\n" +
                "    }\n" +
                "  },\n" +
                linksAndMeta("/" + paymentId + "/funds-confirmation");
    }

    /**
     * Renders the CreditorAccount object shared by every payment type's Initiation example.
     *
     * @return the JSON object, indented to sit inside an Initiation object
     */
    private static String creditorAccount() {
        return "        \"CreditorAccount\": {\n" +
                "          \"SchemeName\": \"BBAN\",\n" +
                "          \"Identification\": \"5500001122\",\n" +
                "          \"Name\": \"Receiver Co\"\n" +
                "        }\n";
    }

    /**
     * Renders a DomesticPaymentRequest Initiation example (required: InstructionIdentification,
     * EndToEndIdentification, InstructedAmount, CreditorAccount).
     *
     * @return the JSON fragment to sit inside the response's Data.Initiation object
     */
    private static String domesticInitiation() {
        return "        \"InstructionIdentification\": \"ID412\",\n" +
                "        \"EndToEndIdentification\": \"E2E123\",\n" +
                "        \"InstructedAmount\": {\n" +
                "          \"Amount\": \"100.00\",\n" +
                "          \"Currency\": \"EUR\"\n" +
                "        },\n" +
                creditorAccount();
    }

    /**
     * Renders a DomesticScheduledPaymentRequest Initiation example (required: InstructionIdentification,
     * RequestedExecutionDateTime, InstructedAmount, CreditorAccount).
     *
     * @return the JSON fragment to sit inside the response's Data.Initiation object
     */
    private static String domesticScheduledInitiation() {
        return "        \"InstructionIdentification\": \"ID412\",\n" +
                "        \"RequestedExecutionDateTime\": \"2026-09-01T00:00:00+00:00\",\n" +
                "        \"InstructedAmount\": {\n" +
                "          \"Amount\": \"100.00\",\n" +
                "          \"Currency\": \"EUR\"\n" +
                "        },\n" +
                creditorAccount();
    }

    /**
     * Renders a DomesticStandingOrderRequest Initiation example (required: MandateRelatedInformation,
     * FirstPaymentAmount, CreditorAccount).
     *
     * @return the JSON fragment to sit inside the response's Data.Initiation object
     */
    private static String domesticStandingOrderInitiation() {
        return "        \"MandateRelatedInformation\": {\n" +
                "          \"Frequency\": {\n" +
                "            \"Type\": \"MNTH\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"FirstPaymentAmount\": {\n" +
                "          \"Amount\": \"50.00\",\n" +
                "          \"Currency\": \"EUR\"\n" +
                "        },\n" +
                creditorAccount();
    }

    /**
     * Renders an InternationalPaymentRequest Initiation example (required: InstructionIdentification,
     * EndToEndIdentification, CurrencyOfTransfer, InstructedAmount, CreditorAccount).
     *
     * @return the JSON fragment to sit inside the response's Data.Initiation object
     */
    private static String internationalInitiation() {
        return "        \"InstructionIdentification\": \"ID412\",\n" +
                "        \"EndToEndIdentification\": \"E2E123\",\n" +
                "        \"CurrencyOfTransfer\": \"USD\",\n" +
                "        \"InstructedAmount\": {\n" +
                "          \"Amount\": \"100.00\",\n" +
                "          \"Currency\": \"EUR\"\n" +
                "        },\n" +
                creditorAccount();
    }
}
