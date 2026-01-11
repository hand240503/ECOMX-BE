package com.ndh.ShopTechnology.dto.request.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetJobReportRequest {
    public String code;
    public Long userId;
}
