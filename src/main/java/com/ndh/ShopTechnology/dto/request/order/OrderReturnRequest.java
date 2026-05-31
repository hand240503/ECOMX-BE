package com.ndh.ShopTechnology.dto.request.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
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

    /** "BANK_TRANSFER" hoặc "WALLET" */
    @Size(max = 50)
    private String refundMethod;

    @Size(max = 50)
    private String bankAccountNumber;

    @Size(max = 100)
    private String bankName;

    @Email
    @Size(max = 200)
    private String bankEmail;
}
