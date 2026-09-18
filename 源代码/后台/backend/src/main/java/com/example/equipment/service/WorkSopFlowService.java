package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.WorkSopFlow;
import com.example.equipment.dto.SearchResponse;

import java.util.List;
import java.util.Map;

public interface WorkSopFlowService extends IService<WorkSopFlow> {

    /**
     * 获取所有活跃的SOP流程列表
     */
    List<WorkSopFlow> listActive();

    /**
     * 生成SOP流程 - 调用Dify Agent处理知识库文档
     * @param documentId 文档ID
     * @param documentName 文档名称
     * @param datasetId 知识库ID
     * @param userId 用户ID
     * @return 生成的SOP流程
     */
    WorkSopFlow generateSopFlow(String documentId, String documentName, String datasetId, Long userId);

    /**
     * SOP流程测试 - 流式调用测试Agent
     * @param sopFlowId SOP流程ID
     * @param stepIndex 当前步骤索引
     * @param userInput 用户输入
     * @param imageUrl 上传图片URL
     * @param userId 用户ID
     * @param callback 流式回调
     */
    void testSopStep(Long sopFlowId, Integer stepIndex, String userInput,
                     String imageUrl, Long userId, DifyService.StreamCallback callback);

    /**
     * 解析步骤JSON
     */
    List<Map<String, Object>> parseSteps(Long sopFlowId);
}
