package com.example.equipment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.equipment.aspect.OperLog;
import com.example.equipment.config.ChatProperties;
import com.example.equipment.dto.DashboardDTO;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.AiFeedback;
import com.example.equipment.entity.ChatHistory;
import com.example.equipment.entity.RepairKnowledge;
import com.example.equipment.entity.SysConfig;
import com.example.equipment.entity.SysModelConfig;
import com.example.equipment.entity.SysOperLog;
import com.example.equipment.service.AiFeedbackService;
import com.example.equipment.service.ChatHistoryService;
import com.example.equipment.service.RepairKnowledgeService;
import com.example.equipment.service.SysConfigService;
import com.example.equipment.service.SysModelConfigService;
import com.example.equipment.service.SysOperLogService;
import com.example.equipment.service.SysUserService;
import com.example.equipment.utils.JwtUtils;
import com.example.equipment.entity.SysUser;
import com.example.equipment.task.AuditTimeoutTask;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/system")
@PreAuthorize("hasRole('ADMIN')")
public class SystemController {

    @Resource
    private SysOperLogService sysOperLogService;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private SysModelConfigService sysModelConfigService;

    @Resource
    private RepairKnowledgeService repairKnowledgeService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AiFeedbackService aiFeedbackService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private ChatProperties chatProperties;

    @Resource
    private AuditTimeoutTask auditTimeoutTask;

