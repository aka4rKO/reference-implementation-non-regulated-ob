package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@JsonTypeName("SuccessResponseForEnrichEventSubscription_data")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class SuccessResponseForEnrichEventSubscriptionData   {
  private Object eventSubscriptionResponse;

  public SuccessResponseForEnrichEventSubscriptionData() {
  }

  /**
   * Event Subscription Response
   **/
  public SuccessResponseForEnrichEventSubscriptionData eventSubscriptionResponse(Object eventSubscriptionResponse) {
    this.eventSubscriptionResponse = eventSubscriptionResponse;
    return this;
  }

  
  @ApiModelProperty(value = "Event Subscription Response")
  @JsonProperty("eventSubscriptionResponse")
  public Object getEventSubscriptionResponse() {
    return eventSubscriptionResponse;
  }

  @JsonProperty("eventSubscriptionResponse")
  public void setEventSubscriptionResponse(Object eventSubscriptionResponse) {
    this.eventSubscriptionResponse = eventSubscriptionResponse;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SuccessResponseForEnrichEventSubscriptionData successResponseForEnrichEventSubscriptionData = (SuccessResponseForEnrichEventSubscriptionData) o;
    return Objects.equals(this.eventSubscriptionResponse, successResponseForEnrichEventSubscriptionData.eventSubscriptionResponse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventSubscriptionResponse);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SuccessResponseForEnrichEventSubscriptionData {\n");
    
    sb.append("    eventSubscriptionResponse: ").append(toIndentedString(eventSubscriptionResponse)).append("\n");
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
