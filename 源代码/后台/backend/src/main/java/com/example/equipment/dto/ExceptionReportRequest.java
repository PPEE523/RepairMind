package com.example.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 检修异常上报请求
 */
@Data
public class ExceptionReportRequest {

    /**
     * 设备型号
     */
    @NotBlank(message = "设备型号不能为空")
    private String equipModel;

    /**
     * 当前作业流程ID
     */
    private Long workflowId;

    /**
     * 当前步骤索引
     */
    private Integer currentStepIndex;

    /**
     * 异常描述
     */
    @NotBlank(message = "异常描述不能为空")
    private String exceptionDescription;

    /**
     * 异常图片URL
     */
    private String imageUrl;

    /**
     * 检修等级
     */
    private String repairLevel;
}