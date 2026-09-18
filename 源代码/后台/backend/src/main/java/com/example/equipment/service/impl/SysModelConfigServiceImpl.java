package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.SysModelConfig;
import com.example.equipment.mapper.SysModelConfigMapper;
import com.example.equipment.service.SysModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Dify配置服务实现
 */
@Slf4j
@Service
public class SysModelConfigServiceImpl extends ServiceImpl<SysModelConfigMapper, SysModelConfig>
        implements SysModelConfigService {

    @Override
    public SysModelConfig getActiveConfig() {
        LambdaQueryWrapper<SysModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysModelConfig::getEnable, 1)
                .orderByDesc(SysModelConfig::getCreateTime)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public void toggleEnable(Long id, Integer enable) {
        SysModelConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("配置不存在");
        }
        
        // 如果要启用，先禁用其他配置
        if (enable == 1) {
            LambdaQueryWrapper<SysModelConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysModelConfig::getEnable, 1);
            SysModelConfig activeConfig = getOne(wrapper);
            if (activeConfig != null && !activeConfig.getId().equals(id)) {
                activeConfig.setEnable(0);
                updateById(activeConfig);
            }
        }
        
        config.setEnable(enable);
        updateById(config);
        
        log.info("Dify配置状态变更: id={}, enable={}", id, enable);
    }
}