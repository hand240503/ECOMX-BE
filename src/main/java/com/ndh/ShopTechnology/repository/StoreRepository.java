package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.store.StoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<StoreEntity, Long> {

    Optional<StoreEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    Optional<StoreEntity> findFirstByIsDefaultTrueOrderByIdAsc();

    List<StoreEntity> findByActiveTrueOrderByIdAsc();

    long countByIsDefaultTrue();

    /** Tìm kho theo tên / mã / thành phố (q null = tất cả). */
    @Query("""
            SELECT s FROM store s
            WHERE (:q IS NULL
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(s.city) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY s.id ASC
            """)
    List<StoreEntity> search(@Param("q") String q);
}
