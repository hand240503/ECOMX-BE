package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

  List<CategoryEntity> findByParentIsNull();

  List<CategoryEntity> findByParentId(Long parentId);

  boolean existsByCode(String code);

  Optional<CategoryEntity> findByCode(String code);
}
