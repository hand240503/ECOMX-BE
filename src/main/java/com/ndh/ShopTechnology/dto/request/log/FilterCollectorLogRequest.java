package com.ndh.ShopTechnology.dto.request.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterCollectorLogRequest {

    private Long userId;

    private Long productId;

    private String event;

    private String sessionId;

    private Date startDate;

    private Date endDate;

    private Integer page;

    private Integer size;
}
