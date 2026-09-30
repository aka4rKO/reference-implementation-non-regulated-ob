package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@JsonTypeName("SuccessResponseForResponseAlternationData")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class SuccessResponseForResponseAlternationData   {
  private Object responseHeaders;
  private Object modifiedResponse;

  public SuccessResponseForResponseAlternationData() {
  }

  /**
   * Headers to be included in the response.
   **/
  public SuccessResponseForResponseAlternationData responseHeaders(Object responseHeaders) {
    this.responseHeaders = responseHeaders;
    return this;
  }

  
  @ApiModelProperty(value = "Headers to be included in the response.")
  @JsonProperty("responseHeaders")
  public Object getResponseHeaders() {
    return responseHeaders;
  }

  @JsonProperty("responseHeaders")
  public void setResponseHeaders(Object responseHeaders) {
    this.responseHeaders = responseHeaders;
  }

  /**
   * Generated custom response body
   **/
  public SuccessResponseForResponseAlternationData modifiedResponse(Object modifiedResponse) {
    this.modifiedResponse = modifiedResponse;
    return this;
  }

  
  @ApiModelProperty(value = "Generated custom response body")
  @JsonProperty("modifiedResponse")
  public Object getModifiedResponse() {
    return modifiedResponse;
  }

  @JsonProperty("modifiedResponse")
  public void setModifiedResponse(Object modifiedResponse) {
    this.modifiedResponse = modifiedResponse;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SuccessResponseForResponseAlternationData successResponseForResponseAlternationData = (SuccessResponseForResponseAlternationData) o;
    return Objects.equals(this.responseHeaders, successResponseForResponseAlternationData.responseHeaders) &&
        Objects.equals(this.modifiedResponse, successResponseForResponseAlternationData.modifiedResponse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responseHeaders, modifiedResponse);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SuccessResponseForResponseAlternationData {\n");
    
    sb.append("    responseHeaders: ").append(toIndentedString(responseHeaders)).append("\n");
    sb.append("    modifiedResponse: ").append(toIndentedString(modifiedResponse)).append("\n");
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
