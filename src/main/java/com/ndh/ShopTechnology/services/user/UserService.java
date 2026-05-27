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

    Page<UserResponse> getStaffUsers(PaginationRequest request);

    UserResponse getStaffUser(Long id);

    UserResponse createStaffUser(CreateUserRequest request);

    UserResponse updateStaffUser(AdminModUserInfoRequest ent);

    Page<UserResponse> getEmployeeUsers(PaginationRequest request);

    UserResponse getEmployeeUser(Long id);

    UserResponse createEmployeeUser(CreateUserRequest request);

    UserResponse updateEmployeeUser(AdminModUserInfoRequest ent);

    Page<UserResponse> getCustomerUsers(PaginationRequest request);

    UserResponse getCustomerUser(Long id);

    Page<UserResponse> getAllUsersForAdmin(PaginationRequest request);

    UserResponse getUserForAdmin(Long id);

    UserResponse updateUserForAdmin(AdminModUserInfoRequest req);

    void deleteUserForAdmin(Long id);

    UserResponse resetUserPasswordForAdmin(Long id);

    void deleteStaffUser(Long id);

    UserResponse resetStaffPassword(Long id);

    UserResponse getProfile();

    UserResponse updateProfileInfo(ModUserInfoRequest ent);

    ChangePasswordResponse changePassword(ChangePasswordRequest request);

    LoginResponse changeContact(ChangeContactRequest request);
}
