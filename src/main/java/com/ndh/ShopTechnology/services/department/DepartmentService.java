package com.ndh.ShopTechnology.services.department;

import com.ndh.ShopTechnology.dto.request.department.CreateDepartmentRequest;
import com.ndh.ShopTechnology.dto.response.department.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResponse> listAll();
    DepartmentResponse getById(Long id);
    DepartmentResponse create(CreateDepartmentRequest request);
    DepartmentResponse update(Long id, CreateDepartmentRequest request);
    void delete(Long id);
    DepartmentResponse addMember(Long departmentId, Long userId, String position);
    void removeMember(Long departmentId, Long userId);
    /** Lấy phòng ban của chính user hiện tại (staff thường chỉ xem được đây). */
    List<DepartmentResponse> getMyDepartments();
}
