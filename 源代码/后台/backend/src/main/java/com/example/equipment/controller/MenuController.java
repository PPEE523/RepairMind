package com.example.equipment.controller;

import com.example.equipment.dto.MenuDTO;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.SysMenu;
import com.example.equipment.service.SysMenuService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/menu")
@PreAuthorize("hasRole('ADMIN')")
public class MenuController {

    @Resource
    private SysMenuService sysMenuService;

    @GetMapping("/tree")
    public Result<List<MenuDTO>> getMenuTree() {
        List<SysMenu> allMenus = sysMenuService.list();
        List<MenuDTO> menuTree = buildMenuTree(allMenus, 0L);
        return Result.success(menuTree);
    }

    @GetMapping("/list")
    public Result<List<SysMenu>> getMenuList() {
        List<SysMenu> menus = sysMenuService.list();
        return Result.success(menus);
    }

    @GetMapping("/{id}")
    public Result<SysMenu> getMenuById(@PathVariable Long id) {
        SysMenu menu = sysMenuService.getById(id);
        if (menu == null) {
            return Result.error(404, "菜单不存在");
        }
        return Result.success(menu);
    }

    @PostMapping
    public Result<Void> saveMenu(@RequestBody SysMenu menu) {
        sysMenuService.save(menu);
        log.info("新增菜单: name={}", menu.getName());
        return Result.success();
    }

    @PutMapping
    public Result<Void> updateMenu(@RequestBody SysMenu menu) {
        sysMenuService.updateById(menu);
        log.info("更新菜单: id={}, name={}", menu.getId(), menu.getName());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        sysMenuService.removeById(id);
        log.info("删除菜单: id={}", id);
        return Result.success();
    }

    @GetMapping("/role/{roleId}")
    public Result<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        List<SysMenu> menus = sysMenuService.getMenusByRoleId(roleId);
        List<Long> menuIds = menus.stream()
                .map(SysMenu::getId)
                .collect(Collectors.toList());
        return Result.success(menuIds);
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
