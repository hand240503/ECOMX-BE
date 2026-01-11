package com.ndh.ShopTechnology.controller.job;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.dto.request.job.CreateJobReportRequest;
import com.ndh.ShopTechnology.dto.request.job.GetJobReportRequest;
import com.ndh.ShopTechnology.dto.request.job.ModJobReportRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.ErrorResponse;
import com.ndh.ShopTechnology.entities.job.JobReportDetailEntity;
import com.ndh.ShopTechnology.entities.job.JobReportEntity;
import com.ndh.ShopTechnology.services.job.JobReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/job")
public class JobReportController {

    private final JobReportService jobReportService;

    @Autowired
    public JobReportController(JobReportService jobReportService) {
        this.jobReportService = jobReportService;
    }

    @PostMapping
    public ResponseEntity<APIResponse<JobReportEntity>> createJobReport(
            @RequestBody CreateJobReportRequest request) throws Exception {

        JobReportEntity entity = jobReportService.createReportForUser(request);

        if (entity == null) {
            APIResponse<JobReportEntity> response = APIResponse.of(
                    false,
                    MessageConstant.USER_NOT_FOUND,
                    null,
                    List.of(ErrorResponse.builder()
                            .field("userId")
                            .message(MessageConstant.USER_NOT_FOUND)
                            .build()),
                    null
            );
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(response);
        }

        APIResponse<JobReportEntity> response = APIResponse.of(
                true,
                "Job report created successfully",
                entity,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping
    public ResponseEntity<APIResponse<JobReportDetailEntity>> modifyJobReport(
            @RequestBody ModJobReportRequest request) throws Exception {

        JobReportDetailEntity entity = jobReportService.modJobReportDetails(request);

        APIResponse<JobReportDetailEntity> response = APIResponse.of(
                true,
                "Job report modified successfully",
                entity,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/details")
    public ResponseEntity<APIResponse<JobReportDetailEntity>> getJobReportForUser(
            @RequestBody GetJobReportRequest request) throws Exception {

        JobReportDetailEntity entity = jobReportService.getJobReportForUser(request);

        APIResponse<JobReportDetailEntity> response = APIResponse.of(
                true,
                "Job report retrieved successfully",
                entity,
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}