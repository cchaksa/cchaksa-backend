//package com.chukchuk.haksa.infrastructure.cache.redis;
//
//import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
//import com.chukchuk.haksa.global.crypto.AesEncryptor;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//
//@Component
//@RequiredArgsConstructor
//@ConditionalOnProperty(name = "portal.credential.store", havingValue = "redis")
//public class RedisPortalCredentialStore implements PortalCredentialStore {
//
//    private static final Duration TTL = Duration.ofMinutes(10);
//
//    private final RedisTemplate<String, String> redisTemplate;
//    private final AesEncryptor aesEncryptor;
//
//    @Override
//    public void save(String userId, String username, String password) {
//        String prefix = key(userId);
//        try {
//            redisTemplate.opsForValue()
//                    .set(prefix + ":username", aesEncryptor.encrypt(username), TTL);
//            redisTemplate.opsForValue()
//                    .set(prefix + ":password", aesEncryptor.encrypt(password), TTL);
//        } catch (Exception e) {
//            throw new RuntimeException("포털 자격증명 Redis 저장 실패", e);
//        }
//    }
//
//    @Override
//    public String getUsername(String userId) {
//        return decrypt(redisTemplate.opsForValue().get(key(userId) + ":username"));
//    }
//
//    @Override
//    public String getPassword(String userId) {
//        return decrypt(redisTemplate.opsForValue().get(key(userId) + ":password"));
//    }
//
//    @Override
//    public void clear(String userId) {
//        redisTemplate.delete(key(userId) + ":username");
//        redisTemplate.delete(key(userId) + ":password");
//    }
//
//    private String decrypt(String encrypted) {
//        if (encrypted == null) return null;
//        try {
//            return aesEncryptor.decrypt(encrypted);
//        } catch (Exception e) {
//            throw new RuntimeException("복호화 실패", e);
//        }
//    }
//
//    private String key(String userId) {
//        return "portal:" + userId;
//    }
//}