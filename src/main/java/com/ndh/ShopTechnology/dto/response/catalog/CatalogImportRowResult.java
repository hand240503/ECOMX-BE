package com.ndh.ShopTechnology.dto.response.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Kết quả upsert một dòng (thương hiệu / danh mục) khi import. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogImportRowResult {

    /** Dòng trong file (1-based, kể cả tiêu đề). */
    private Integer rowNumber;

    /** Khóa nhận diện (code hoặc name) để người dùng dễ dò. */
    private String key;

    /** CREATED | UPDATED | FAILED */
    private String action;

    private boolean success;

    /** ID bản ghi sau khi tạo/cập nhật. */
    private Long id;

    private String message;
}
