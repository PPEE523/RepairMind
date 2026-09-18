package com.example.equipment.enums;

import lombok.Getter;

/**
 * Dify Agent业务类型枚举
 */
@Getter
public enum DifyAgentType {

    ZDJC("zdjc", "多模态检索"),
    ZYZY("zyzy", "作业管理");

    private final String code;
    private final String desc;

    DifyAgentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举值
     */
    public static DifyAgentType fromCode(String code) {
        if (code == null) {
            return ZDJC;
        }
        for (DifyAgentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return ZDJC;
    }
}
