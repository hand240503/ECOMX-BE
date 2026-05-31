package com.ndh.ShopTechnology.dto.response.department;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.entities.department.UserDepartmentEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentMemberResponse {

    @JsonProperty("user_id")
    private Long userId;

    private String username;

    @JsonProperty("full_name")
    private String fullName;

    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    /** LEADER hoặc MEMBER */
    private String position;

    @JsonProperty("assigned_by")
    private String assignedBy;

    public static DepartmentMemberResponse fromEntity(UserDepartmentEntity ud) {
        UserEntity u = ud.getUser();
        String fullName = (u.getUserInfo() != null) ? u.getUserInfo().getFullName() : null;
        return DepartmentMemberResponse.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .fullName(fullName)
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .position(ud.getPosition())
                .assignedBy(ud.getAssignedBy())
                .build();
    }
}
