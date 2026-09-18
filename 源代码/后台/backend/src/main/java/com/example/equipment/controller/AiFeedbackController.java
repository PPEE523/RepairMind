package com.example.equipment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.equipment.aspect.OperLog;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.AiFeedback;
import com.example.equipment.service.AiFeedbackService;
import com.example.equipment.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiFeedbackController {

    @Resource
    private AiFeedbackService aiFeedbackService;

    @Resource
    private JwtUtils jwtUtils;

    /**
     * 提交AI答案反馈
     */
    @PostMapping("/feedback")
    @OperLog(module = "AI反馈", operType = "提交反馈", description = "提交AI答案修正反馈")
    public Result<Void> submitFeedback(
            @RequestBody AiFeedback feedback,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtils.getUserIdFromToken(token.replace("Bearer ", ""));
        
        feedback.setUserId(userId);
        feedback.setCreateTime(LocalDateTime.now());
        aiFeedbackService.save(feedback);

        log.info("用户 {} 提交AI反馈: rating={}", userId, feedback.getRating());
        return Result.success();
    }

    /**
     * 获取反馈统计
     */
    @GetMapping("/feedback/stats")
    public Result<Map<String, Object>> getFeedbackStats() {
        // 总反馈数
        Long totalFeedback = aiFeedbackService.count();

        // 正确数
        LambdaQueryWrapper<AiFeedback> correctWrapper = new LambdaQueryWrapper<>();
        correctWrapper.eq(AiFeedback::getRating, 1);
        Long correctCount = aiFeedbackService.count(correctWrapper);

        // 错误数
        LambdaQueryWrapper<AiFeedback> errorWrapper = new LambdaQueryWrapper<>();
        errorWrapper.eq(AiFeedback::getRating, 0);
        Long errorCount = aiFeedbackService.count(errorWrapper);

        // 正确率
        Double correctRate = totalFeedback > 0 ? (double) correctCount / totalFeedback * 100 : 0.0;

        // 反馈类型分布
        LambdaQueryWrapper<AiFeedback> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.select(AiFeedback::getFeedbackType);
        List<AiFeedback> feedbacks = aiFeedbackService.list(typeWrapper);
        Map<String, Long> typeDistribution = feedbacks.stream()
                .filter(f -> f.getFeedbackType() != null)
                .collect(Collectors.groupingBy(AiFeedback::getFeedbackType, Collectors.counting()));

        // 构建统计结果
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedback", totalFeedback);
        stats.put("correctCount", correctCount);
        stats.put("errorCount", errorCount);
        stats.put("correctRate", correctRate);
        stats.put("typeDistribution", typeDistribution);

        return Result.success(stats);
    }

    /**
     * 获取反馈列表
     */
    @GetMapping("/feedback/list")
    public Result<List<AiFeedback>> getFeedbackList(
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LambdaQueryWrapper<AiFeedback> wrapper = new LambdaQueryWrapper<>();
        if (rating != null) {
            wrapper.eq(AiFeedback::getRating, rating);
        }
        wrapper.orderByDesc(AiFeedback::getCreateTime);
        List<AiFeedback> feedbacks = aiFeedbackService.list(wrapper);
        return Result.success(feedbacks);
    }
}
