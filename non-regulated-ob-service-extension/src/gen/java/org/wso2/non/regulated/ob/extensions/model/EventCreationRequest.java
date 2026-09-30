package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;


@JsonTypeName("EventCreationRequest")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class EventCreationRequest   {
  private Object eventData;

  public EventCreationRequest() {
  }

  /**
   * Event creation Payload
   **/
  public EventCreationRequest eventData(Object eventData) {
    this.eventData = eventData;
    return this;
  }

  
  @ApiModelProperty(value = "Event creation Payload")
  @JsonProperty("eventData")
  public Object getEventData() {
    return eventData;
  }

  @JsonProperty("eventData")
  public void setEventData(Object eventData) {
    this.eventData = eventData;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventCreationRequest eventCreationRequest = (EventCreationRequest) o;
    return Objects.equals(this.eventData, eventCreationRequest.eventData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventData);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EventCreationRequest {\n");
    
    sb.append("    eventData: ").append(toIndentedString(eventData)).append("\n");
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
