package com.ndh.ShopTechnology.dto.response.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Tổng hợp kết quả import/upsert thương hiệu hoặc danh mục. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogImportResponse {

    private int totalRows;
    private int createdCount;
    private int updatedCount;
    /** Số dòng bị bỏ qua vì dữ liệu không thay đổi (giống hệt bản ghi hiện có). */
    private int skippedCount;
    private int failureCount;

    @Builder.Default
    private List<CatalogImportRowResult> results = new ArrayList<>();
}
