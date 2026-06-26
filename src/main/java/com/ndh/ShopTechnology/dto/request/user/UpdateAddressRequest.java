package com.ndh.ShopTechnology.dto.request.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateAddressRequest {

  private String addressLine;
  private String city;
  private String state;
  private String country;
  private String zipCode;
  private Boolean isDefault;

  /** Kho (store) User đang chọn — tính lại phí ship từ kho này. Null = không đổi kho. */
  private Long storeId;
}
