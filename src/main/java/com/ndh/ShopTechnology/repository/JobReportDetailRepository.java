package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.job.JobReportDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobReportDetailRepository extends JpaRepository<JobReportDetailEntity, Long> {

    JobReportDetailEntity findByJobReportId(Long id);
}
