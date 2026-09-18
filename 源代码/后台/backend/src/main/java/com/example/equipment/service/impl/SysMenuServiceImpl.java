package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.SysMenu;
import com.example.equipment.entity.SysRoleMenu;
import com.example.equipment.mapper.SysMenuMapper;
import com.example.equipment.mapper.SysRoleMenuMapper;
import com.example.equipment.service.SysMenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysMenu> getMenusByRoleId(Long roleId) {
        // 查询角色关联的菜单ID
        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<Long> menuIds = sysRoleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return List.of();
        }

        // 查询菜单详情
        LambdaQueryWrapper<SysMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSortOrder);
        return list(menuWrapper);
    }
}
