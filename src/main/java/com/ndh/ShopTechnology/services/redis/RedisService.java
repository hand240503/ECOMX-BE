//package com.ndh.ShopTechnology.services.redis;
//
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
////@Service
//public class RedisService {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    public RedisService(RedisTemplate<String, Object> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    public void set(String key, Object value, long timeoutInSeconds) {
//        redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
//    }
//
//    public Object get(String key) {
//        return redisTemplate.opsForValue().get(key);
//    }
//
//    public void delete(String key) {
//        redisTemplate.delete(key);
//    }
//
//    public boolean exists(String key) {
//        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
//    }
//}
