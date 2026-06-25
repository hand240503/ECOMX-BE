package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

  List<CategoryEntity> findByParentIsNull();

  Page<CategoryEntity> findByParentIsNull(Pageable pageable);

  List<CategoryEntity> findByParentId(Long parentId);

  boolean existsByParent_Id(Long parentId);

  boolean existsByCode(String code);

  Optional<CategoryEntity> findByCode(String code);

  Optional<CategoryEntity> findFirstByCodeIgnoreCase(String code);

  Optional<CategoryEntity> findFirstByNameIgnoreCase(String name);

  boolean existsByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

  /** Đưa danh mục con của các danh mục này lên gốc (parent = null) — dùng khi xóa danh mục cha. */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE category c SET c.parent = null WHERE c.parent.id IN :parentIds")
  int detachChildrenOfParents(@Param("parentIds") Collection<Long> parentIds);
}
