package com.example.equipment.aspect;

import com.example.equipment.dto.Result;
import com.example.equipment.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 请求频率限制切面
 * 每分钟最多100次请求
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int LOCK_DURATION_MINUTES = 1;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 定义切点 - 所有Controller方法（排除登录和公开接口）
     */
    @Pointcut("execution(* com.example.equipment.controller..*.*(..)) && " +
              "!execution(* com.example.equipment.controller.AuthController.login(..)) && " +
              "!execution(* com.example.equipment.controller.AuthController.register(..)) && " +
              "!execution(* com.example.equipment.controller..*.public*(..))")
    public void rateLimitPointcut() {}

    @Around("rateLimitPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        // 获取用户ID或IP作为限流key
        Object userIdObj = request.getAttribute("userId");
        String userId = null;
        if (userIdObj != null) {
            userId = userIdObj instanceof Long ? String.valueOf(userIdObj) : userIdObj.toString();
        }
        String key = RATE_LIMIT_PREFIX + (userId != null ? userId : getClientIp(request));

        // 检查请求次数
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("请求频率超限: key={}, count={}", key, count);
            throw new BusinessException(429, "请求频率过高，请稍后再试");
        }

        // 增加请求次数
        Long newCount = stringRedisTemplate.opsForValue().increment(key);
        if (newCount != null && newCount == 1) {
            // 首次请求，设置过期时间1分钟
            stringRedisTemplate.expire(key, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        return joinPoint.proceed();
    }

    /**
     * 获取Request对象
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}