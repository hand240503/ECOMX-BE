package com.ndh.ShopTechnology.dto.request.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateJobReportRequest {

    @NotBlank(message = "ID user cannot be blank")
    private Long userId;

    private Integer month;
    private Integer year;
    private Integer day;
    private Integer type;
}
