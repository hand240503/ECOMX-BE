package com.ndh.ShopTechnology.services.user;

import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.user.UserEntity;

import org.springframework.data.domain.Page;

public interface UserService {
    Page<UserResponse> getAllUsers(PaginationRequest request);

    UserEntity getCurrentUser();

    UserResponse getUserInfo(Long id);

    UserResponse getProfile();

    UserResponse updateUserInfo(ModUserInfoRequest ent);

    UserResponse updateProfileInfo(ModUserInfoRequest ent);

    UserResponse createUser(CreateUserRequest request);
}
