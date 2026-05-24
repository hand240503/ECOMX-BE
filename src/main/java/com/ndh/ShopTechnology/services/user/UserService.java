package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.request.user.ChangePasswordRequest;
import com.ndh.ShopTechnology.dto.request.user.ChangeContactRequest;
import com.ndh.ShopTechnology.dto.request.user.AdminModUserInfoRequest;
import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;
import com.ndh.ShopTechnology.dto.response.user.ChangePasswordResponse;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.user.UserEntity;

import org.springframework.data.domain.Page;

public interface UserService {
    UserEntity getCurrentUser();

    /** Nhân viên nội bộ (mọi role trừ CUSTOMER). */
    Page<UserResponse> getStaffUsers(PaginationRequest request);

    UserResponse getStaffUser(Long id);

    UserResponse createStaffUser(CreateUserRequest request);

    UserResponse updateStaffUser(AdminModUserInfoRequest ent);

    /** Chỉ tài khoản role EMPLOYEE. */
    Page<UserResponse> getEmployeeUsers(PaginationRequest request);

    UserResponse getEmployeeUser(Long id);

    UserResponse createEmployeeUser(CreateUserRequest request);

    UserResponse updateEmployeeUser(AdminModUserInfoRequest ent);

    /** Khách hàng (role CUSTOMER), phân trang. */
    Page<UserResponse> getCustomerUsers(PaginationRequest request);

    UserResponse getCustomerUser(Long id);

    /** Toàn bộ user (mọi role), phân trang — quyền {@code READ_USER} / {@code READ_ALL}. */
    Page<UserResponse> getAllUsersForAdmin(PaginationRequest request);

    /** Chi tiết user bất kỳ — quyền {@code READ_USER} / {@code READ_ALL}. */
    UserResponse getUserForAdmin(Long id);

    /** Cập nhật user bất kỳ (CUSTOMER và nội bộ). */
    UserResponse updateUserForAdmin(AdminModUserInfoRequest req);

    void deleteUserForAdmin(Long id);

    UserResponse resetUserPasswordForAdmin(Long id);

    void deleteStaffUser(Long id);

    /** Đặt lại mật khẩu 6 chữ số (5 số đầu SĐT + 1 số ngẫu nhiên); trả về {@link UserResponse#getTemporaryPassword()} một lần. */
    UserResponse resetStaffPassword(Long id);

    UserResponse getProfile();

    UserResponse updateProfileInfo(ModUserInfoRequest ent);

    ChangePasswordResponse changePassword(ChangePasswordRequest request);

    LoginResponse changeContact(ChangeContactRequest request);
}
