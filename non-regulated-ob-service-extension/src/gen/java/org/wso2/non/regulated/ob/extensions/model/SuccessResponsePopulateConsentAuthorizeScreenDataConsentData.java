package org.wso2.non.regulated.ob.extensions.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;


@JsonTypeName("SuccessResponsePopulateConsentAuthorizeScreenData_consentData")
@JsonFormat(shape=JsonFormat.Shape.OBJECT)
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-07-23T14:19:20.447074+05:30[Asia/Colombo]", comments = "Generator version: 7.24.0")
public class SuccessResponsePopulateConsentAuthorizeScreenDataConsentData  {
  private String type;
  private @Valid Map<String, List<String>> basicConsentData = new HashMap<>();
  private @Valid List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner> permissions = new ArrayList<>();
  private @Valid List<Account> initiatedAccountsForConsent = new ArrayList<>();
  private Boolean allowMultipleAccounts;
  private Boolean handleAccountSelectionSeparately;
  private Boolean isReauthorization;
  private Object consentMetadata;
  private Map<String, Object> additionalProperties = new HashMap<>();

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData() {
  }

  /**
   * The type of consent
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData type(String type) {
    this.type = type;
    return this;
  }

  
  @ApiModelProperty(value = "The type of consent")
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Structured descriptive text shown on the consent page, split into sections. Each key is a section title, and its value is a list of bullet points displayed under that section.
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData basicConsentData(Map<String, List<String>> basicConsentData) {
    this.basicConsentData = basicConsentData;
    return this;
  }

  
  @ApiModelProperty(value = "Structured descriptive text shown on the consent page, split into sections. Each key is a section title, and its value is a list of bullet points displayed under that section.")
  @JsonProperty("basicConsentData")
  public Map<String, List<String>> getBasicConsentData() {
    return basicConsentData;
  }

  @JsonProperty("basicConsentData")
  public void setBasicConsentData(Map<String, List<String>> basicConsentData) {
    this.basicConsentData = basicConsentData;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData putBasicConsentDataItem(String key, List<String> basicConsentDataItem) {
    if (this.basicConsentData == null) {
      this.basicConsentData = new HashMap<>();
    }

    this.basicConsentData.put(key, basicConsentDataItem);
    return this;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData removeBasicConsentDataItem(String key) {
    if (this.basicConsentData != null) {
      this.basicConsentData.remove(key);
    }

    return this;
  }
  /**
   * List of permissions for the consent (optional)
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData permissions(List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner> permissions) {
    this.permissions = permissions;
    return this;
  }

  
  @ApiModelProperty(value = "List of permissions for the consent (optional)")
  @JsonProperty("permissions")
  @Valid public List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner> getPermissions() {
    return permissions;
  }

  @JsonProperty("permissions")
  public void setPermissions(List<@Valid SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner> permissions) {
    this.permissions = permissions;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData addPermissionsItem(SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner permissionsItem) {
    if (this.permissions == null) {
      this.permissions = new ArrayList<>();
    }

    this.permissions.add(permissionsItem);
    return this;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData removePermissionsItem(SuccessResponsePopulateConsentAuthorizeScreenDataConsentDataPermissionsInner permissionsItem) {
    if (permissionsItem != null && this.permissions != null) {
      this.permissions.remove(permissionsItem);
    }

    return this;
  }
  /**
   * Initialized accounts for the overall consent (optional)
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData initiatedAccountsForConsent(List<Account> initiatedAccountsForConsent) {
    this.initiatedAccountsForConsent = initiatedAccountsForConsent;
    return this;
  }

  
  @ApiModelProperty(value = "Initialized accounts for the overall consent (optional)")
  @JsonProperty("initiatedAccountsForConsent")
  @Valid public List<@Valid Account> getInitiatedAccountsForConsent() {
    return initiatedAccountsForConsent;
  }

  @JsonProperty("initiatedAccountsForConsent")
  public void setInitiatedAccountsForConsent(List<Account> initiatedAccountsForConsent) {
    this.initiatedAccountsForConsent = initiatedAccountsForConsent;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData addInitiatedAccountsForConsentItem(Account initiatedAccountsForConsentItem) {
    if (this.initiatedAccountsForConsent == null) {
      this.initiatedAccountsForConsent = new ArrayList<>();
    }

    this.initiatedAccountsForConsent.add(initiatedAccountsForConsentItem);
    return this;
  }

  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData removeInitiatedAccountsForConsentItem(Account initiatedAccountsForConsentItem) {
    if (initiatedAccountsForConsentItem != null && this.initiatedAccountsForConsent != null) {
      this.initiatedAccountsForConsent.remove(initiatedAccountsForConsentItem);
    }

    return this;
  }
  /**
   * Indicates if multiple consumer accounts can be selected per consent / permission
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData allowMultipleAccounts(Boolean allowMultipleAccounts) {
    this.allowMultipleAccounts = allowMultipleAccounts;
    return this;
  }

  
  @ApiModelProperty(value = "Indicates if multiple consumer accounts can be selected per consent / permission")
  @JsonProperty("allowMultipleAccounts")
  public Boolean getAllowMultipleAccounts() {
    return allowMultipleAccounts;
  }

  @JsonProperty("allowMultipleAccounts")
  public void setAllowMultipleAccounts(Boolean allowMultipleAccounts) {
    this.allowMultipleAccounts = allowMultipleAccounts;
  }

  /**
   * Indicates if account selection must be handled separately in the UI
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData handleAccountSelectionSeparately(Boolean handleAccountSelectionSeparately) {
    this.handleAccountSelectionSeparately = handleAccountSelectionSeparately;
    return this;
  }

  
  @ApiModelProperty(value = "Indicates if account selection must be handled separately in the UI")
  @JsonProperty("handleAccountSelectionSeparately")
  public Boolean getHandleAccountSelectionSeparately() {
    return handleAccountSelectionSeparately;
  }

  @JsonProperty("handleAccountSelectionSeparately")
  public void setHandleAccountSelectionSeparately(Boolean handleAccountSelectionSeparately) {
    this.handleAccountSelectionSeparately = handleAccountSelectionSeparately;
  }

  /**
   * Indicates if this is a reauthorization flow (optional)
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData isReauthorization(Boolean isReauthorization) {
    this.isReauthorization = isReauthorization;
    return this;
  }

  
  @ApiModelProperty(value = "Indicates if this is a reauthorization flow (optional)")
  @JsonProperty("isReauthorization")
  public Boolean getIsReauthorization() {
    return isReauthorization;
  }

  @JsonProperty("isReauthorization")
  public void setIsReauthorization(Boolean isReauthorization) {
    this.isReauthorization = isReauthorization;
  }

  /**
   * Hidden consent metadata to be forwarded to consent persistence.
   **/
  public SuccessResponsePopulateConsentAuthorizeScreenDataConsentData consentMetadata(Object consentMetadata) {
    this.consentMetadata = consentMetadata;
    return this;
  }

  
  @ApiModelProperty(value = "Hidden consent metadata to be forwarded to consent persistence.")
  @JsonProperty("consentMetadata")
  public Object getConsentMetadata() {
    return consentMetadata;
  }

