package com.example.equipment.controller;

import com.example.equipment.dto.ChangePasswordRequest;
import com.example.equipment.dto.LoginRequest;
import com.example.equipment.dto.LoginResponse;
import com.example.equipment.dto.MenuDTO;
import com.example.equipment.dto.Result;
import com.example.equipment.dto.UpdateProfileRequest;
import com.example.equipment.entity.SysMenu;
import com.example.equipment.entity.SysUser;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.service.SysMenuService;
import com.example.equipment.service.SysUserService;
import com.example.equipment.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String TOKEN_CACHE_PREFIX = "token:";
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("=== 登录请求开始 ===");
        log.info("请求用户名: {}", request.getUsername());
        
        // 检查登录失败次数
        String failKey = LOGIN_FAIL_PREFIX + request.getUsername();
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        log.info("Redis中的失败次数: {}", failCount);
        
        if (failCount != null && Integer.parseInt(failCount) >= 5) {
            throw new BusinessException(403, "登录失败次数过多，账户已锁定15分钟");
        }

        SysUser user = sysUserService.findByUsername(request.getUsername());
        if (user == null) {
            log.warn("用户不存在: {}", request.getUsername());
            incrementFailCount(failKey);
            throw new BusinessException(401, "用户名或密码错误");
        }

        log.info("用户信息 - ID: {}, 用户名: {}, 角色ID: {}, 状态: {}", 
                user.getId(), user.getUsername(), user.getRoleId(), user.getStatus());
        boolean passwordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("密码匹配结果: {}", passwordMatch);
        
        if (!passwordMatch) {
            log.warn("密码不匹配");
            incrementFailCount(failKey);
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 清除登录失败次数
        stringRedisTemplate.delete(failKey);

        // 检查密码是否过期（90天）
        boolean passwordExpired = false;
        if (user.getPasswordUpdateTime() != null) {
            LocalDateTime expireTime = user.getPasswordUpdateTime().plusDays(90);
            passwordExpired = LocalDateTime.now().isAfter(expireTime);
        } else {
            // 如果没有密码更新时间，默认已过期
            passwordExpired = true;
        }

        // 生成Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        // 将Token存入Redis
        String cacheKey = TOKEN_CACHE_PREFIX + user.getId();
        stringRedisTemplate.opsForValue().set(cacheKey, token, 24, TimeUnit.HOURS);
        log.info("Token已存入Redis, key: {}", cacheKey);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roleId(user.getRoleId())
                .passwordExpired(passwordExpired)
                .build();

        log.info("=== 登录成功 ===");
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody LoginRequest request) {
        // 检查用户名是否已存在
        SysUser existingUser = sysUserService.findByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 密码强度校验
        if (!isStrongPassword(request.getPassword())) {
            throw new BusinessException(400, "密码强度不足，需包含大小写字母、数字和特殊字符，至少8位");
        }

        // 创建新用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setRoleId(3L); // 默认实习检修人员角色
        user.setStatus(1);
        user.setPasswordUpdateTime(java.time.LocalDateTime.now());
        sysUserService.save(user);

        log.info("用户 {} 注册成功", request.getUsername());
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(actualToken);

        // 从Redis中删除Token
        String cacheKey = TOKEN_CACHE_PREFIX + userId;
        stringRedisTemplate.delete(cacheKey);

        log.info("用户ID {} 已退出登录", userId);
        return Result.success();
    }

    /**
     * 增加登录失败次数
     */
    private void incrementFailCount(String failKey) {
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1) {
            // 首次失败，设置过期时间15分钟
            stringRedisTemplate.expire(failKey, 15, TimeUnit.MINUTES);
        }
    }

    /**
     * 密码强度校验
     */
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);
        
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        // 验证新密码强度
        if (!isStrongPassword(request.getNewPassword())) {
            throw new BusinessException(400, "密码强度不足，需包含大小写字母、数字和特殊字符，至少8位");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordUpdateTime(java.time.LocalDateTime.now());
        sysUserService.updateById(user);

        log.info("用户 {} 修改密码成功", user.getUsername());
        return Result.success();
    }

    /**
     * 更新当前用户个人信息（用户名、昵称）
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);

        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        // 如果用户名有变更，检查唯一性
        if (!user.getUsername().equals(request.getUsername())) {
            SysUser existingUser = sysUserService.findByUsername(request.getUsername());
            if (existingUser != null) {
                throw new BusinessException(400, "用户名已存在");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            user.setNickname(request.getNickname());
        }

        sysUserService.updateById(user);
        log.info("用户 {} 更新个人信息成功", user.getUsername());
        return Result.success();
    }

    @GetMapping("/menu")
    public Result<List<MenuDTO>> getMenu(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);
        
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        // 从数据库查询角色对应的菜单
        List<SysMenu> menuList = sysMenuService.getMenusByRoleId(user.getRoleId());
        
        // 构建树形菜单
        List<MenuDTO> menus = buildMenuTree(menuList, 0L);

        return Result.success(menus);
    }

    @GetMapping("/menu/tree")
    public Result<List<MenuDTO>> getMenuTree() {
        // 获取所有菜单
        List<SysMenu> allMenus = sysMenuService.list();
        
        // 构建树形菜单
        List<MenuDTO> menuTree = buildMenuTree(allMenus, 0L);

        return Result.success(menuTree);
    }

    private List<MenuDTO> buildMenuTree(List<SysMenu> menuList, Long parentId) {
        return menuList.stream()
                .filter(menu -> menu.getParentId().equals(parentId))
                .map(menu -> MenuDTO.builder()
                        .id(menu.getId())
                        .path(menu.getPath())
                        .title(menu.getName())
                        .icon(menu.getIcon())
                        .children(buildMenuTree(menuList, menu.getId()))
                        .build())
                .collect(Collectors.toList());
    }
}
