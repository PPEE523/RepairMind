package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.SysConfig;
import com.example.equipment.mapper.SysConfigMapper;
import com.example.equipment.service.SysConfigService;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {
}
