package com.ndh.ShopTechnology.entities.job;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "job_report_detail")
public class JobReportDetailEntity extends BaseEntity {

    public static final String COL_JOB_REPORT_ID    =   "job_report_id";
    public static final String COL_JOB_DATE_1       =   "job_date_1";
    public static final String COL_JOB_DATE_2       =   "job_date_2";
    public static final String COL_JOB_DATE_3       =   "job_date_3";
    public static final String COL_JOB_DATE_4       =   "job_date_4";
    public static final String COL_JOB_DATE_5       =   "job_date_5";
    public static final String COL_JOB_DATE_6       =   "job_date_6";
    public static final String COL_JOB_DATE_7       =   "job_date_7";
    public static final String COL_JOB_DATE_8       =   "job_date_8";
    public static final String COL_JOB_DATE_9       =   "job_date_9";
    public static final String COL_JOB_DATE_10      =   "job_date_10";
    public static final String COL_JOB_DATE_11      =   "job_date_11";
    public static final String COL_JOB_DATE_12      =   "job_date_12";
    public static final String COL_JOB_DATE_13      =   "job_date_13";
    public static final String COL_JOB_DATE_14      =   "job_date_14";
    public static final String COL_JOB_DATE_15      =   "job_date_15";
    public static final String COL_JOB_DATE_16      =   "job_date_16";
    public static final String COL_JOB_DATE_17      =   "job_date_17";
    public static final String COL_JOB_DATE_18      =   "job_date_18";
    public static final String COL_JOB_DATE_19      =   "job_date_19";
    public static final String COL_JOB_DATE_20      =   "job_date_20";
    public static final String COL_JOB_DATE_21      =   "job_date_21";
    public static final String COL_JOB_DATE_22      =   "job_date_22";
    public static final String COL_JOB_DATE_23      =   "job_date_23";
    public static final String COL_JOB_DATE_24      =   "job_date_24";
    public static final String COL_JOB_DATE_25      =   "job_date_25";
    public static final String COL_JOB_DATE_26      =   "job_date_26";
    public static final String COL_JOB_DATE_27      =   "job_date_27";
    public static final String COL_JOB_DATE_28      =   "job_date_28";
    public static final String COL_JOB_DATE_29      =   "job_date_29";
    public static final String COL_JOB_DATE_30      =   "job_date_30";
    public static final String COL_JOB_DATE_31      =   "job_date_31";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_JOB_REPORT_ID)
    private JobReportEntity jobReport;

    @Column(name = COL_JOB_DATE_1, nullable = true)
    private Double jobDate1;

    @Column(name = COL_JOB_DATE_2, nullable = true)
    private Double jobDate2;

    @Column(name = COL_JOB_DATE_3, nullable = true)
    private Double jobDate3;

    @Column(name = COL_JOB_DATE_4, nullable = true)
    private Double jobDate4;

    @Column(name = COL_JOB_DATE_5, nullable = true)
    private Double jobDate5;

    @Column(name = COL_JOB_DATE_6, nullable = true)
    private Double jobDate6;

    @Column(name = COL_JOB_DATE_7, nullable = true)
    private Double jobDate7;

    @Column(name = COL_JOB_DATE_8, nullable = true)
    private Double jobDate8;

    @Column(name = COL_JOB_DATE_9, nullable = true)
    private Double jobDate9;

    @Column(name = COL_JOB_DATE_10, nullable = true)
    private Double jobDate10;

    @Column(name = COL_JOB_DATE_11, nullable = true)
    private Double jobDate11;

    @Column(name = COL_JOB_DATE_12, nullable = true)
    private Double jobDate12;

    @Column(name = COL_JOB_DATE_13, nullable = true)
    private Double jobDate13;

    @Column(name = COL_JOB_DATE_14, nullable = true)
    private Double jobDate14;

    @Column(name = COL_JOB_DATE_15, nullable = true)
    private Double jobDate15;

    @Column(name = COL_JOB_DATE_16, nullable = true)
    private Double jobDate16;

    @Column(name = COL_JOB_DATE_17, nullable = true)
    private Double jobDate17;

    @Column(name = COL_JOB_DATE_18, nullable = true)
    private Double jobDate18;

    @Column(name = COL_JOB_DATE_19, nullable = true)
    private Double jobDate19;

    @Column(name = COL_JOB_DATE_20, nullable = true)
    private Double jobDate20;

    @Column(name = COL_JOB_DATE_21, nullable = true)
    private Double jobDate21;

    @Column(name = COL_JOB_DATE_22, nullable = true)
    private Double jobDate22;

    @Column(name = COL_JOB_DATE_23, nullable = true)
    private Double jobDate23;

    @Column(name = COL_JOB_DATE_24, nullable = true)
    private Double jobDate24;

    @Column(name = COL_JOB_DATE_25, nullable = true)
    private Double jobDate25;

    @Column(name = COL_JOB_DATE_26, nullable = true)
    private Double jobDate26;

    @Column(name = COL_JOB_DATE_27, nullable = true)
    private Double jobDate27;

    @Column(name = COL_JOB_DATE_28, nullable = true)
    private Double jobDate28;

    @Column(name = COL_JOB_DATE_29, nullable = true)
    private Double jobDate29;

    @Column(name = COL_JOB_DATE_30, nullable = true)
    private Double jobDate30;

    @Column(name = COL_JOB_DATE_31, nullable = true)
    private Double jobDate31;
}
