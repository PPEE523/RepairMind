package com.example.equipment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.equipment.aspect.OperLog;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.SysMenu;
import com.example.equipment.entity.SysRole;
import com.example.equipment.entity.SysRoleMenu;
import com.example.equipment.entity.SysUser;
import com.example.equipment.service.SysMenuService;
import com.example.equipment.service.SysRoleMenuService;
import com.example.equipment.service.SysRoleService;
import com.example.equipment.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private SysRoleMenuService sysRoleMenuService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String TOKEN_CACHE_PREFIX = "token:";

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        List<SysRole> roles = sysRoleService.list();
        return Result.success(roles);
    }

    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        SysRole role = sysRoleService.getById(id);
        return Result.success(role);
    }

    @PostMapping
    @OperLog(module = "角色管理", operType = "新增", description = "新增角色")
    public Result<Void> add(@RequestBody SysRole role) {
        role.setCreateTime(LocalDateTime.now());
        sysRoleService.save(role);
        log.info("新增角色: {}", role.getRoleName());
        return Result.success();
    }

    @PutMapping
    @OperLog(module = "角色管理", operType = "修改", description = "修改角色")
    public Result<Void> update(@RequestBody SysRole role) {
        sysRoleService.updateById(role);
        log.info("修改角色: {}", role.getId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperLog(module = "角色管理", operType = "删除", description = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        if (id <= 3) {
            return Result.error(400, "系统内置角色不允许删除");
        }
        sysRoleService.removeById(id);
        // 删除角色菜单关联
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, id);
        sysRoleMenuService.remove(wrapper);
        log.info("删除角色: {}", id);
        return Result.success();
    }

    @GetMapping("/{id}/menus")
    public Result<List<Long>> getRoleMenus(@PathVariable Long id) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, id);
        List<Long> menuIds = sysRoleMenuService.list(wrapper)
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
        return Result.success(menuIds);
    }

    @PostMapping("/{id}/menus")
    @OperLog(module = "角色管理", operType = "权限配置", description = "配置角色菜单权限")
    public Result<Void> saveRoleMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        // 先删除原有权限
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, id);
        sysRoleMenuService.remove(wrapper);

        // 添加新权限
        List<SysRoleMenu> roleMenus = menuIds.stream()
                .map(menuId -> {
                    SysRoleMenu roleMenu = new SysRoleMenu();
                    roleMenu.setRoleId(id);
                    roleMenu.setMenuId(menuId);
                    roleMenu.setCreateTime(LocalDateTime.now());
                    return roleMenu;
                })
                .collect(Collectors.toList());
        sysRoleMenuService.saveBatch(roleMenus);

        // 刷新该角色下所有用户的Token缓存
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getRoleId, id);
        List<SysUser> users = sysUserService.list(userWrapper);
        for (SysUser user : users) {
            String cacheKey = TOKEN_CACHE_PREFIX + user.getId();
            stringRedisTemplate.delete(cacheKey);
            log.info("清除用户Token缓存: userId={}, roleId={}", user.getId(), id);
        }

        log.info("配置角色 {} 菜单权限: {}", id, menuIds);
        return Result.success();
    }
}
