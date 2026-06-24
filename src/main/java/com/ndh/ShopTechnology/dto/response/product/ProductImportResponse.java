package com.ndh.ShopTechnology.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Tổng hợp kết quả import sản phẩm hàng loạt từ file Excel/CSV/TXT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportResponse {

    /** Tổng số sản phẩm (nhóm dòng) phát hiện trong file. */
    private int totalProducts;

    /** Số sản phẩm xử lý thành công (tạo mới + cập nhật). */
    private int successCount;

    /** Số sản phẩm sẽ/được THÊM MỚI. */
    private int createdCount;

    /** Số sản phẩm sẽ/được CẬP NHẬT. */
    private int updatedCount;

    /** Số sản phẩm BỎ QUA vì không có thay đổi nào so với dữ liệu hiện có. */
    private int skippedCount;

    /** Số sản phẩm thất bại. */
    private int failureCount;

    /** Tổng số biến thể đã tạo. */
    private int createdVariantCount;

    /** Chi tiết từng sản phẩm. */
    @Builder.Default
    private List<ProductImportRowResult> results = new ArrayList<>();
}
