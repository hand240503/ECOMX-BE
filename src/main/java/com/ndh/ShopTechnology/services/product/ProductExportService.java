package com.ndh.ShopTechnology.services.product;

/**
 * Xuất toàn bộ dữ liệu sản phẩm (kèm danh mục, thương hiệu, biến thể, giá, đơn vị)
 * ra file Excel theo tên cột trong CSDL.
 */
public interface ProductExportService {

    /** Sinh file Excel chứa mọi sản phẩm — mỗi dòng là một dòng giá (biến thể × đơn vị). */
    byte[] exportProductsXlsx();

    /**
     * Sinh file Excel chỉ gồm sản phẩm CHƯA HOÀN THIỆN: chưa có biến thể nào,
     * hoặc có biến thể nhưng chưa có giá. Dùng để rà soát và bổ sung biến thể/giá.
     */
    byte[] exportIncompleteProductsXlsx();

    /** Sinh file Excel chứa toàn bộ thương hiệu (brands) theo cột CSDL. */
    byte[] exportBrandsXlsx();

    /** Sinh file Excel chứa toàn bộ danh mục (category) theo cột CSDL. */
    byte[] exportCategoriesXlsx();
}
