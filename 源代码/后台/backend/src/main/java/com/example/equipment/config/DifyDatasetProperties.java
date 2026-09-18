package com.example.equipment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify 知识库数据集配置属性
 * 用于按模块过滤可见的知识库列表
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify.dataset")
public class DifyDatasetProperties {

    /**
     * 按模块的可见知识库白名单
     * key: 模块 scope（如 knowledge-management, case-audit）
     * value: 逗号分隔的知识库 ID 列表
     */
    private Map<String, String> visibleIds = new HashMap<>();
}
