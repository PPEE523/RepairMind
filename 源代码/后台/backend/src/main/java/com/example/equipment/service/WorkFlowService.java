package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.WorkFlow;

import java.util.List;

public interface WorkFlowService extends IService<WorkFlow> {

    WorkFlow getLatestByEquipModel(String equipModel, String repairLevel);

    /**
     * 获取版本历史
     */
    List<WorkFlow> getVersionHistory(String equipModel, String repairLevel);

    /**
     * 回滚到指定版本
     */
    void rollbackToVersion(Long workflowId, Long userId);
}
