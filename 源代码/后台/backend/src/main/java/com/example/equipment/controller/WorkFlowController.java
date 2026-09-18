package com.example.equipment.controller;

import com.example.equipment.aspect.OperLog;
import com.example.equipment.dto.ExceptionReportRequest;
import com.example.equipment.dto.Result;
import com.example.equipment.dto.SearchRequest;
import com.example.equipment.dto.SearchResponse;
import com.example.equipment.entity.WorkFlow;
import com.example.equipment.enums.DifyAgentType;
import com.example.equipment.service.DifyService;
import com.example.equipment.service.WorkFlowService;
import com.example.equipment.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/workflow")
public class WorkFlowController {

    @Resource
    private WorkFlowService workFlowService;

    @Resource
    private DifyService difyService;

    @Resource
    private JwtUtils jwtUtils;

    @GetMapping("/get")
    public Result<WorkFlow> getWorkFlow(
            @RequestParam String equipModel,
            @RequestParam String repairLevel) {
        WorkFlow workFlow = workFlowService.getLatestByEquipModel(equipModel, repairLevel);
        return Result.success(workFlow);
    }

    @PostMapping("/save")
    @OperLog(module = "作业流程", operType = "保存", description = "保存作业流程")
    public Result<Void> saveWorkFlow(
            @RequestBody WorkFlow workFlow,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        // 获取最新版本号
        WorkFlow latest = workFlowService.getLatestByEquipModel(
                workFlow.getEquipModel(), workFlow.getRepairLevel());
        int newVersion = latest != null ? latest.getVersion() + 1 : 1;

        workFlow.setVersion(newVersion);
        workFlow.setCreateUser(userId);
        workFlow.setCreateTime(LocalDateTime.now());
        workFlowService.save(workFlow);

        log.info("保存作业流程: equipModel={}, repairLevel={}, version={}",
                workFlow.getEquipModel(), workFlow.getRepairLevel(), newVersion);
        return Result.success();
    }

    /**
     * 获取版本历史
     */
    @GetMapping("/versions")
    public Result<List<WorkFlow>> getVersionHistory(
            @RequestParam String equipModel,
            @RequestParam String repairLevel) {
        List<WorkFlow> versions = workFlowService.getVersionHistory(equipModel, repairLevel);
        return Result.success(versions);
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/rollback")
    @OperLog(module = "作业流程", operType = "回滚", description = "回滚作业流程版本")
    public Result<Void> rollback(
            @RequestParam Long workflowId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        workFlowService.rollbackToVersion(workflowId, userId);
        return Result.success();
    }

    /**
     * 检修异常上报
     * 收到异常后调用大模型生成临时应急处置方案
     */
    @PostMapping("/exception")
    public Result<SearchResponse> reportException(
            @Valid @RequestBody ExceptionReportRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        log.info("检修异常上报: userId={}, equipModel={}, description={}", 
                userId, request.getEquipModel(), request.getExceptionDescription());

        // 构建检索请求，调用Dify生成应急方案
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery("检修异常紧急处理：" + request.getExceptionDescription());
        searchRequest.setEquipModel(request.getEquipModel());

        // 调用Dify生成应急处置方案
        SearchResponse response = difyService.search(searchRequest, userId, DifyAgentType.ZYZY);

        log.info("应急方案生成完成: userId={}, answerLength={}", 
                userId, response.getAnswer() != null ? response.getAnswer().length() : 0);

        return Result.success(response);
    }
}