    /**
     * 数据统计看板
     */
    @GetMapping("/dashboard")
    public Result<DashboardDTO> getDashboard(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));

        // 获取时间范围
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX);

        // 1. 文档总量（已审核通过的案例数）
        LambdaQueryWrapper<RepairKnowledge> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(RepairKnowledge::getAuditStatus, 1);
        Long totalDocuments = repairKnowledgeService.count(docWrapper);

        // 2. 检索次数统计（基于chat_history表）
        LambdaQueryWrapper<ChatHistory> todaySearchWrapper = new LambdaQueryWrapper<>();
        todaySearchWrapper.ge(ChatHistory::getCreateTime, todayStart);
        Long todaySearchCount = chatHistoryService.count(todaySearchWrapper);

        LambdaQueryWrapper<ChatHistory> monthSearchWrapper = new LambdaQueryWrapper<>();
        monthSearchWrapper.ge(ChatHistory::getCreateTime, monthStart);
        Long monthSearchCount = chatHistoryService.count(monthSearchWrapper);

        Long totalSearchCount = chatHistoryService.count();

        // 3. 案例新增数量统计
        LambdaQueryWrapper<RepairKnowledge> todayCaseWrapper = new LambdaQueryWrapper<>();
        todayCaseWrapper.ge(RepairKnowledge::getCreateTime, todayStart);
        Long todayNewCases = repairKnowledgeService.count(todayCaseWrapper);

        LambdaQueryWrapper<RepairKnowledge> monthCaseWrapper = new LambdaQueryWrapper<>();
        monthCaseWrapper.ge(RepairKnowledge::getCreateTime, monthStart);
        Long monthNewCases = repairKnowledgeService.count(monthCaseWrapper);

        // 4. 待审核案例数
        LambdaQueryWrapper<RepairKnowledge> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(RepairKnowledge::getAuditStatus, 0);
        Long pendingAuditCount = repairKnowledgeService.count(pendingWrapper);

        // 5. AI反馈统计
        LambdaQueryWrapper<AiFeedback> feedbackWrapper = new LambdaQueryWrapper<>();
        Long totalFeedback = aiFeedbackService.count(feedbackWrapper);

        LambdaQueryWrapper<AiFeedback> correctWrapper = new LambdaQueryWrapper<>();
        correctWrapper.eq(AiFeedback::getRating, 1);
        Long correctCount = aiFeedbackService.count(correctWrapper);

        LambdaQueryWrapper<AiFeedback> errorWrapper = new LambdaQueryWrapper<>();
        errorWrapper.eq(AiFeedback::getRating, 0);
        Long errorCount = aiFeedbackService.count(errorWrapper);

        Double correctRate = totalFeedback > 0 ? (double) correctCount / totalFeedback * 100 : 0.0;

        DashboardDTO.AiFeedbackStats aiFeedbackStats = DashboardDTO.AiFeedbackStats.builder()
                .totalFeedback(totalFeedback)
                .correctCount(correctCount)
                .errorCount(errorCount)
                .correctRate(correctRate)
                .build();

        // 7. 检索趋势（最近7天，基于chat_history表）
        List<DashboardDTO.DailyStats> searchTrend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            LambdaQueryWrapper<ChatHistory> dayWrapper = new LambdaQueryWrapper<>();
            dayWrapper.ge(ChatHistory::getCreateTime, dayStart)
                    .le(ChatHistory::getCreateTime, dayEnd);
            Long dayCount = chatHistoryService.count(dayWrapper);

            searchTrend.add(DashboardDTO.DailyStats.builder()
                    .date(date.format(formatter))
                    .count(dayCount)
                    .build());
        }

        DashboardDTO dashboard = DashboardDTO.builder()
                .totalDocuments(totalDocuments)
                .todaySearchCount(todaySearchCount)
                .monthSearchCount(monthSearchCount)
                .totalSearchCount(totalSearchCount)
                .todayNewCases(todayNewCases)
                .monthNewCases(monthNewCases)
                .pendingAuditCount(pendingAuditCount)
                .aiFeedbackStats(aiFeedbackStats)
                .searchTrend(searchTrend)
                .build();

        return Result.success(dashboard);
    }

    @GetMapping("/log")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysOperLog::getUserId, userId);
        }
        wrapper.orderByDesc(SysOperLog::getOperTime);
        
        // 使用MyBatis-Plus分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysOperLog> page = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysOperLog> resultPage = 
            sysOperLogService.page(page, wrapper);

        // 填充用户昵称
        List<SysOperLog> records = resultPage.getRecords();
        if (!records.isEmpty()) {
            List<Long> userIds = records.stream()
                    .map(SysOperLog::getUserId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            Map<Long, String> userNicknameMap = sysUserService.listByIds(userIds)
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(SysUser::getId, SysUser::getNickname));
            records.forEach(log -> {
                if (log.getUserId() != null) {
                    log.setNickname(userNicknameMap.getOrDefault(log.getUserId(), String.valueOf(log.getUserId())));
                }
            });
        }

        return Result.success(Map.of(
            "records", resultPage.getRecords(),
            "total", resultPage.getTotal(),
            "pageNum", resultPage.getCurrent(),
            "pageSize", resultPage.getSize()
        ));
    }

    /**
     * 通知列表接口 - 根据用户角色返回不同的通知
     * 管理员/资深工程师：超时未审核的案例
     * 普通用户：自己上传案例的审核结果变化
     */
    @GetMapping("/notification/list")
    public Result<Map<String, Object>> getNotificationList(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);

        if (user == null) {
            return Result.error(401, "用户不存在");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        boolean isAuditor = (user.getRoleId() != null && (user.getRoleId() == 1 || user.getRoleId() == 2));

        if (isAuditor) {
            // 审核人：返回超时待审核案例
            List<RepairKnowledge> timeoutList = auditTimeoutTask.getTimeoutList();
            for (RepairKnowledge k : timeoutList) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "audit-timeout-" + k.getId());
                item.put("type", "audit_timeout");
                item.put("title", "案例审核超时");
                item.put("content", "案例《" + truncate(k.getTitle(), 20) + "》待审核已超过48小时，请尽快处理");
                item.put("time", k.getCreateTime());
                item.put("link", "/audit");
                items.add(item);
            }
        } else {
            // 普通用户：返回自己上传案例最近的审核结果
            LambdaQueryWrapper<RepairKnowledge> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RepairKnowledge::getUploadUserId, userId)
                    .in(RepairKnowledge::getAuditStatus, 1, 2)  // 已通过或已驳回
                    .ge(RepairKnowledge::getUpdateTime, LocalDateTime.now().minusDays(7))  // 最近7天
                    .orderByDesc(RepairKnowledge::getUpdateTime)
                    .last("LIMIT 10");
            List<RepairKnowledge> resultList = repairKnowledgeService.list(wrapper);

            for (RepairKnowledge k : resultList) {
                boolean passed = k.getAuditStatus() == 1;
                Map<String, Object> item = new HashMap<>();
                item.put("id", "audit-result-" + k.getId());
                item.put("type", passed ? "audit_passed" : "audit_rejected");
                item.put("title", passed ? "案例审核通过" : "案例审核驳回");
                String content = "您上传的案例《" + truncate(k.getTitle(), 20) + "》已" + (passed ? "审核通过" : "被驳回");
                if (!passed && k.getAuditRemark() != null && !k.getAuditRemark().isEmpty()) {
                    content += "，理由：" + truncate(k.getAuditRemark(), 30);
                }
                item.put("content", content);
                item.put("time", k.getUpdateTime());
                item.put("link", "/case");
                items.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", items.size());
        return Result.success(result);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    @GetMapping("/config")
    public Result<List<SysConfig>> getConfigs() {
        List<SysConfig> configs = sysConfigService.list();
        return Result.success(configs);
    }

    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody SysConfig config) {
        sysConfigService.saveOrUpdate(config);
        return Result.success();
    }

    /**
     * 获取上传开关配置
     * 从 sys_config 表读取，若无记录则返回 yml 默认值
     */
    @GetMapping("/upload-config")
    public Result<Map<String, Object>> getUploadConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        // 读取开关配置，优先从 sys_config 表读取
        config.put("enableImageUpload", getSysConfigBool("chat.enable.image.upload", chatProperties.isEnableImageUpload()));
        config.put("enableVideoUpload", getSysConfigBool("chat.enable.video.upload", chatProperties.isEnableVideoUpload()));
        config.put("enableAudioUpload", getSysConfigBool("chat.enable.audio.upload", chatProperties.isEnableAudioUpload()));
        config.put("enableFileUpload", getSysConfigBool("chat.enable.file.upload", chatProperties.isEnableFileUpload()));
        config.put("allowedImageTypes", getSysConfigValue("chat.allowed.image.types", chatProperties.getAllowedImageTypes()));
        config.put("allowedVideoTypes", getSysConfigValue("chat.allowed.video.types", chatProperties.getAllowedVideoTypes()));
        config.put("allowedAudioTypes", getSysConfigValue("chat.allowed.audio.types", chatProperties.getAllowedAudioTypes()));
        config.put("allowedFileTypes", getSysConfigValue("chat.allowed.file.types", chatProperties.getAllowedFileTypes()));
        config.put("maxFileSizeMb", chatProperties.getMaxFileSizeMb());
        return Result.success(config);
    }

    /**
     * 保存上传开关配置
     * 写入 sys_config 表，同时更新 ChatProperties 运行时值
     */
    @PostMapping("/upload-config")
    @OperLog(module = "系统配置", operType = "修改", description = "修改上传开关配置")
    public Result<Void> saveUploadConfig(@RequestBody Map<String, Object> configData) {
        // 保存到 sys_config 表
        saveSysConfigItem("chat.enable.image.upload", String.valueOf(configData.get("enableImageUpload")), "图片上传开关");
        saveSysConfigItem("chat.enable.video.upload", String.valueOf(configData.get("enableVideoUpload")), "视频上传开关");
        saveSysConfigItem("chat.enable.audio.upload", String.valueOf(configData.get("enableAudioUpload")), "音频上传开关");
        saveSysConfigItem("chat.enable.file.upload", String.valueOf(configData.get("enableFileUpload")), "文件上传开关");

        saveSysConfigItem("chat.allowed.image.types", String.valueOf(configData.get("allowedImageTypes")), "允许的图片格式");
        saveSysConfigItem("chat.allowed.video.types", String.valueOf(configData.get("allowedVideoTypes")), "允许的视频格式");
        saveSysConfigItem("chat.allowed.audio.types", String.valueOf(configData.get("allowedAudioTypes")), "允许的音频格式");
        saveSysConfigItem("chat.allowed.file.types", String.valueOf(configData.get("allowedFileTypes")), "允许的文件格式");

        // 同步更新 ChatProperties 运行时值，立即生效
        chatProperties.setEnableImageUpload(getSysConfigBool("chat.enable.image.upload", true));
        chatProperties.setEnableVideoUpload(getSysConfigBool("chat.enable.video.upload", true));
        chatProperties.setEnableAudioUpload(getSysConfigBool("chat.enable.audio.upload", true));
        chatProperties.setEnableFileUpload(getSysConfigBool("chat.enable.file.upload", true));

        chatProperties.setAllowedImageTypes(getSysConfigValue("chat.allowed.image.types", chatProperties.getAllowedImageTypes()));
        chatProperties.setAllowedVideoTypes(getSysConfigValue("chat.allowed.video.types", chatProperties.getAllowedVideoTypes()));
        chatProperties.setAllowedAudioTypes(getSysConfigValue("chat.allowed.audio.types", chatProperties.getAllowedAudioTypes()));
        chatProperties.setAllowedFileTypes(getSysConfigValue("chat.allowed.file.types", chatProperties.getAllowedFileTypes()));

        log.info("上传开关配置已更新");
        return Result.success();
    }

    /**
     * 从 sys_config 表读取配置值，若无则返回默认值
     */
    private String getSysConfigValue(String key, String defaultValue) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig config = sysConfigService.getOne(wrapper);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    /**
     * 从 sys_config 表读取布尔配置值
     */
    private boolean getSysConfigBool(String key, boolean defaultValue) {
        String value = getSysConfigValue(key, null);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value);
    }

    /**
     * 保存或更新 sys_config 记录
     */
    private void saveSysConfigItem(String key, String value, String remark) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig existing = sysConfigService.getOne(wrapper);
        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdateTime(LocalDateTime.now());
            sysConfigService.updateById(existing);
        } else {
            SysConfig newConfig = new SysConfig();
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(value);
            newConfig.setRemark(remark);
            newConfig.setUpdateTime(LocalDateTime.now());
            sysConfigService.save(newConfig);
        }
    }

    // ========== Dify配置管理接口 ==========

    /**
     * 获取Dify配置列表
     */
    @GetMapping("/model")
    public Result<List<SysModelConfig>> getModelConfigs() {
        List<SysModelConfig> configs = sysModelConfigService.list();
        return Result.success(configs);
    }

    /**
     * 获取当前启用的Dify配置
     */
    @GetMapping("/model/active")
    public Result<SysModelConfig> getActiveModelConfig() {
        SysModelConfig config = sysModelConfigService.getActiveConfig();
        return Result.success(config);
    }

    /**
     * 新增/修改Dify配置
     */
    @PostMapping("/model")
    @OperLog(module = "系统配置", operType = "新增/修改", description = "配置Dify平台连接信息")
    public Result<Void> saveModelConfig(@RequestBody SysModelConfig config) {
        sysModelConfigService.saveOrUpdate(config);
        log.info("Dify配置保存成功: id={}", config.getId());
        return Result.success();
    }

    /**
     * 切换Dify配置启用状态
     */
    @PutMapping("/model/{id}/toggle/{enable}")
    @OperLog(module = "系统配置", operType = "状态变更", description = "切换Dify配置启用状态")
    public Result<Void> toggleModelConfig(@PathVariable Long id, @PathVariable Integer enable) {
        sysModelConfigService.toggleEnable(id, enable);
        log.info("Dify配置状态变更: id={}, enable={}", id, enable);
        return Result.success();
    }

    /**
     * 删除Dify配置
     */
    @DeleteMapping("/model/{id}")
    @OperLog(module = "系统配置", operType = "删除", description = "删除Dify配置")
    public Result<Void> deleteModelConfig(@PathVariable Long id) {
        sysModelConfigService.removeById(id);
        log.info("Dify配置删除成功: id={}", id);
        return Result.success();
    }

    /**
     * 删除单条操作日志（仅管理员）
     */
    @DeleteMapping("/log/{id}")
    @OperLog(module = "系统配置", operType = "删除", description = "删除操作日志")
    public Result<Void> deleteLog(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getRoleId() == null || user.getRoleId() != 1) {
            return Result.error(403, "权限不足，仅管理员可删除操作日志");
        }
        sysOperLogService.removeById(id);
        log.info("管理员 {} 删除了操作日志 id={}", userId, id);
        return Result.success();
    }

    /**
     * 批量删除操作日志（仅管理员）
     */
    @DeleteMapping("/log/batch")
    @OperLog(module = "系统配置", operType = "删除", description = "批量删除操作日志")
    public Result<Void> deleteLogs(@RequestBody List<Long> ids, @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getRoleId() == null || user.getRoleId() != 1) {
            return Result.error(403, "权限不足，仅管理员可删除操作日志");
        }
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的日志");
        }
        sysOperLogService.removeByIds(ids);
        log.info("管理员 {} 批量删除了 {} 条操作日志", userId, ids.size());
        return Result.success();
    }

    /**
     * 清空所有操作日志（仅管理员）
     */
    @DeleteMapping("/log/clear")
    @OperLog(module = "系统配置", operType = "删除", description = "清空所有操作日志")
    public Result<Void> clearLogs(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getRoleId() == null || user.getRoleId() != 1) {
            return Result.error(403, "权限不足，仅管理员可清空操作日志");
        }
        sysOperLogService.remove(new LambdaQueryWrapper<>());
        log.info("管理员 {} 清空了所有操作日志", userId);
        return Result.success();
    }

    /**
     * 导出操作日志
     */
    @GetMapping("/log/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(SysOperLog::getUserId, userId);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SysOperLog::getOperTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SysOperLog::getOperTime, LocalDateTime.parse(endTime));
        }
        wrapper.orderByDesc(SysOperLog::getOperTime);

        List<SysOperLog> logs = sysOperLogService.list(wrapper);

        // 填充用户昵称
        if (!logs.isEmpty()) {
            List<Long> userIds = logs.stream()
                    .map(SysOperLog::getUserId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            Map<Long, String> userNicknameMap = sysUserService.listByIds(userIds)
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(SysUser::getId, SysUser::getNickname));
            logs.forEach(log -> {
                if (log.getUserId() != null) {
                    log.setNickname(userNicknameMap.getOrDefault(log.getUserId(), String.valueOf(log.getUserId())));
                }
            });
        }

        // 构建CSV内容
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM for Excel
        csv.append("ID,用户,操作类型,操作内容,操作时间,IP地址\n");
        for (SysOperLog logItem : logs) {
            csv.append(logItem.getId()).append(",");
            csv.append(logItem.getNickname() != null ? logItem.getNickname() : logItem.getUserId()).append(",");
            csv.append(logItem.getOperType()).append(",");
            csv.append("\"").append(logItem.getContent() != null ? logItem.getContent().replace("\"", "\"\"") : "").append("\",");
            csv.append(logItem.getOperTime()).append(",");
            csv.append(logItem.getIpAddress()).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String fileName = "操作日志_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }
}
