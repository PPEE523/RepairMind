package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.WorkSopFlow;
import com.example.equipment.exception.BusinessException;
import com.example.equipment.mapper.WorkSopFlowMapper;
import com.example.equipment.service.DifyService;
import com.example.equipment.service.WorkSopFlowService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WorkSopFlowServiceImpl extends ServiceImpl<WorkSopFlowMapper, WorkSopFlow>
        implements WorkSopFlowService {

    @Resource
    private DifyService difyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<WorkSopFlow> listActive() {
        LambdaQueryWrapper<WorkSopFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkSopFlow::getStatus, "active")
               .orderByDesc(WorkSopFlow::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public WorkSopFlow generateSopFlow(String documentId, String documentName,
                                        String datasetId, Long userId) {
        log.info("开始生成SOP流程: documentName={}, userId={}", documentName, userId);

        try {
            // 1. 调用Dify SOP Agent生成流程
            String sopResult = difyService.generateSopFromDocument(documentName, userId);

            // 2. 解析返回的步骤JSON
            List<Map<String, Object>> steps;
            try {
                // 尝试从结果中提取JSON数组
                String stepJson = extractJsonArray(sopResult);
                steps = objectMapper.readValue(stepJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                log.warn("解析SOP结果JSON失败，使用默认格式: {}", e.getMessage());
                // 降级：将整个结果作为单个步骤
                steps = List.of(Map.of(
                    "title", "AI生成流程",
                    "description", sopResult,
                    "acceptance_criteria", "",
                    "tools", List.of(),
                    "safety_notes", ""
                ));
            }

            // 3. 构建实体并保存
            WorkSopFlow sopFlow = new WorkSopFlow();
            sopFlow.setName(documentName);
            sopFlow.setSourceDocumentId(documentId);
            sopFlow.setSourceDocumentName(documentName);
            sopFlow.setDatasetId(datasetId);
            sopFlow.setStepJson(objectMapper.writeValueAsString(steps));
            sopFlow.setTotalSteps(steps.size());
            sopFlow.setStatus("active");
            sopFlow.setCreateUser(userId);
            sopFlow.setCreateTime(LocalDateTime.now());
            sopFlow.setUpdateTime(LocalDateTime.now());

            save(sopFlow);
            log.info("SOP流程生成成功: id={}, name={}, totalSteps={}", sopFlow.getId(), sopFlow.getName(), sopFlow.getTotalSteps());

            return sopFlow;

        } catch (Exception e) {
            log.error("生成SOP流程失败", e);
            throw new BusinessException("生成SOP流程失败: " + e.getMessage());
        }
    }

    @Override
    public void testSopStep(Long sopFlowId, Integer stepIndex, String userInput,
                            String imageUrl, Long userId, DifyService.StreamCallback callback) {
        WorkSopFlow sopFlow = getById(sopFlowId);
        if (sopFlow == null) {
            callback.onError("SOP流程不存在");
            return;
        }

        // 提取当前步骤的JSON
        String currentStepJson = "{}";
        try {
            List<Map<String, Object>> steps = objectMapper.readValue(sopFlow.getStepJson(),
                    new TypeReference<List<Map<String, Object>>>() {});
            if (stepIndex < steps.size()) {
                currentStepJson = objectMapper.writeValueAsString(steps.get(stepIndex));
            }
        } catch (Exception e) {
            log.warn("解析步骤JSON失败，使用原始步骤数据: {}", e.getMessage());
            currentStepJson = sopFlow.getStepJson();
        }

        log.info("开始SOP流程测试: sopFlowId={}, stepIndex={}, userId={}", sopFlowId, stepIndex, userId);
        difyService.testSopStep(currentStepJson, stepIndex, userInput, imageUrl, userId, callback);
    }

    @Override
    public List<Map<String, Object>> parseSteps(Long sopFlowId) {
        WorkSopFlow sopFlow = getById(sopFlowId);
        if (sopFlow == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(sopFlow.getStepJson(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析步骤JSON失败: sopFlowId={}", sopFlowId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从AI返回结果中提取JSON数组
     */
    private String extractJsonArray(String text) {
        if (text == null || text.isBlank()) {
            return "[]";
        }
        // 移除可能的markdown代码块标记
        String cleaned = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        // 如果没有数组，包装成单个元素数组
        return "[" + cleaned + "]";
    }
}
