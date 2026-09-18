package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    SysUser findByUsername(String username);
}
