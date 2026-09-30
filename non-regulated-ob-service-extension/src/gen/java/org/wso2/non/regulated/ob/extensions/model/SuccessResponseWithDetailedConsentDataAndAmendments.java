package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

import javax.validation.Valid;


@JsonTypeName("SuccessResponseWithDetailedConsentDataAndAmendments")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class SuccessResponseWithDetailedConsentDataAndAmendments   {
  private DetailedConsentResourceDataWithAmendments consentResource;

  public SuccessResponseWithDetailedConsentDataAndAmendments() {
  }

  /**
   **/
  public SuccessResponseWithDetailedConsentDataAndAmendments consentResource(DetailedConsentResourceDataWithAmendments consentResource) {
    this.consentResource = consentResource;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("consentResource")
  @Valid public DetailedConsentResourceDataWithAmendments getConsentResource() {
    return consentResource;
  }

  @JsonProperty("consentResource")
  public void setConsentResource(DetailedConsentResourceDataWithAmendments consentResource) {
    this.consentResource = consentResource;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SuccessResponseWithDetailedConsentDataAndAmendments successResponseWithDetailedConsentDataAndAmendments = (SuccessResponseWithDetailedConsentDataAndAmendments) o;
    return Objects.equals(this.consentResource, successResponseWithDetailedConsentDataAndAmendments.consentResource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consentResource);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SuccessResponseWithDetailedConsentDataAndAmendments {\n");
    
    sb.append("    consentResource: ").append(toIndentedString(consentResource)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }


}
