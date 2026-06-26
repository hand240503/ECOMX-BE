package com.ndh.ShopTechnology.dto.response.recommendation.insights;

import lombok.*;

/** Thống kê số lượt mỗi loại sự kiện (event) trong collector_log. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventStatDto {
    private String event;
    private long count;
}
