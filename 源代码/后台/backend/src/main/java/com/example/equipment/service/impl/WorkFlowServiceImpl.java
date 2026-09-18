package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.WorkFlow;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.mapper.WorkFlowMapper;
import com.example.equipment.service.WorkFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class WorkFlowServiceImpl extends ServiceImpl<WorkFlowMapper, WorkFlow> implements WorkFlowService {

    @Override
    public WorkFlow getLatestByEquipModel(String equipModel, String repairLevel) {
        LambdaQueryWrapper<WorkFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkFlow::getEquipModel, equipModel)
                .eq(WorkFlow::getRepairLevel, repairLevel)
                .orderByDesc(WorkFlow::getVersion)
                .last("LIMIT 1");
        return getOne(wrapper);
    }

    @Override
    public List<WorkFlow> getVersionHistory(String equipModel, String repairLevel) {
        LambdaQueryWrapper<WorkFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkFlow::getEquipModel, equipModel)
                .eq(WorkFlow::getRepairLevel, repairLevel)
                .orderByDesc(WorkFlow::getVersion);
        return list(wrapper);
    }

    @Override
    public void rollbackToVersion(Long workflowId, Long userId) {
        WorkFlow source = getById(workflowId);
        if (source == null) {
            throw new BusinessException("作业流程不存在");
        }

        // 获取最新版本号
        WorkFlow latest = getLatestByEquipModel(source.getEquipModel(), source.getRepairLevel());
        int newVersion = latest != null ? latest.getVersion() + 1 : 1;

        // 创建新版本（基于历史版本）
        WorkFlow newFlow = new WorkFlow();
        newFlow.setEquipModel(source.getEquipModel());
        newFlow.setRepairLevel(source.getRepairLevel());
        newFlow.setStepJson(source.getStepJson());
        newFlow.setVersion(newVersion);
        newFlow.setCreateUser(userId);
        newFlow.setCreateTime(LocalDateTime.now());
        save(newFlow);

        log.info("回滚作业流程: sourceId={}, newVersion={}", workflowId, newVersion);
    }
}
