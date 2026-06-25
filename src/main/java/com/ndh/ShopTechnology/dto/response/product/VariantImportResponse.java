package com.ndh.ShopTechnology.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tổng hợp kết quả import biến thể (phân loại) cho một sản phẩm từ file Excel/CSV/TXT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantImportResponse {

    /** ID sản phẩm được nạp biến thể. */
    private Long productId;

    /** Tổng số biến thể (dòng) phát hiện trong file. */
    private int totalVariants;

    /** Số biến thể xử lý thành công (tạo mới + cập nhật). */
    private int successCount;

    /** Số biến thể sẽ/được THÊM MỚI. */
    private int createdCount;

    /** Số biến thể sẽ/được CẬP NHẬT. */
    private int updatedCount;

    /** Số biến thể BỎ QUA vì không có thay đổi nào. */
    private int skippedCount;

    /** Số biến thể thất bại. */
    private int failureCount;

    /** Chi tiết từng biến thể. */
    @Builder.Default
    private List<VariantImportRowResult> results = new ArrayList<>();
}