  @JsonProperty("consentMetadata")
  public void setConsentMetadata(Object consentMetadata) {
    this.consentMetadata = consentMetadata;
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
    SuccessResponsePopulateConsentAuthorizeScreenDataConsentData successResponsePopulateConsentAuthorizeScreenDataConsentData = (SuccessResponsePopulateConsentAuthorizeScreenDataConsentData) o;
    return Objects.equals(this.type, successResponsePopulateConsentAuthorizeScreenDataConsentData.type) &&
        Objects.equals(this.basicConsentData, successResponsePopulateConsentAuthorizeScreenDataConsentData.basicConsentData) &&
        Objects.equals(this.permissions, successResponsePopulateConsentAuthorizeScreenDataConsentData.permissions) &&
        Objects.equals(this.initiatedAccountsForConsent, successResponsePopulateConsentAuthorizeScreenDataConsentData.initiatedAccountsForConsent) &&
        Objects.equals(this.allowMultipleAccounts, successResponsePopulateConsentAuthorizeScreenDataConsentData.allowMultipleAccounts) &&
        Objects.equals(this.handleAccountSelectionSeparately, successResponsePopulateConsentAuthorizeScreenDataConsentData.handleAccountSelectionSeparately) &&
        Objects.equals(this.isReauthorization, successResponsePopulateConsentAuthorizeScreenDataConsentData.isReauthorization) &&
        Objects.equals(this.consentMetadata, successResponsePopulateConsentAuthorizeScreenDataConsentData.consentMetadata) &&
        Objects.equals(this.additionalProperties, successResponsePopulateConsentAuthorizeScreenDataConsentData.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, basicConsentData, permissions, initiatedAccountsForConsent, allowMultipleAccounts, handleAccountSelectionSeparately, isReauthorization, consentMetadata, additionalProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SuccessResponsePopulateConsentAuthorizeScreenDataConsentData {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    basicConsentData: ").append(toIndentedString(basicConsentData)).append("\n");
    sb.append("    permissions: ").append(toIndentedString(permissions)).append("\n");
    sb.append("    initiatedAccountsForConsent: ").append(toIndentedString(initiatedAccountsForConsent)).append("\n");
    sb.append("    allowMultipleAccounts: ").append(toIndentedString(allowMultipleAccounts)).append("\n");
    sb.append("    handleAccountSelectionSeparately: ").append(toIndentedString(handleAccountSelectionSeparately)).append("\n");
    sb.append("    isReauthorization: ").append(toIndentedString(isReauthorization)).append("\n");
    sb.append("    consentMetadata: ").append(toIndentedString(consentMetadata)).append("\n");
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
