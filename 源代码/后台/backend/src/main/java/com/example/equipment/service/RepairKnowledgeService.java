package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.RepairKnowledge;

import java.util.List;
import java.util.Map;

public interface RepairKnowledgeService extends IService<RepairKnowledge> {

    Page<RepairKnowledge> getAuditList(Integer auditStatus, Integer pageNum, Integer pageSize);

    Page<RepairKnowledge> getKnowledgeList(Integer auditStatus, Integer pageNum, Integer pageSize, Long currentUserId, Long roleId);

    void audit(Long knowledgeId, Integer auditStatus, String remark, String title, String content, String symptom, String solution, String equipModel);
    
    /**
     * 审核案例并上传到指定知识库
     * @param knowledgeId 案例ID
     * @param auditStatus 审核状态
     * @param remark 备注
     * @param title 标题
     * @param content 内容
     * @param symptom 故障描述
     * @param solution 解决方案
     * @param equipModel 设备型号
     * @param datasetIds 要上传的知识库ID列表
     */
    void auditAndUpload(Long knowledgeId, Integer auditStatus, String remark, String title, String content, String symptom, String solution, String equipModel, List<String> datasetIds);
    
    /**
     * 批量检查案例的知识库文档是否存在
     * @param knowledgeIds 案例ID列表
     * @return Map<knowledgeId, Map<"exists", true/false>>
     */
    Map<Long, Map<String, Object>> checkDocuments(List<Long> knowledgeIds);

    /**
     * 根据 Dify 文档 ID 批量查询已审核的案例（用于用户端案例卡片）
     * @param docIds Dify document_id 列表
     * @return 案例列表
     */
    List<RepairKnowledge> listByDocumentIds(List<String> docIds);
}
