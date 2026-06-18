package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.BrandEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    long countByBrand_Id(Long brandId);

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

    List<ProductEntity> findByStatus(Integer status);
}
