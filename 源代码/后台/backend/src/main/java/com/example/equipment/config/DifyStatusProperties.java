package com.example.equipment.config;

import com.example.equipment.enums.DifyAgentType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dify状态相关配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify.status")
public class DifyStatusProperties {

    private String zyzyApiKey;
    private String zdjcApiKey;
    private String datasetApiKey;

    /**
     * 根据Agent类型获取对应的API Key
     */
    public String getApiKey(DifyAgentType agentType) {
        if (agentType == null) {
            return zdjcApiKey;
        }
        return switch (agentType) {
            case ZDJC -> zdjcApiKey;
            case ZYZY -> zyzyApiKey;
        };
    }
}
