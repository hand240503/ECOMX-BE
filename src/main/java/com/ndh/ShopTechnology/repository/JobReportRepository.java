package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.job.JobReportEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobReportRepository extends JpaRepository<JobReportEntity, Long> {

    JobReportEntity findByUserId(Long id);
    JobReportEntity findByCode(String code);

    JobReportEntity findByUserIdAndCode(Long userId, String code);
}
