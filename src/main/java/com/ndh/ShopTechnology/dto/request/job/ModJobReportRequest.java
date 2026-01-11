package com.ndh.ShopTechnology.dto.request.job;

import com.ndh.ShopTechnology.entities.job.JobReportDetailEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModJobReportRequest {

    private Long userId;
    private String code;
    private Double dayWork;
    private Double dayLeave;
    private Double dayOff;
    private Integer status;

    private Double jobDate1;
    private Double jobDate2;
    private Double jobDate3;
    private Double jobDate4;
    private Double jobDate5;
    private Double jobDate6;
    private Double jobDate7;
    private Double jobDate8;
    private Double jobDate9;
    private Double jobDate10;
    private Double jobDate11;
    private Double jobDate12;
    private Double jobDate13;
    private Double jobDate14;
    private Double jobDate15;
    private Double jobDate16;
    private Double jobDate17;
    private Double jobDate18;
    private Double jobDate19;
    private Double jobDate20;
    private Double jobDate21;
    private Double jobDate22;
    private Double jobDate23;
    private Double jobDate24;
    private Double jobDate25;
    private Double jobDate26;
    private Double jobDate27;
    private Double jobDate28;
    private Double jobDate29;
    private Double jobDate30;
    private Double jobDate31;

    public JobReportDetailEntity toEntity() {
        JobReportDetailEntity entity = new JobReportDetailEntity();



        entity.setJobDate1(getJobDateOrDefault(jobDate1));
        entity.setJobDate2(getJobDateOrDefault(jobDate2));
        entity.setJobDate3(getJobDateOrDefault(jobDate3));
        entity.setJobDate4(getJobDateOrDefault(jobDate4));
        entity.setJobDate5(getJobDateOrDefault(jobDate5));
        entity.setJobDate6(getJobDateOrDefault(jobDate6));
        entity.setJobDate7(getJobDateOrDefault(jobDate7));
        entity.setJobDate8(getJobDateOrDefault(jobDate8));
        entity.setJobDate9(getJobDateOrDefault(jobDate9));
        entity.setJobDate10(getJobDateOrDefault(jobDate10));
        entity.setJobDate11(getJobDateOrDefault(jobDate11));
        entity.setJobDate12(getJobDateOrDefault(jobDate12));
        entity.setJobDate13(getJobDateOrDefault(jobDate13));
        entity.setJobDate14(getJobDateOrDefault(jobDate14));
        entity.setJobDate15(getJobDateOrDefault(jobDate15));
        entity.setJobDate16(getJobDateOrDefault(jobDate16));
        entity.setJobDate17(getJobDateOrDefault(jobDate17));
        entity.setJobDate18(getJobDateOrDefault(jobDate18));
        entity.setJobDate19(getJobDateOrDefault(jobDate19));
        entity.setJobDate20(getJobDateOrDefault(jobDate20));
        entity.setJobDate21(getJobDateOrDefault(jobDate21));
        entity.setJobDate22(getJobDateOrDefault(jobDate22));
        entity.setJobDate23(getJobDateOrDefault(jobDate23));
        entity.setJobDate24(getJobDateOrDefault(jobDate24));
        entity.setJobDate25(getJobDateOrDefault(jobDate25));
        entity.setJobDate26(getJobDateOrDefault(jobDate26));
        entity.setJobDate27(getJobDateOrDefault(jobDate27));
        entity.setJobDate28(getJobDateOrDefault(jobDate28));
        entity.setJobDate29(getJobDateOrDefault(jobDate29));
        entity.setJobDate30(getJobDateOrDefault(jobDate30));
        entity.setJobDate31(getJobDateOrDefault(jobDate31));
        return entity;
    }

    public JobReportDetailEntity toEntity(ModJobReportRequest request, JobReportDetailEntity entity) {


        entity.setJobDate1(getJobDateOrDefault(request.getJobDate1()));
        entity.setJobDate2(getJobDateOrDefault(request.getJobDate2()));
        entity.setJobDate3(getJobDateOrDefault(request.getJobDate3()));
        entity.setJobDate4(getJobDateOrDefault(request.getJobDate4()));
        entity.setJobDate5(getJobDateOrDefault(request.getJobDate5()));
        entity.setJobDate6(getJobDateOrDefault(request.getJobDate6()));
        entity.setJobDate7(getJobDateOrDefault(request.getJobDate7()));
        entity.setJobDate8(getJobDateOrDefault(request.getJobDate8()));
        entity.setJobDate9(getJobDateOrDefault(request.getJobDate9()));
        entity.setJobDate10(getJobDateOrDefault(request.getJobDate10()));
        entity.setJobDate11(getJobDateOrDefault(request.getJobDate11()));
        entity.setJobDate12(getJobDateOrDefault(request.getJobDate12()));
        entity.setJobDate13(getJobDateOrDefault(request.getJobDate13()));
        entity.setJobDate14(getJobDateOrDefault(request.getJobDate14()));
        entity.setJobDate15(getJobDateOrDefault(request.getJobDate15()));
        entity.setJobDate16(getJobDateOrDefault(request.getJobDate16()));
        entity.setJobDate17(getJobDateOrDefault(request.getJobDate17()));
        entity.setJobDate18(getJobDateOrDefault(request.getJobDate18()));
        entity.setJobDate19(getJobDateOrDefault(request.getJobDate19()));
        entity.setJobDate20(getJobDateOrDefault(request.getJobDate20()));
        entity.setJobDate21(getJobDateOrDefault(request.getJobDate21()));
        entity.setJobDate22(getJobDateOrDefault(request.getJobDate22()));
        entity.setJobDate23(getJobDateOrDefault(request.getJobDate23()));
        entity.setJobDate24(getJobDateOrDefault(request.getJobDate24()));
        entity.setJobDate25(getJobDateOrDefault(request.getJobDate25()));
        entity.setJobDate26(getJobDateOrDefault(request.getJobDate26()));
        entity.setJobDate27(getJobDateOrDefault(request.getJobDate27()));
        entity.setJobDate28(getJobDateOrDefault(request.getJobDate28()));
        entity.setJobDate29(getJobDateOrDefault(request.getJobDate29()));
        entity.setJobDate30(getJobDateOrDefault(request.getJobDate30()));
        entity.setJobDate31(getJobDateOrDefault(request.getJobDate31()));

        return entity;
    }
    private Double getJobDateOrDefault(Double jobDate) {
        if (jobDate == null || jobDate < 0 || jobDate > 1) {
            return 0.0;
        }
        return jobDate;
    }

}
