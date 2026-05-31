package com.ndh.ShopTechnology.dto.request.department;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddDepartmentMemberRequest {

    /**
     * Vị trí trong phòng ban: LEADER hoặc MEMBER (default).
     * Mỗi phòng ban chỉ được có 1 LEADER.
     */
    @Pattern(regexp = "^(LEADER|MEMBER)$", message = "position phải là LEADER hoặc MEMBER")
    private String position = "MEMBER";
}
