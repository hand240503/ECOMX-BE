package com.ndh.ShopTechnology.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    public static final String COL_CREATED_DATE   = "created_date";
    public static final String COL_MODIFIED_DATE  = "modified_date";
    public static final String COL_CREATED_BY     = "created_by";
    public static final String COL_MODIFIED_BY    = "modified_by";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = COL_CREATED_DATE)
    @CreatedDate
    private Date createdDate;

    @Column(name = COL_MODIFIED_DATE)
    @LastModifiedDate
    private Date modifiedDate;

    @Column(name = COL_CREATED_BY)
    @CreatedBy
    private String createdBy;

    @Column(name = COL_MODIFIED_BY)
    @LastModifiedBy
    private String modifiedBy;
}
