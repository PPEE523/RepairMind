package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.WorkExecution;
import com.example.equipment.entity.WorkExecutionMain;

import java.util.List;

public interface WorkExecutionService extends IService<WorkExecution> {

    /**
     * 开始执行作业流程
     */
    WorkExecutionMain startExecution(Long workflowId, Long userId, Integer totalSteps);

    /**
     * 完成步骤
     */
    void completeStep(Long executionId, Integer stepIndex, String remark);

    /**
     * 标记步骤异常
     */
    void markException(Long executionId, Integer stepIndex, String remark, String imageUrls);

    /**
     * 获取执行记录
     */
    List<WorkExecution> getExecutionDetails(Long executionId);

    /**
     * 获取用户执行记录
     */
    List<WorkExecutionMain> getUserExecutions(Long userId);
}
