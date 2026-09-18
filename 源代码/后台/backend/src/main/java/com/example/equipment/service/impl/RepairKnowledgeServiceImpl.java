package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.RepairKnowledge;
import com.example.equipment.entity.SysUser;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.mapper.RepairKnowledgeMapper;
import com.example.equipment.service.DifyKnowledgeService;
import com.example.equipment.service.DifyService;
import com.example.equipment.service.RepairKnowledgeService;
import com.example.equipment.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RepairKnowledgeServiceImpl extends ServiceImpl<RepairKnowledgeMapper, RepairKnowledge>
        implements RepairKnowledgeService {

    @Resource
    private DifyService difyService;

    @Resource
    private DifyKnowledgeService difyKnowledgeService;

    @Resource
    private SysUserService sysUserService;

    @Value("${dify.dataset.case-id:${dify.dataset.id}}")
    private String caseDatasetId;

    @Override
    public Page<RepairKnowledge> getAuditList(Integer auditStatus, Integer pageNum, Integer pageSize) {
        Page<RepairKnowledge> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairKnowledge> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(RepairKnowledge::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(RepairKnowledge::getCreateTime);
        page = page(page, wrapper);

        // 批量查询上传人昵称并填充
        fillUploadUserNames(page.getRecords());

        return page;
    }

    private void fillUploadUserNames(List<RepairKnowledge> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> userIds = records.stream()
                .map(RepairKnowledge::getUploadUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userNicknameMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
        records.forEach(record -> {
            if (record.getUploadUserId() != null) {
                record.setUploadUserName(userNicknameMap.getOrDefault(record.getUploadUserId(), String.valueOf(record.getUploadUserId())));
            }
        });
    }

    @Override
    public Page<RepairKnowledge> getKnowledgeList(Integer auditStatus, Integer pageNum, Integer pageSize, Long currentUserId, Long roleId) {
        Page<RepairKnowledge> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RepairKnowledge> wrapper = new LambdaQueryWrapper<>();

        // 管理员可以看到所有案例
        if (roleId != null && roleId == 1) {
            if (auditStatus != null) {
                wrapper.eq(RepairKnowledge::getAuditStatus, auditStatus);
            }
        } else {
            // 非管理员
            if (auditStatus != null) {
                // 指定了审核状态：所有该状态的案例对所有角色可见
                wrapper.eq(RepairKnowledge::getAuditStatus, auditStatus);
            } else {
                // 未指定审核状态：展示自己上传的所有案例 + 他人已审核通过的案例
                wrapper.and(w -> w
                        .eq(RepairKnowledge::getUploadUserId, currentUserId)
                        .or()
                        .eq(RepairKnowledge::getAuditStatus, 1)
                );
            }
        }

        wrapper.orderByDesc(RepairKnowledge::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public void audit(Long knowledgeId, Integer auditStatus, String remark, String title, String content, String symptom, String solution, String equipModel) {
        RepairKnowledge knowledge = getById(knowledgeId);
        if (knowledge == null) {
            throw new BusinessException("案例不存在");
        }

        // 更新案例内容（如果审核人提供了修改）
        if (title != null && !title.isEmpty()) {
            knowledge.setTitle(title);
        }
        if (content != null && !content.isEmpty()) {
            knowledge.setContent(content);
        }
        if (symptom != null && !symptom.isEmpty()) {
            knowledge.setSymptom(symptom);
        }
        if (solution != null && !solution.isEmpty()) {
            knowledge.setSolution(solution);
        }
        if (equipModel != null && !equipModel.isEmpty()) {
            knowledge.setEquipModel(equipModel);
        }

        knowledge.setAuditStatus(auditStatus);
        updateById(knowledge);

        // 审核通过后上传到默认知识库
        if (auditStatus == 1) {
            try {
                String documentId = difyService.uploadToDataset(knowledge);
                knowledge.setDocumentId(documentId);
                knowledge.setDatasetId(caseDatasetId);
                updateById(knowledge);
                log.info("案例 {} 审核通过，已上传至默认知识库，documentId={}", knowledgeId, documentId);
            } catch (Exception e) {
                log.error("案例 {} 上传至默认知识库失败: {}", knowledgeId, e.getMessage());
            }
        }
    }
    
    @Override
    public void auditAndUpload(Long knowledgeId, Integer auditStatus, String remark, String title, String content, String symptom, String solution, String equipModel, List<String> datasetIds) {
        RepairKnowledge knowledge = getById(knowledgeId);
        if (knowledge == null) {
            throw new BusinessException("案例不存在");
        }

        // 更新案例内容（如果审核人提供了修改）
        if (title != null && !title.isEmpty()) {
            knowledge.setTitle(title);
        }
        if (content != null && !content.isEmpty()) {
            knowledge.setContent(content);
        }
        if (symptom != null && !symptom.isEmpty()) {
            knowledge.setSymptom(symptom);
        }
        if (solution != null && !solution.isEmpty()) {
            knowledge.setSolution(solution);
        }
        if (equipModel != null && !equipModel.isEmpty()) {
            knowledge.setEquipModel(equipModel);
        }

        knowledge.setAuditStatus(auditStatus);
        updateById(knowledge);

        // 审核通过后上传到指定的知识库
        if (auditStatus == 1 && datasetIds != null && !datasetIds.isEmpty()) {
            StringBuilder docIds = new StringBuilder();
            StringBuilder dsIds = new StringBuilder();
            for (String datasetId : datasetIds) {
                try {
                    String documentId = difyService.uploadToDataset(knowledge, datasetId);
                    if (docIds.length() > 0) docIds.append(",");
                    docIds.append(documentId);
                    if (dsIds.length() > 0) dsIds.append(",");
                    dsIds.append(datasetId);
                    log.info("案例 {} 审核通过，已上传至知识库 {}，documentId={}", knowledgeId, datasetId, documentId);
                } catch (Exception e) {
                    log.error("案例 {} 上传至知识库 {} 失败: {}", knowledgeId, datasetId, e.getMessage());
                }
            }
            if (docIds.length() > 0) {
                knowledge.setDocumentId(docIds.toString());
                knowledge.setDatasetId(dsIds.toString());
                updateById(knowledge);
            }
        }
    }
    
    @Override
    public Map<Long, Map<String, Object>> checkDocuments(List<Long> knowledgeIds) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return result;
        }
        
        List<RepairKnowledge> knowledges = listByIds(knowledgeIds);
        for (RepairKnowledge knowledge : knowledges) {
            Map<String, Object> status = new HashMap<>();
            Long id = knowledge.getId();
            
            // 未审核通过或无documentId
            if (knowledge.getAuditStatus() == null || knowledge.getAuditStatus() != 1 
                    || knowledge.getDocumentId() == null || knowledge.getDocumentId().isEmpty()) {
                status.put("exists", false);
                status.put("reason", "NOT_UPLOADED");
                result.put(id, status);
                continue;
            }
            
            // 检查文档是否存在
            String datasetId = knowledge.getDatasetId();
            String documentId = knowledge.getDocumentId();
            
            if (datasetId == null || datasetId.isEmpty()) {
                status.put("exists", false);
                status.put("reason", "NO_DATASET");
                result.put(id, status);
                continue;
            }
            
            try {
                boolean exists = difyKnowledgeService.checkDocumentExists(datasetId, documentId);
                status.put("exists", exists);
                status.put("reason", exists ? "OK" : "NOT_FOUND");
            } catch (Exception e) {
                status.put("exists", false);
                status.put("reason", "ERROR: " + e.getMessage());
            }
            result.put(id, status);
        }
        
        return result;
    }

    @Override
    public List<RepairKnowledge> listByDocumentIds(List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(RepairKnowledge::getDocumentId, docIds)
                .eq(RepairKnowledge::getAuditStatus, 1)
                .list();
    }
}
