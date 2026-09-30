package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@JsonTypeName("SuccessResponseForEnrichEventPolling_data")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class SuccessResponseForEnrichEventPollingData   {
  private Object eventPollingResponse;

  public SuccessResponseForEnrichEventPollingData() {
  }

  /**
   * Event Polling Response
   **/
  public SuccessResponseForEnrichEventPollingData eventPollingResponse(Object eventPollingResponse) {
    this.eventPollingResponse = eventPollingResponse;
    return this;
  }

  
  @ApiModelProperty(value = "Event Polling Response")
  @JsonProperty("eventPollingResponse")
  public Object getEventPollingResponse() {
    return eventPollingResponse;
  }

  @JsonProperty("eventPollingResponse")
  public void setEventPollingResponse(Object eventPollingResponse) {
    this.eventPollingResponse = eventPollingResponse;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SuccessResponseForEnrichEventPollingData successResponseForEnrichEventPollingData = (SuccessResponseForEnrichEventPollingData) o;
    return Objects.equals(this.eventPollingResponse, successResponseForEnrichEventPollingData.eventPollingResponse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventPollingResponse);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SuccessResponseForEnrichEventPollingData {\n");
    
    sb.append("    eventPollingResponse: ").append(toIndentedString(eventPollingResponse)).append("\n");
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
