//package com.ndh.ShopTechnology.services.redis;
//
//import com.ndh.ShopTechnology.dto.response.user.UserResponse;
//import org.springframework.stereotype.Service;
//
////@Service
//public class UserRedisService {
//
//    private final RedisService redisService;
//
//    public UserRedisService(RedisService redisService) {
//        this.redisService = redisService;
//    }
//
//    private String getKey(String username) {
//        return "user:username:" + username;
//    }
//
//    // Lưu user vào cache
//    public void saveUser(UserResponse user, long timeoutInSeconds) {
//        redisService.set(getKey(user.getUsername()), user, timeoutInSeconds);
//    }
//
//    // Lấy user từ cache
//    public UserResponse getUser(String username) {
//        return (UserResponse) redisService.get(getKey(username));
//    }
//
//    // Xoá user khỏi cache
//    public void deleteUser(String username) {
//        redisService.delete(getKey(username));
//    }
//
//    // Kiểm tra tồn tại
//    public boolean exists(String username) {
//        return redisService.exists(getKey(username));
//    }
//}
