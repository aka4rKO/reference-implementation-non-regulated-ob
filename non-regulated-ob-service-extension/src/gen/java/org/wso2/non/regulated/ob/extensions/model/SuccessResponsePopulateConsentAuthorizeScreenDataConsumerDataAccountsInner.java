package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;


@JsonTypeName("SuccessResponsePopulateConsentAuthorizeScreenData_consumerData_accounts_inner")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner   {
  private String displayName;
  private String title;
  private String description;
  private Boolean selected;

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner() {
  }

  @JsonCreator
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner(
    @JsonProperty(required = true, value = "displayName") String displayName,
    @JsonProperty(required = true, value = "selected") Boolean selected
  ) {
    this.displayName = displayName;
    this.selected = selected;
  }

  /**
   * Account display name
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Account display name")
  @JsonProperty(required = true, value = "displayName")
  @NotNull public String getDisplayName() {
    return displayName;
  }

  @JsonProperty(required = true, value = "displayName")
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Account title
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner title(String title) {
    this.title = title;
    return this;
  }

  
  @ApiModelProperty(value = "Account title")
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Account description
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner description(String description) {
    this.description = description;
    return this;
  }

  
  @ApiModelProperty(value = "Account description")
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Whether the account is selected by default
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner selected(Boolean selected) {
    this.selected = selected;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "Whether the account is selected by default")
  @JsonProperty(required = true, value = "selected")
  @NotNull public Boolean getSelected() {
    return selected;
  }

  @JsonProperty(required = true, value = "selected")
  public void setSelected(Boolean selected) {
    this.selected = selected;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner successResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner = (SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner) o;
    return Objects.equals(this.displayName, successResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner.displayName) &&
        Objects.equals(this.title, successResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner.title) &&
        Objects.equals(this.description, successResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner.description) &&
        Objects.equals(this.selected, successResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner.selected);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayName, title, description, selected);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner {\n");
    
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    selected: ").append(toIndentedString(selected)).append("\n");
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
