package com.ndh.ShopTechnology.entities.job;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "job_report_resume")
public class JobReportResumeEntity extends BaseEntity {

    public static final String COL_JOB_REPORT_DETAIL_ID = "job_report_detail_id";
    public static final String COL_JOB_DAY_1            = "job_day_1";
    public static final String COL_JOB_DAY_2            = "job_day_2";
    public static final String COL_JOB_DAY_3            = "job_day_3";
    public static final String COL_JOB_DAY_4            = "job_day_4";
    public static final String COL_JOB_DAY_5            = "job_day_5";
    public static final String COL_JOB_DAY_6            = "job_day_6";
    public static final String COL_JOB_DAY_7            = "job_day_7";
    public static final String COL_JOB_DAY_8            = "job_day_8";
    public static final String COL_JOB_DAY_9            = "job_day_9";
    public static final String COL_JOB_DAY_10           = "job_day_10";
    public static final String COL_JOB_DAY_11           = "job_day_11";
    public static final String COL_JOB_DAY_12           = "job_day_12";
    public static final String COL_JOB_DAY_13           = "job_day_13";
    public static final String COL_JOB_DAY_14           = "job_day_14";
    public static final String COL_JOB_DAY_15           = "job_day_15";
    public static final String COL_JOB_DAY_16           = "job_day_16";
    public static final String COL_JOB_DAY_17           = "job_day_17";
    public static final String COL_JOB_DAY_18           = "job_day_18";
    public static final String COL_JOB_DAY_19           = "job_day_19";
    public static final String COL_JOB_DAY_20           = "job_day_20";
    public static final String COL_JOB_DAY_21           = "job_day_21";
    public static final String COL_JOB_DAY_22           = "job_day_22";
    public static final String COL_JOB_DAY_23           = "job_day_23";
    public static final String COL_JOB_DAY_24           = "job_day_24";
    public static final String COL_JOB_DAY_25           = "job_day_25";
    public static final String COL_JOB_DAY_26           = "job_day_26";
    public static final String COL_JOB_DAY_27           = "job_day_27";
    public static final String COL_JOB_DAY_28           = "job_day_28";
    public static final String COL_JOB_DAY_29           = "job_day_29";
    public static final String COL_JOB_DAY_30           = "job_day_30";
    public static final String COL_JOB_DAY_31           = "job_day_31";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_JOB_REPORT_DETAIL_ID)
    private JobReportDetailEntity jobReportDetail;

    @Column(name = COL_JOB_DAY_1, nullable = true)
    private Double jobDay1;

    @Column(name = COL_JOB_DAY_2, nullable = true)
    private Double jobDay2;

    @Column(name = COL_JOB_DAY_3, nullable = true)
    private Double jobDay3;

    @Column(name = COL_JOB_DAY_4, nullable = true)
    private Double jobDay4;

    @Column(name = COL_JOB_DAY_5, nullable = true)
    private Double jobDay5;

    @Column(name = COL_JOB_DAY_6, nullable = true)
    private Double jobDay6;

    @Column(name = COL_JOB_DAY_7, nullable = true)
    private Double jobDay7;

    @Column(name = COL_JOB_DAY_8, nullable = true)
    private Double jobDay8;

    @Column(name = COL_JOB_DAY_9, nullable = true)
    private Double jobDay9;

    @Column(name = COL_JOB_DAY_10, nullable = true)
    private Double jobDay10;

    @Column(name = COL_JOB_DAY_11, nullable = true)
    private Double jobDay11;

    @Column(name = COL_JOB_DAY_12, nullable = true)
    private Double jobDay12;

    @Column(name = COL_JOB_DAY_13, nullable = true)
    private Double jobDay13;

    @Column(name = COL_JOB_DAY_14, nullable = true)
    private Double jobDay14;

    @Column(name = COL_JOB_DAY_15, nullable = true)
    private Double jobDay15;

    @Column(name = COL_JOB_DAY_16, nullable = true)
    private Double jobDay16;

    @Column(name = COL_JOB_DAY_17, nullable = true)
    private Double jobDay17;

    @Column(name = COL_JOB_DAY_18, nullable = true)
    private Double jobDay18;

    @Column(name = COL_JOB_DAY_19, nullable = true)
    private Double jobDay19;

    @Column(name = COL_JOB_DAY_20, nullable = true)
    private Double jobDay20;

    @Column(name = COL_JOB_DAY_21, nullable = true)
    private Double jobDay21;

    @Column(name = COL_JOB_DAY_22, nullable = true)
    private Double jobDay22;

    @Column(name = COL_JOB_DAY_23, nullable = true)
    private Double jobDay23;

    @Column(name = COL_JOB_DAY_24, nullable = true)
    private Double jobDay24;

    @Column(name = COL_JOB_DAY_25, nullable = true)
    private Double jobDay25;

    @Column(name = COL_JOB_DAY_26, nullable = true)
    private Double jobDay26;

    @Column(name = COL_JOB_DAY_27, nullable = true)
    private Double jobDay27;

    @Column(name = COL_JOB_DAY_28, nullable = true)
    private Double jobDay28;

    @Column(name = COL_JOB_DAY_29, nullable = true)
    private Double jobDay29;

    @Column(name = COL_JOB_DAY_30, nullable = true)
    private Double jobDay30;

    @Column(name = COL_JOB_DAY_31, nullable = true)
    private Double jobDay31;
}
