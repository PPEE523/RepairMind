package com.example.equipment.controller;

import com.example.equipment.aspect.OperLog;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.WorkExecution;
import com.example.equipment.entity.WorkExecutionMain;
import com.example.equipment.entity.WorkFlow;
import com.example.equipment.mapper.WorkExecutionMainMapper;
import com.example.equipment.service.WorkExecutionService;
import com.example.equipment.service.WorkFlowService;
import com.example.equipment.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflow/execution")
public class WorkExecutionController {

    @Resource
    private WorkExecutionService workExecutionService;

    @Resource
    private WorkExecutionMainMapper workExecutionMainMapper;

    @Resource
    private WorkFlowService workFlowService;

    @Resource
    private JwtUtils jwtUtils;

    /**
     * 开始执行作业流程
     */
    @PostMapping("/start")
    @OperLog(module = "作业执行", operType = "开始执行", description = "开始执行作业流程")
    public Result<WorkExecutionMain> startExecution(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        Long workflowId = Long.valueOf(request.get("workflowId").toString());
        Integer totalSteps = Integer.valueOf(request.get("totalSteps").toString());

        WorkExecutionMain main = workExecutionService.startExecution(workflowId, userId, totalSteps);
        return Result.success(main);
    }

    /**
     * 完成步骤
     */
    @PostMapping("/complete-step")
    @OperLog(module = "作业执行", operType = "完成步骤", description = "完成作业步骤")
    public Result<Void> completeStep(@RequestBody Map<String, Object> request) {
        Long executionId = Long.valueOf(request.get("executionId").toString());
        Integer stepIndex = Integer.valueOf(request.get("stepIndex").toString());
        String remark = request.get("remark") != null ? request.get("remark").toString() : null;

        workExecutionService.completeStep(executionId, stepIndex, remark);
        return Result.success();
    }

    /**
     * 标记步骤异常
     */
    @PostMapping("/mark-exception")
    @OperLog(module = "作业执行", operType = "标记异常", description = "标记作业步骤异常")
    public Result<Void> markException(@RequestBody Map<String, Object> request) {
        Long executionId = Long.valueOf(request.get("executionId").toString());
        Integer stepIndex = Integer.valueOf(request.get("stepIndex").toString());
        String remark = request.get("remark") != null ? request.get("remark").toString() : "";
        String imageUrls = request.get("imageUrls") != null ? request.get("imageUrls").toString() : "";

        workExecutionService.markException(executionId, stepIndex, remark, imageUrls);
        return Result.success();
    }

    /**
     * 获取执行详情
     */
    @GetMapping("/{executionId}/details")
    public Result<List<WorkExecution>> getExecutionDetails(@PathVariable Long executionId) {
        List<WorkExecution> details = workExecutionService.getExecutionDetails(executionId);
        return Result.success(details);
    }

    /**
     * 导出作业报告
     */
    @GetMapping("/{executionId}/export")
    public ResponseEntity<byte[]> exportReport(@PathVariable Long executionId) {
        // 获取执行主记录
        WorkExecutionMain main = workExecutionMainMapper.selectById(executionId);
        if (main == null) {
            return ResponseEntity.notFound().build();
        }

        // 获取作业流程
        WorkFlow workFlow = workFlowService.getById(main.getWorkflowId());
        
        // 获取执行详情
        List<WorkExecution> details = workExecutionService.getExecutionDetails(executionId);

        // 构建CSV报告
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM for Excel
        csv.append("设备检修作业报告\n\n");
        csv.append("作业流程ID,").append(main.getWorkflowId()).append("\n");
        csv.append("执行状态,").append(main.getStatus()).append("\n");
        csv.append("开始时间,").append(main.getStartTime()).append("\n");
        csv.append("结束时间,").append(main.getEndTime() != null ? main.getEndTime() : "进行中").append("\n");
        csv.append("总步骤数,").append(main.getTotalSteps()).append("\n");
        csv.append("已完成步骤,").append(main.getCompletedSteps()).append("\n\n");
        
        csv.append("步骤执行详情\n");
        csv.append("步骤序号,步骤状态,完成时间,备注\n");
        
        for (WorkExecution detail : details) {
            csv.append(detail.getStepIndex() + 1).append(",");
            csv.append(detail.getStepStatus()).append(",");
            csv.append(detail.getCompletedTime() != null ? detail.getCompletedTime() : "").append(",");
            csv.append(detail.getRemark() != null ? detail.getRemark() : "").append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = "作业报告_" + executionId + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }
}
