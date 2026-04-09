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
    Page<UserResponse> getAllUsers(PaginationRequest request);

    UserEntity getCurrentUser();

    UserResponse getUserInfo(Long id);

    UserResponse getProfile();

    UserResponse updateUserInfo(AdminModUserInfoRequest ent);

    UserResponse updateProfileInfo(ModUserInfoRequest ent);

    ChangePasswordResponse changePassword(ChangePasswordRequest request);

    LoginResponse changeContact(ChangeContactRequest request);

    UserResponse createUser(CreateUserRequest request);
}
