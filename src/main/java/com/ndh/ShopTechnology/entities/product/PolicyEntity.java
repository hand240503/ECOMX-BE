package com.ndh.ShopTechnology.entities.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "policies")
@Table(name = "policies")
public class PolicyEntity extends BaseEntity {

  public static final String COL_CODE = "code";
  public static final String COL_NAME = "name";
  public static final String COL_POLICY_TYPE = "policy_type";
  public static final String COL_NUMERIC_VALUE = "numeric_value";
  public static final String COL_TEXT_VALUE = "text_value";
  public static final String COL_DETAIL = "detail";
  public static final String COL_ACTIVE = "active";

  @Column(name = COL_CODE, unique = true, length = 64)
  private String code;

  @Column(name = COL_NAME, nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = COL_POLICY_TYPE, nullable = false, length = 40)
  private PolicyType policyType;

  @Column(name = COL_NUMERIC_VALUE)
  private Double numericValue;

  @Column(name = COL_TEXT_VALUE, length = 500)
  private String textValue;

  @Column(name = COL_DETAIL, columnDefinition = "TEXT")
  private String detail;

  @Column(name = COL_ACTIVE)
  @Builder.Default
  private Boolean active = true;

  @ManyToMany(mappedBy = "policies", fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private Set<ProductEntity> products = new HashSet<>();
}
