package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.product.PolicyEntity;
import com.ndh.ShopTechnology.entities.product.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyResponse {

  private Long id;
  private String code;
  private String name;
  private PolicyType policyType;
  private Double numericValue;
  private String textValue;
  private String detail;
  private Boolean active;

  public static PolicyResponse fromEntity(PolicyEntity entity) {
    if (entity == null) {
      return null;
    }
    return PolicyResponse.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .policyType(entity.getPolicyType())
        .numericValue(entity.getNumericValue())
        .textValue(entity.getTextValue())
        .detail(entity.getDetail())
        .active(entity.getActive())
        .build();
  }
}
