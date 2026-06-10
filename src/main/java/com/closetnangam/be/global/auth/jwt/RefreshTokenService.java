package com.closetnangam.be.global.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + userId, refreshToken, Duration.ofDays(7));
    }

    public String get(Long userId) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + userId);
    }

    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}