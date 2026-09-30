package org.wso2.non.regulated.ob.extensions.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;


@JsonTypeName("EventPollingRequest")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class EventPollingRequest   {
  private Object eventPollingData;

  public EventPollingRequest() {
  }

  /**
   * Event polling data
   **/
  public EventPollingRequest eventPollingData(Object eventPollingData) {
    this.eventPollingData = eventPollingData;
    return this;
  }

  
  @ApiModelProperty(value = "Event polling data")
  @JsonProperty("eventPollingData")
  public Object getEventPollingData() {
    return eventPollingData;
  }

  @JsonProperty("eventPollingData")
  public void setEventPollingData(Object eventPollingData) {
    this.eventPollingData = eventPollingData;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EventPollingRequest eventPollingRequest = (EventPollingRequest) o;
    return Objects.equals(this.eventPollingData, eventPollingRequest.eventPollingData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventPollingData);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EventPollingRequest {\n");
    
    sb.append("    eventPollingData: ").append(toIndentedString(eventPollingData)).append("\n");
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
