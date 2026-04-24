package com.ndh.ShopTechnology.dto.request.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderReturnRequest {

    @Size(max = 2000, message = "reason must be at most 2000 characters")
    private String reason;
}
