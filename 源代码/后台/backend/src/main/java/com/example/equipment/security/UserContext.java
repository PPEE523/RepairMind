package com.example.equipment.security;

import lombok.Data;

/**
 * 用户上下文信息
 */
@Data
public class UserContext {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 角色编码
     */
    private String roleCode;
}
