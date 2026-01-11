package com.ndh.ShopTechnology.services.job.impl;

import com.ndh.ShopTechnology.constant.SystemConstant;
import com.ndh.ShopTechnology.dto.request.job.CreateJobReportRequest;
import com.ndh.ShopTechnology.dto.request.job.GetJobReportRequest;
import com.ndh.ShopTechnology.dto.request.job.ModJobReportRequest;
import com.ndh.ShopTechnology.entities.job.JobReportDetailEntity;
import com.ndh.ShopTechnology.entities.job.JobReportEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.JobReportDetailRepository;
import com.ndh.ShopTechnology.repository.JobReportRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.job.JobReportService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
public class JobReportServiceImpl implements JobReportService {
    private final UserRepository userRepository;
    private final JobReportRepository jobReportRepository;
    private final JobReportDetailRepository jobReportDetailRepository;

    public JobReportServiceImpl(UserRepository userRepository, JobReportRepository jobReportRepository, JobReportDetailRepository jobReportDetailRepository) {
        this.userRepository = userRepository;
        this.jobReportRepository = jobReportRepository;
        this.jobReportDetailRepository = jobReportDetailRepository;
    }


    public JobReportEntity createReportForUser(CreateJobReportRequest request) {

        if (request.getUserId() == null) return null;

        Optional<UserEntity> user = userRepository.findById(request.getUserId());
        if (user.isEmpty()) return null;

        Long uId = user.get().getId();
        JobReportEntity jobReport = new JobReportEntity();
        jobReport.setUser(user.get());

        LocalDate today = LocalDate.now();
        int day = request.getDay() != null ? request.getDay() : today.getDayOfMonth();
        int month = request.getMonth() != null ? request.getMonth() : today.getMonthValue();
        int year = request.getYear() != null ? request.getYear() : today.getYear();
        int type = request.getType() != null ? request.getType() : 0;

        String code = year + "-" + month;

        if (jobReportRepository.findByUserIdAndCode(uId, code) != null) return null;

        double dayRequired = getRemainingDaysInMonth(type, month, year, day);
        jobReport.setDayRequired(dayRequired);

        jobReport.setCode(code);
        jobReport.setDayWorked(0.0);
        jobReport.setDayOff(0.0);
        jobReport.setDayLeave(0.0);
        jobReport.setStatus(SystemConstant.INACTIVE_STATUS);

        jobReport = jobReportRepository.save(jobReport);

        ModJobReportRequest jobDetailsRequest = new ModJobReportRequest();
        JobReportDetailEntity jobReportDetail = jobDetailsRequest.toEntity();
        jobReportDetail.setJobReport(jobReport);
        jobReportDetailRepository.save(jobReportDetail);

        return jobReport;
    }


    private static int getDaysInMonth(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.lengthOfMonth();
    }

    private double getRemainingDaysInMonth(int type, int month, int year, int startDay) {

        int daysInMonth = getDaysInMonth(month, year);
        double count = 0.0;

        for (int i = startDay; i <= daysInMonth; i++) {
            LocalDate date = LocalDate.of(year, month, i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            if (type == 0) {
                if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                    count++;
                }

            } else if (type == 1) {
                if (dayOfWeek != DayOfWeek.SUNDAY) {
                    count++;
                }
            } else if (type == 2) {
                count++;
            }
        }

        return count;
    }

    @Override
    public JobReportDetailEntity modJobReportDetails(ModJobReportRequest request) {
        JobReportEntity jobEnt = jobReportRepository.findByUserIdAndCode(request.getUserId(), request.getCode());

        if (jobEnt == null) return null;

        JobReportDetailEntity entity = jobReportDetailRepository.findByJobReportId(jobEnt.getId());
        entity = request.toEntity(request, entity);
        if(request.getStatus() != null) jobEnt.setStatus(request.getStatus());

        jobEnt.setDayWorked(request.getDayWork());
        jobEnt.setDayOff(request.getDayOff());

        entity.setJobReport(jobEnt);

        jobReportRepository.save(jobEnt);
        return jobReportDetailRepository.save(entity);
    }



    @Override
    public  JobReportDetailEntity getJobReportForUser(GetJobReportRequest request) {

        JobReportEntity jobReport = jobReportRepository.findByUserIdAndCode(request.getUserId(),request.getCode());
        if(jobReport == null) return null;

        return jobReportDetailRepository.findByJobReportId(jobReport.getId());
    }
}
