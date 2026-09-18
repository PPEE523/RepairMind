package com.example.equipment.security;

import com.example.equipment.entity.SysUser;
import com.example.equipment.service.SysUserService;
import com.example.equipment.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";
    private static final String TOKEN_CACHE_PREFIX = "token:";

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SysUserService sysUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 放行CORS预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 放行登录接口和公开接口
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取Token
        String token = extractToken(request);

        if (!StringUtils.hasText(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未提供认证令牌\",\"permission\":false}");
            return;
        }

        // 验证Token
        if (!jwtUtils.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"令牌无效或已过期\",\"permission\":false}");
            return;
        }

        // 检查Token是否在Redis中（用于实现注销功能）
        Long userId = jwtUtils.getUserIdFromToken(token);
        String cacheKey = TOKEN_CACHE_PREFIX + userId;
        String cachedToken = stringRedisTemplate.opsForValue().get(cacheKey);

        if (cachedToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"令牌已失效，请重新登录\",\"permission\":false}");
            return;
        }

        // 刷新Token过期时间
        stringRedisTemplate.expire(cacheKey, 24, TimeUnit.HOURS);

        // 设置认证信息
        String username = jwtUtils.getUsernameFromToken(token);

        // 构建UserContext作为principal
        UserContext userContext = new UserContext();
        userContext.setUserId(userId);
        userContext.setUsername(username);

        // 从数据库查询用户角色信息
        SysUser sysUser = sysUserService.getById(userId);
        if (sysUser != null) {
            userContext.setRoleId(sysUser.getRoleId());
            userContext.setDeptId(sysUser.getDeptId());
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userContext,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 将用户信息存入Request属性，方便后续使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);

        filterChain.doFilter(request, response);
    }

    /**
     * 提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 判断是否为公开路径
     */
    private boolean isPublicPath(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.startsWith("/api/public/")
                || path.startsWith("/uploads/")
                || path.equals("/actuator/health")
                || path.startsWith("/doc.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/dify/knowledge/files/preview");
    }
}
