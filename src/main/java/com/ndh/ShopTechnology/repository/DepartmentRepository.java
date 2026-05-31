package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.department.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    List<DepartmentEntity> findAllByStatusOrderByIdAsc(Integer status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT d FROM DepartmentEntity d LEFT JOIN FETCH d.members m WHERE d.id = :id")
    Optional<DepartmentEntity> findByIdWithMembers(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT d FROM DepartmentEntity d
        JOIN d.members m
        WHERE m.user.id = :userId AND d.status = 1
        """)
    List<DepartmentEntity> findActiveByUserId(@Param("userId") Long userId);
}
