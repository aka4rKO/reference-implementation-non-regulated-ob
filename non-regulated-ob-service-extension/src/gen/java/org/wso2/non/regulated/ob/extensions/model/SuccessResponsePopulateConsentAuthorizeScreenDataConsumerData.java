package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Consumer related data fetched from the banking backend.
 **/
@ApiModel(description = "Consumer related data fetched from the banking backend.")
@JsonTypeName("SuccessResponsePopulateConsentAuthorizeScreenData_consumerData")
@JsonFormat(shape=JsonFormat.Shape.OBJECT)
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData  {
  private @Valid List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner> accounts = new ArrayList<>();
  private Map<String, Object> additionalProperties = new HashMap<>();

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData() {
  }

  /**
   * List of all user accounts/resources selectable in the UI
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData accounts(List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner> accounts) {
    this.accounts = accounts;
    return this;
  }

  
  @ApiModelProperty(value = "List of all user accounts/resources selectable in the UI")
  @JsonProperty("accounts")
  @Valid public List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner> getAccounts() {
    return accounts;
  }

  @JsonProperty("accounts")
  public void setAccounts(List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner> accounts) {
    this.accounts = accounts;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData addAccountsItem(SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner accountsItem) {
    if (this.accounts == null) {
      this.accounts = new ArrayList<>();
    }

    this.accounts.add(accountsItem);
    return this;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData removeAccountsItem(SuccessResponsePopulateConsentAuthorizeScreenDataConsumerDataAccountsInner accountsItem) {
    if (accountsItem != null && this.accounts != null) {
      this.accounts.remove(accountsItem);
    }

    return this;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData successResponsePopulateConsentAuthorizeScreenDataConsumerData = (SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData) o;
    return Objects.equals(this.accounts, successResponsePopulateConsentAuthorizeScreenDataConsumerData.accounts) &&
            Objects.equals(this.additionalProperties, successResponsePopulateConsentAuthorizeScreenDataConsumerData.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accounts, additionalProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SuccessResponsePopulateConsentAuthorizeScreenDataConsumerData {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    accounts: ").append(toIndentedString(accounts)).append("\n");
    sb.append("    additionalProperties: ").append(toIndentedString(additionalProperties)).append("\n");
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
