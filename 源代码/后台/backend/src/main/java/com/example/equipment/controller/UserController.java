package com.example.equipment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.equipment.aspect.OperLog;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.SysUser;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public Result<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username) {

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserService.page(page, wrapper);

        // 清除密码字段
        result.getRecords().forEach(user -> user.setPassword(null));

        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 新增用户
     */
    @PostMapping
    @OperLog(module = "用户管理", operType = "新增", description = "新增用户")
    public Result<Void> add(@RequestBody SysUser user) {
        // 检查用户名是否已存在
        SysUser existingUser = sysUserService.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(1);

        sysUserService.save(user);
        log.info("新增用户: {}", user.getUsername());
        return Result.success();
    }

    /**
     * 修改用户
     */
    @PutMapping
    @OperLog(module = "用户管理", operType = "修改", description = "修改用户信息")
    public Result<Void> update(@RequestBody SysUser user) {
        SysUser existingUser = sysUserService.getById(user.getId());
        if (existingUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 不更新密码
        user.setPassword(null);
        sysUserService.updateById(user);

        log.info("修改用户: {}", user.getId());
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @OperLog(module = "用户管理", operType = "删除", description = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 不允许删除管理员
        if (user.getRoleId() != null && user.getRoleId() == 1) {
            throw new BusinessException("不允许删除管理员账户");
        }

        sysUserService.removeById(id);
        log.info("删除用户: {}", id);
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/reset-password")
    @OperLog(module = "用户管理", operType = "重置密码", description = "重置用户密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordUpdateTime(LocalDateTime.now());
        sysUserService.updateById(user);

        log.info("重置用户密码: {}", id);
        return Result.success();
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status/{status}")
    @OperLog(module = "用户管理", operType = "状态变更", description = "变更用户状态")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setStatus(status);
        sysUserService.updateById(user);

        log.info("变更用户状态: {} -> {}", id, status);
        return Result.success();
    }
}
