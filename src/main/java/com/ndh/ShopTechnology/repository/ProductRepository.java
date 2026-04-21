package com.ndh.ShopTechnology.repository;

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

  @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
  @Query("SELECT p FROM products p")
  Page<ProductEntity> findPageWithListRelations(Pageable pageable);

  @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
  @Query("SELECT p FROM products p")
  List<ProductEntity> findAllWithListRelations();

  /**
   * Paginated products for one category (exact {@code category.id}, not subtree).
   * No {@code @EntityGraph} on this query: graph + {@code Page} can break counts; relations load in the service tx.
   */
  @Query(
      value = "SELECT p FROM products p WHERE p.category.id = :categoryId",
      countQuery = "SELECT count(p) FROM products p WHERE p.category.id = :categoryId")
  Page<ProductEntity> findPageByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

  /**
   * Products whose {@code category.parent.id} equals {@code parentCategoryId}
   * (direct children of that parent only), matching {@code JOIN category c ON c.id = p.category_id WHERE c.parent_id = :id}.
   */
  @Query(
      value = "SELECT p FROM products p WHERE p.category.parent.id = :parentCategoryId",
      countQuery = "SELECT count(p) FROM products p WHERE p.category.parent.id = :parentCategoryId")
  Page<ProductEntity> findPageByDirectChildCategoriesOf(@Param("parentCategoryId") Long parentCategoryId,
      Pageable pageable);

  @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
  List<ProductEntity> findByIsFeaturedTrue(Pageable pageable);

  @EntityGraph(attributePaths = { "category", "category.parent", "brand" })
  List<ProductEntity> findTopNByOrderBySoldCountDesc(Pageable pageable);

  @Query("SELECT DISTINCT p FROM products p "
      + "LEFT JOIN FETCH p.category c "
      + "LEFT JOIN FETCH c.parent "
      + "LEFT JOIN FETCH p.brand "
      + "LEFT JOIN FETCH p.prices pr "
      + "LEFT JOIN FETCH pr.unit "
      + "WHERE p.id = :id")
  Optional<ProductEntity> findWithFullRelationsById(@Param("id") Long id);

  @Query("SELECT DISTINCT p FROM products p "
      + "LEFT JOIN FETCH p.category c "
      + "LEFT JOIN FETCH c.parent "
      + "LEFT JOIN FETCH p.brand "
      + "LEFT JOIN FETCH p.prices pr "
      + "LEFT JOIN FETCH pr.unit "
      + "WHERE p.id IN :ids")
  List<ProductEntity> findAllWithFullRelationsByIdIn(@Param("ids") Collection<Long> ids);

  Optional<ProductEntity> findByProductName(String productName);

  List<ProductEntity> findByStatus(Integer status);
}
