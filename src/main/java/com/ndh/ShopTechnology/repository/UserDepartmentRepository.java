package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.department.UserDepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartmentEntity, Long> {

    boolean existsByUser_IdAndDepartment_Id(Long userId, Long departmentId);

    boolean existsByDepartment_IdAndPosition(Long departmentId, String position);

    Optional<UserDepartmentEntity> findByUser_IdAndDepartment_Id(Long userId, Long departmentId);

    @Query("SELECT ud FROM UserDepartmentEntity ud JOIN FETCH ud.user u WHERE ud.department.id = :deptId")
    List<UserDepartmentEntity> findByDepartmentIdWithUser(@Param("deptId") Long deptId);

    void deleteByUser_IdAndDepartment_Id(Long userId, Long departmentId);
}
