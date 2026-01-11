package com.ndh.ShopTechnology.services.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ndh.ShopTechnology.dto.request.job.CreateJobReportRequest;
import com.ndh.ShopTechnology.dto.request.job.GetJobReportRequest;
import com.ndh.ShopTechnology.dto.request.job.ModJobReportRequest;
import com.ndh.ShopTechnology.entities.job.JobReportDetailEntity;
import com.ndh.ShopTechnology.entities.job.JobReportEntity;

public interface JobReportService {

    JobReportEntity createReportForUser(CreateJobReportRequest request);

    JobReportDetailEntity modJobReportDetails(ModJobReportRequest request) throws JsonProcessingException;

    JobReportDetailEntity getJobReportForUser(GetJobReportRequest request);
}
