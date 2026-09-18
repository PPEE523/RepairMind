package com.example.equipment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 请求频率限制配置
 */
@Configuration
public class RateLimitConfig {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 600;

    /**
     * 检查请求频率
     * @param key 限制key（通常是用户ID或IP）
     * @return 是否允许请求
     */
    public boolean isAllowed(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        String count = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (count == null) {
            stringRedisTemplate.opsForValue().set(redisKey, "1", 1, TimeUnit.MINUTES);
            return true;
        }
        
        int currentCount = Integer.parseInt(count);
        if (currentCount >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        
        stringRedisTemplate.opsForValue().increment(redisKey);
        return true;
    }

    /**
     * 获取剩余请求次数
     */
    public int getRemainingRequests(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        String count = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (count == null) {
            return MAX_REQUESTS_PER_MINUTE;
        }
        
        int currentCount = Integer.parseInt(count);
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount);
    }
}
