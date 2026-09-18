package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> getMenusByRoleId(Long roleId);
}
