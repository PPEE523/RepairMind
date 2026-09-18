package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.WorkExecution;
import com.example.equipment.entity.WorkExecutionMain;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.mapper.WorkExecutionMainMapper;
import com.example.equipment.mapper.WorkExecutionMapper;
import com.example.equipment.service.WorkExecutionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class WorkExecutionServiceImpl extends ServiceImpl<WorkExecutionMapper, WorkExecution>
        implements WorkExecutionService {

    @Resource
    private WorkExecutionMainMapper workExecutionMainMapper;

    @Override
    @Transactional
    public WorkExecutionMain startExecution(Long workflowId, Long userId, Integer totalSteps) {
        // 创建执行主记录
        WorkExecutionMain main = new WorkExecutionMain();
        main.setWorkflowId(workflowId);
        main.setUserId(userId);
        main.setStatus("in_progress");
        main.setStartTime(LocalDateTime.now());
        main.setTotalSteps(totalSteps);
        main.setCompletedSteps(0);
        main.setCreateTime(LocalDateTime.now());
        main.setUpdateTime(LocalDateTime.now());
        workExecutionMainMapper.insert(main);

        // 创建每个步骤的执行记录
        for (int i = 0; i < totalSteps; i++) {
            WorkExecution execution = new WorkExecution();
            execution.setWorkflowId(main.getId());
            execution.setUserId(userId);
            execution.setStepIndex(i);
            execution.setStepStatus("pending");
            execution.setCreateTime(LocalDateTime.now());
            execution.setUpdateTime(LocalDateTime.now());
            save(execution);
        }

        log.info("开始执行作业流程: workflowId={}, userId={}, totalSteps={}", workflowId, userId, totalSteps);
        return main;
    }

    @Override
    @Transactional
    public void completeStep(Long executionId, Integer stepIndex, String remark) {
        LambdaQueryWrapper<WorkExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkExecution::getWorkflowId, executionId)
                .eq(WorkExecution::getStepIndex, stepIndex);
        
        WorkExecution execution = getOne(wrapper);
        if (execution == null) {
            throw new BusinessException("步骤记录不存在");
        }

        execution.setStepStatus("completed");
        execution.setCompletedTime(LocalDateTime.now());
        execution.setRemark(remark);
        execution.setUpdateTime(LocalDateTime.now());
        updateById(execution);

        // 更新主表完成步骤数
        WorkExecutionMain main = workExecutionMainMapper.selectById(executionId);
        if (main != null) {
            main.setCompletedSteps(main.getCompletedSteps() + 1);
            main.setUpdateTime(LocalDateTime.now());
            
            // 检查是否所有步骤都完成
            if (main.getCompletedSteps() >= main.getTotalSteps()) {
                main.setStatus("completed");
                main.setEndTime(LocalDateTime.now());
            }
            
            workExecutionMainMapper.updateById(main);
        }

        log.info("步骤完成: executionId={}, stepIndex={}", executionId, stepIndex);
    }

    @Override
    @Transactional
    public void markException(Long executionId, Integer stepIndex, String remark, String imageUrls) {
        LambdaQueryWrapper<WorkExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkExecution::getWorkflowId, executionId)
                .eq(WorkExecution::getStepIndex, stepIndex);
        
        WorkExecution execution = getOne(wrapper);
        if (execution == null) {
            throw new BusinessException("步骤记录不存在");
        }

        execution.setStepStatus("exception");
        execution.setRemark(remark);
        execution.setImageUrls(imageUrls);
        execution.setUpdateTime(LocalDateTime.now());
        updateById(execution);

        // 更新主表状态
        WorkExecutionMain main = workExecutionMainMapper.selectById(executionId);
        if (main != null) {
            main.setStatus("exception");
            main.setUpdateTime(LocalDateTime.now());
            workExecutionMainMapper.updateById(main);
        }

        log.info("步骤异常: executionId={}, stepIndex={}", executionId, stepIndex);
    }

    @Override
    public List<WorkExecution> getExecutionDetails(Long executionId) {
        LambdaQueryWrapper<WorkExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkExecution::getWorkflowId, executionId)
                .orderByAsc(WorkExecution::getStepIndex);
        return list(wrapper);
    }

    @Override
    public List<WorkExecutionMain> getUserExecutions(Long userId) {
        LambdaQueryWrapper<WorkExecutionMain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkExecutionMain::getUserId, userId)
                .orderByDesc(WorkExecutionMain::getCreateTime);
        return workExecutionMainMapper.selectList(wrapper);
    }
}
