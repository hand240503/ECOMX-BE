package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePriceRequest {

  @NotNull(message = "Unit ID is required")
  @JsonProperty("unit_id")
  private Long unitId;

  @NotNull(message = "Current value is required")
  @JsonProperty("current_value")
  private Double currentValue;

  @JsonProperty("old_value")
  private Double oldValue;
}
