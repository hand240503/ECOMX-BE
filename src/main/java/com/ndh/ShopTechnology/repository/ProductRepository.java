package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    long countByBrand_Id(Long brandId);

    /** Số sản phẩm đang thuộc các danh mục này (để cảnh báo trước khi xóa danh mục). */
    long countByCategory_IdIn(Collection<Long> categoryIds);

    /** Gỡ danh mục khỏi mọi sản phẩm thuộc các danh mục này (set category = null) — dùng khi xóa danh mục. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE products p SET p.category = null WHERE p.category.id IN :categoryIds")
    int clearCategoryForCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);

    /** Gỡ thương hiệu khỏi mọi sản phẩm thuộc các thương hiệu này (set brand = null) — dùng khi xóa thương hiệu. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE products p SET p.brand = null WHERE p.brand.id IN :brandIds")
    int clearBrandForBrandIds(@Param("brandIds") Collection<Long> brandIds);

    @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
    @Query("SELECT p FROM products p")
    Page<ProductEntity> findPageWithListRelations(Pageable pageable);

    @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
    @Query("SELECT p FROM products p")
    List<ProductEntity> findAllWithListRelations();

    @Query(value = "SELECT p FROM products p WHERE p.category.id = :categoryId", countQuery = "SELECT count(p) FROM products p WHERE p.category.id = :categoryId")
    Page<ProductEntity> findPageByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = "SELECT p FROM products p WHERE p.category.parent.id = :parentCategoryId", countQuery = "SELECT count(p) FROM products p WHERE p.category.parent.id = :parentCategoryId")
    Page<ProductEntity> findPageByDirectChildCategoriesOf(@Param("parentCategoryId") Long parentCategoryId,
            Pageable pageable);

    @Query(value = "SELECT p FROM products p WHERE p.category.id = :categoryId AND p.brand.id IN :brandIds", countQuery = "SELECT count(p) FROM products p WHERE p.category.id = :categoryId AND p.brand.id IN :brandIds")
    Page<ProductEntity> findPageByCategoryIdAndBrandIds(@Param("categoryId") Long categoryId,
            @Param("brandIds") Collection<Long> brandIds, Pageable pageable);

    @Query(value = "SELECT p FROM products p WHERE p.category.parent.id = :parentCategoryId AND p.brand.id IN :brandIds", countQuery = "SELECT count(p) FROM products p WHERE p.category.parent.id = :parentCategoryId AND p.brand.id IN :brandIds")
    Page<ProductEntity> findPageByDirectChildCategoriesOfAndBrandIds(
            @Param("parentCategoryId") Long parentCategoryId,
            @Param("brandIds") Collection<Long> brandIds, Pageable pageable);

    /** Distinct brands có sản phẩm trực tiếp trong category (lá). */
    @Query("SELECT DISTINCT p.brand FROM products p WHERE p.category.id = :categoryId AND p.brand IS NOT NULL ORDER BY p.brand.name ASC")
    List<BrandEntity> findDistinctBrandsByCategoryId(@Param("categoryId") Long categoryId);

    /** Distinct brands có sản phẩm trong các danh mục con trực tiếp của parent. */
    @Query("SELECT DISTINCT p.brand FROM products p WHERE p.category.parent.id = :parentCategoryId AND p.brand IS NOT NULL ORDER BY p.brand.name ASC")
    List<BrandEntity> findDistinctBrandsByDirectChildCategoriesOf(@Param("parentCategoryId") Long parentCategoryId);

    // --- Subtree (category + toàn bộ con/cháu ở mọi cấp) ---

    @Query(value = "SELECT p FROM products p WHERE p.category.id IN :categoryIds", countQuery = "SELECT count(p) FROM products p WHERE p.category.id IN :categoryIds")
    Page<ProductEntity> findPageByCategoryIdIn(@Param("categoryIds") Collection<Long> categoryIds, Pageable pageable);

    @Query(value = "SELECT p FROM products p WHERE p.category.id IN :categoryIds AND p.brand.id IN :brandIds", countQuery = "SELECT count(p) FROM products p WHERE p.category.id IN :categoryIds AND p.brand.id IN :brandIds")
    Page<ProductEntity> findPageByCategoryIdInAndBrandIds(@Param("categoryIds") Collection<Long> categoryIds,
            @Param("brandIds") Collection<Long> brandIds, Pageable pageable);

    /** Distinct brands có sản phẩm trong toàn bộ subtree danh mục. */
    @Query("SELECT DISTINCT p.brand FROM products p WHERE p.category.id IN :categoryIds AND p.brand IS NOT NULL ORDER BY p.brand.name ASC")
    List<BrandEntity> findDistinctBrandsByCategoryIdIn(@Param("categoryIds") Collection<Long> categoryIds);

    @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
    List<ProductEntity> findByIsFeaturedTrue(Pageable pageable);

    @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
    List<ProductEntity> findByIsFeaturedTrueOrderByIdDesc();

    @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
    List<ProductEntity> findByHotSaleTrue(Pageable pageable);

    @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
    List<ProductEntity> findByHotSaleTrueOrderByIdDesc();

    @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
    List<ProductEntity> findTopNByOrderBySoldCountDesc(Pageable pageable);

    @Query("SELECT DISTINCT p FROM products p "
            + "LEFT JOIN FETCH p.category c "
            + "LEFT JOIN FETCH c.parent "
            + "LEFT JOIN FETCH p.brand "
            + "LEFT JOIN FETCH p.variants v "
            + "WHERE p.id = :id")
    Optional<ProductEntity> findWithFullRelationsById(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM products p "
            + "LEFT JOIN FETCH p.category c "
            + "LEFT JOIN FETCH c.parent "
            + "LEFT JOIN FETCH p.brand "
            + "LEFT JOIN FETCH p.variants v "
            + "WHERE p.id IN :ids")
    List<ProductEntity> findAllWithFullRelationsByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "brand")
    @Query("SELECT p FROM products p WHERE p.id IN :ids")
    List<ProductEntity> findAllWithBrandByIdIn(@Param("ids") Collection<Long> ids);

    Optional<ProductEntity> findByProductName(String productName);

    /** Tra sản phẩm theo SKU (khóa nghiệp vụ) — dùng để phát hiện upsert khi import. */
    Optional<ProductEntity> findFirstBySku(Long sku);

    /** Tra sản phẩm theo tên không phân biệt hoa thường — dùng khi không có SKU. */
    Optional<ProductEntity> findFirstByProductNameIgnoreCase(String productName);

    List<ProductEntity> findByStatus(Integer status);

    /** Cộng số lượng đã bán (khi đơn hoàn thành). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE products p SET p.soldCount = COALESCE(p.soldCount, 0) + :qty WHERE p.id = :id")
    int incrementSoldCount(@Param("id") Long id, @Param("qty") long qty);

    /** Trừ số lượng đã bán (khi đơn được hoàn tiền), không cho âm. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE products p SET p.soldCount = CASE WHEN COALESCE(p.soldCount, 0) >= :qty "
            + "THEN COALESCE(p.soldCount, 0) - :qty ELSE 0 END WHERE p.id = :id")
    int decrementSoldCount(@Param("id") Long id, @Param("qty") long qty);
}
