package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

  @EntityGraph(attributePaths = {"category", "category.parent", "brand"})
  @Query("SELECT p FROM products p")
  Page<ProductEntity> findPageWithListRelations(Pageable pageable);

  @EntityGraph(attributePaths = {"category", "category.parent", "brand"})
  @Query("SELECT p FROM products p")
  List<ProductEntity> findAllWithListRelations();

  @EntityGraph(attributePaths = {"category", "category.parent", "brand"})
  List<ProductEntity> findByCategoryId(Long categoryId);

  @EntityGraph(attributePaths = {"category", "category.parent", "brand"})
  List<ProductEntity> findByIsFeaturedTrue(Pageable pageable);

  @EntityGraph(attributePaths = {"category", "category.parent", "brand"})
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
