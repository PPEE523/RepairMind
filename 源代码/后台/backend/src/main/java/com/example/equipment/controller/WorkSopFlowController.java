package com.example.equipment.controller;

import com.example.equipment.aspect.OperLog;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.WorkSopFlow;
import com.example.equipment.service.WorkSopFlowService;
import com.example.equipment.service.DifyService;
import com.example.equipment.utils.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflow/sop")
public class WorkSopFlowController {

    @Resource
    private WorkSopFlowService workSopFlowService;

    @Resource
    private DifyService difyService;

    @Resource
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成SOP流程 - 从知识库文档生成
     */
    @PostMapping("/generate")
    @OperLog(module = "SOP流程管理", operType = "生成SOP", description = "从知识库文档生成SOP流程")
    public Result<WorkSopFlow> generateSop(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String documentId = request.get("documentId");
        String documentName = request.get("documentName");
        String datasetId = request.get("datasetId");

        if (documentId == null || documentName == null || datasetId == null) {
            return Result.error(400, "参数不完整：需要 documentId, documentName, datasetId");
        }

        String token = httpRequest.getHeader("Authorization");
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        WorkSopFlow sopFlow = workSopFlowService.generateSopFlow(documentId, documentName, datasetId, userId);
        return Result.success(sopFlow);
    }

    /**
     * 获取SOP流程列表
     */
    @GetMapping("/list")
    public Result<List<WorkSopFlow>> listSopFlows() {
        List<WorkSopFlow> flows = workSopFlowService.listActive();
        return Result.success(flows);
    }

    /**
     * 获取SOP流程详情（含解析后的步骤）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getSopFlowDetail(@PathVariable Long id) {
        WorkSopFlow sopFlow = workSopFlowService.getById(id);
        if (sopFlow == null) {
            return Result.error(404, "SOP流程不存在");
        }
        List<Map<String, Object>> steps = workSopFlowService.parseSteps(id);

        Map<String, Object> result = new HashMap<>();
        result.put("flow", sopFlow);
        result.put("steps", steps);
        return Result.success(result);
    }

    /**
     * 修改SOP流程（编辑步骤内容）
     */
    @PutMapping("/{id}")
    @OperLog(module = "SOP流程管理", operType = "修改SOP", description = "修改SOP流程步骤")
    public Result<WorkSopFlow> updateSopFlow(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        WorkSopFlow sopFlow = workSopFlowService.getById(id);
        if (sopFlow == null) {
            return Result.error(404, "SOP流程不存在");
        }

        if (request.containsKey("name")) {
            sopFlow.setName((String) request.get("name"));
        }
        if (request.containsKey("stepJson")) {
            sopFlow.setStepJson((String) request.get("stepJson"));
            // 更新总步骤数
            try {
                List<Map<String, Object>> steps = workSopFlowService.parseSteps(id);
                sopFlow.setTotalSteps(steps.size());
            } catch (Exception e) {
                log.warn("解析步骤JSON失败: {}", e.getMessage());
            }
        }
        sopFlow.setUpdateTime(java.time.LocalDateTime.now());
        workSopFlowService.updateById(sopFlow);

        return Result.success(sopFlow);
    }

    /**
     * 删除SOP流程（软删除：status → archived）
     */
    @DeleteMapping("/{id}")
    @OperLog(module = "SOP流程管理", operType = "删除SOP", description = "删除SOP流程")
    public Result<Void> deleteSopFlow(@PathVariable Long id) {
        WorkSopFlow sopFlow = workSopFlowService.getById(id);
        if (sopFlow == null) {
            return Result.error(404, "SOP流程不存在");
        }
        sopFlow.setStatus("archived");
        sopFlow.setUpdateTime(java.time.LocalDateTime.now());
        workSopFlowService.updateById(sopFlow);
        return Result.success();
    }

    /**
     * SOP流程测试 - 同步请求返回结果JSON
     */
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testSopFlowSync(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        Integer stepIndex = request.get("stepIndex") != null
                ? ((Number) request.get("stepIndex")).intValue() : 0;
        String userInput = (String) request.getOrDefault("userInput", "");
        String imageUrl = (String) request.getOrDefault("imageUrl", "");
        String token = httpRequest.getHeader("Authorization");
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        // 获取流程步骤JSON
        WorkSopFlow sopFlow = workSopFlowService.getById(id);
        if (sopFlow == null) {
            return Result.error(404, "SOP流程不存在");
        }
        // 只提取当前步骤内容，而非全部步骤
        String currentStepJson = "{}";
        try {
            List<Map<String, Object>> steps = objectMapper.readValue(sopFlow.getStepJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                currentStepJson = objectMapper.writeValueAsString(steps.get(stepIndex));
            }
        } catch (Exception e) {
            log.warn("解析步骤JSON失败，使用原始数据: {}", e.getMessage());
            currentStepJson = sopFlow.getStepJson();
        }

        // 调用同步Dify测试接口（仅传入当前步骤）
        log.info("发送给Dify的当前步骤内容: stepIndex={}, stepJson={}", stepIndex,
                currentStepJson.length() > 300 ? currentStepJson.substring(0, 300) + "..." : currentStepJson);
        String fullAnswer = difyService.testSopStepSync(currentStepJson, stepIndex, userInput, imageUrl, userId);
        
        if (fullAnswer == null || fullAnswer.isEmpty()) {
            return Result.error(500, "测试失败");
        }

        log.info("SOP测试完成: fullAnswer={}", fullAnswer.length() > 200 ? fullAnswer.substring(0, 200) : fullAnswer);

        // 解析AI返回结果
        boolean passed = false;
        String summary = "", reason = "", suggestion = "";
        try {
            JsonNode result = objectMapper.readTree(fullAnswer);
            passed = result.path("passed").asBoolean(false);
            summary = result.path("summary").asText("");
            reason = result.path("reason").asText("");
            suggestion = result.path("suggestion").asText("");
            log.info("JSON解析成功: passed={}", passed);
        } catch (Exception e) {
            log.warn("JSON解析失败，降级为文本匹配");
            passed = fullAnswer.contains("\"passed\":true") || fullAnswer.contains("[PASS]");
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("passed", passed);
        resultMap.put("summary", summary);
        resultMap.put("reason", reason);
        resultMap.put("suggestion", suggestion);
        return Result.success(resultMap);
    }
}
