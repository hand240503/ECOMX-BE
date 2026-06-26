package com.ndh.ShopTechnology.dto.response.recommendation.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Trang dữ liệu đơn giản cho các bảng insights (content + thông tin phân trang).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsPage<T> {

    private List<T> content;
    private int page;
    private int size;
    private long total;
    private int totalPages;

    public static <T> InsightsPage<T> of(List<T> content, int page, int size, long total) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return InsightsPage.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .total(total)
                .totalPages(totalPages)
                .build();
    }
}
