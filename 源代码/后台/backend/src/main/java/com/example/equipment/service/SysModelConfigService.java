package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.SysModelConfig;

/**
 * Dify配置服务
 */
public interface SysModelConfigService extends IService<SysModelConfig> {

    /**
     * 获取当前启用的配置
     * @return 启用的配置
     */
    SysModelConfig getActiveConfig();

    /**
     * 切换配置启用状态
     * @param id 配置ID
     * @param enable 是否启用
     */
    void toggleEnable(Long id, Integer enable);
}