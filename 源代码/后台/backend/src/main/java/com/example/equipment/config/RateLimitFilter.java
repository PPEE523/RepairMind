package com.example.equipment.config;

import com.example.equipment.dto.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求频率限制过滤器
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Resource
    private RateLimitConfig rateLimitConfig;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // 只限制API请求
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取客户端标识（IP或用户ID）
        String clientKey = getClientKey(request);
        // 上传接口使用独立限流 key，避免受影响
        String rateKey = path.startsWith("/api/chat/upload") ? clientKey + ":upload" : clientKey;

        if (!rateLimitConfig.isAllowed(rateKey)) {
            log.warn("请求频率超限: client={}, path={}", clientKey, path);
            
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            
            Result<Void> result = Result.error(429, "请求过于频繁，请稍后再试");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return;
        }

        // 添加剩余请求次数到响应头
        int remaining = rateLimitConfig.getRemainingRequests(clientKey);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        
        filterChain.doFilter(request, response);
    }

    private String getClientKey(HttpServletRequest request) {
        // 优先使用用户ID，否则使用IP
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "user:" + userId.toString();
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        return "ip:" + ip;
    }
}
