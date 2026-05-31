package com.ndh.ShopTechnology.dto.response.department;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.entities.department.DepartmentEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentResponse {

    private Long id;
    private String name;
    private String description;
    private String color;
    private Integer status;

    @JsonProperty("permission_codes")
    private Set<Integer> permissionCodes;

    @JsonProperty("member_count")
    private Integer memberCount;

    /** Tên của leader (nếu có) */
    @JsonProperty("leader_name")
    private String leaderName;

    /** Chỉ trả khi fetch chi tiết (GET /{id}) */
    private List<DepartmentMemberResponse> members;

    @JsonProperty("created_date")
    private Date createdDate;

    @JsonProperty("modified_date")
    private Date modifiedDate;

    public static DepartmentResponse fromEntity(DepartmentEntity e) {
        return DepartmentResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .color(e.getColor())
                .status(e.getStatus())
                .permissionCodes(e.getPermissionCodes())
                .memberCount(e.getMembers() != null ? e.getMembers().size() : 0)
                .createdDate(e.getCreatedDate())
                .modifiedDate(e.getModifiedDate())
                .build();
    }
}
