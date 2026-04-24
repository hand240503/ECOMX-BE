package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.order.PaymentMethodEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodResponse {

    private Long id;
    private String name;
    private String code;
    private Integer sortOrder;

    public static PaymentMethodResponse fromEntity(PaymentMethodEntity e) {
        if (e == null) {
            return null;
        }
        return PaymentMethodResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .code(e.getCode())
                .sortOrder(e.getSortOrder())
                .build();
    }
}
