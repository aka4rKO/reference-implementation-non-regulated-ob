package org.wso2.non.regulated.ob.extensions.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.wso2.non.regulated.ob.extensions.impl.PersistAuthorizedConsentApiImpl;
import org.wso2.non.regulated.ob.extensions.model.ErrorResponse;
import org.wso2.non.regulated.ob.extensions.model.PersistAuthorizedConsentRequestBody;
import org.wso2.non.regulated.ob.extensions.model.Response200ForPersistAuthorizedConsent;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
* Represents a collection of functions to interact with the API endpoints.
*/
@Path("/persist-authorized-consent")
@Api(description = "the persist-authorized-consent API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class PersistAuthorizedConsentApi {

    @POST
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "handle consent persistence logic and enrich response with user authorization and account mapping data", notes = "", response = Response200ForPersistAuthorizedConsent.class, authorizations = {
        @Authorization(value = "OAuth2", scopes = {
             }),
        
        @Authorization(value = "BasicAuth")
         }, tags={ "Consent" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Successful response", response = Response200ForPersistAuthorizedConsent.class),
        @ApiResponse(code = 400, message = "Bad Request", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Server Error", response = ErrorResponse.class)
    })
    public Response persistAuthorizedConsentPost(@Valid @NotNull PersistAuthorizedConsentRequestBody persistAuthorizedConsentRequestBody) {

        return PersistAuthorizedConsentApiImpl.handlePersistAuthorizedConsent(persistAuthorizedConsentRequestBody);
    }
}
