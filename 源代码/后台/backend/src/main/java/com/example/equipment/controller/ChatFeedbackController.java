package com.example.equipment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.equipment.dto.ChatFeedbackStatsVO;
import com.example.equipment.dto.ChatFeedbackVO;
import com.example.equipment.dto.Result;
import com.example.equipment.entity.ChatDocumentResource;
import com.example.equipment.entity.ChatFeedback;
import com.example.equipment.entity.ChatHistory;
import com.example.equipment.entity.ChatSegment;
import com.example.equipment.mapper.ChatDocumentResourceMapper;
import com.example.equipment.mapper.ChatFeedbackMapper;
import com.example.equipment.mapper.ChatHistoryMapper;
import com.example.equipment.mapper.ChatSegmentMapper;
import com.example.equipment.service.DifyKnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI修正统计 - 对话反馈管理Controller
 * 基于chat_feedback表，独立于旧的AiFeedbackController
 */
@Slf4j
@RestController
@RequestMapping("/api/chat-feedback")
public class ChatFeedbackController {

    @Resource
    private ChatFeedbackMapper chatFeedbackMapper;

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private ChatDocumentResourceMapper chatDocumentResourceMapper;

    @Resource
    private ChatSegmentMapper chatSegmentMapper;

    @Resource
    private DifyKnowledgeService difyKnowledgeService;

    /**
     * 获取反馈统计
     */
    @GetMapping("/stats")
    public Result<ChatFeedbackStatsVO> getStats() {
        ChatFeedbackStatsVO statsVO = new ChatFeedbackStatsVO();

        Long correctCount = chatFeedbackMapper.selectCount(
                new LambdaQueryWrapper<ChatFeedback>().eq(ChatFeedback::getFeedbackStatus, "correct")
        );
        Long errorCount = chatFeedbackMapper.selectCount(
                new LambdaQueryWrapper<ChatFeedback>().eq(ChatFeedback::getFeedbackStatus, "incorrect")
        );
        Long processedCount = chatFeedbackMapper.selectCount(
                new LambdaQueryWrapper<ChatFeedback>().eq(ChatFeedback::getFeedbackStatus, "processed")
        );
        Long pendingCount = chatFeedbackMapper.selectCount(
                new LambdaQueryWrapper<ChatFeedback>().eq(ChatFeedback::getFeedbackStatus, "pending")
        );

        statsVO.setCorrectCount(correctCount);
        statsVO.setErrorCount(errorCount);
        statsVO.setProcessedCount(processedCount);
        statsVO.setPendingCount(pendingCount);
        statsVO.setTotalFeedback(correctCount + errorCount + processedCount);

        double correctRate = statsVO.getTotalFeedback() > 0
                ? (double) (correctCount + processedCount) / statsVO.getTotalFeedback() * 100 : 0.0;
        statsVO.setCorrectRate(correctRate);

        // 按日统计最近7天
        List<ChatFeedbackStatsVO.DailyStat> dailyStats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            ChatFeedbackStatsVO.DailyStat dailyStat = new ChatFeedbackStatsVO.DailyStat();
            dailyStat.setDate(date.format(formatter));
            dailyStat.setCorrectCount(chatFeedbackMapper.selectCount(
                    new LambdaQueryWrapper<ChatFeedback>()
                            .eq(ChatFeedback::getFeedbackStatus, "correct")
                            .ge(ChatFeedback::getFeedbackTime, dayStart)
                            .lt(ChatFeedback::getFeedbackTime, dayEnd)
            ));
            dailyStat.setErrorCount(chatFeedbackMapper.selectCount(
                    new LambdaQueryWrapper<ChatFeedback>()
                            .eq(ChatFeedback::getFeedbackStatus, "incorrect")
                            .ge(ChatFeedback::getFeedbackTime, dayStart)
                            .lt(ChatFeedback::getFeedbackTime, dayEnd)
            ));
            dailyStat.setProcessedCount(chatFeedbackMapper.selectCount(
                    new LambdaQueryWrapper<ChatFeedback>()
                            .eq(ChatFeedback::getFeedbackStatus, "processed")
                            .ge(ChatFeedback::getFeedbackTime, dayStart)
                            .lt(ChatFeedback::getFeedbackTime, dayEnd)
            ));
            dailyStat.setPendingCount(chatFeedbackMapper.selectCount(
                    new LambdaQueryWrapper<ChatFeedback>()
                            .eq(ChatFeedback::getFeedbackStatus, "pending")
                            .ge(ChatFeedback::getFeedbackTime, dayStart)
                            .lt(ChatFeedback::getFeedbackTime, dayEnd)
            ));
            dailyStats.add(dailyStat);
        }
        statsVO.setDailyStats(dailyStats);

        return Result.success(statsVO);
    }

    /**
     * 获取反馈列表
     */
    @GetMapping("/list")
    public Result<List<ChatFeedbackVO>> getList(
            @RequestParam(required = false) String feedbackStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<ChatFeedback> wrapper = new LambdaQueryWrapper<>();
        if (feedbackStatus != null && !feedbackStatus.isEmpty()) {
            if ("pending".equals(feedbackStatus)) {
                // 筛选待处理的记录
                wrapper.and(w -> w.eq(ChatFeedback::getFeedbackStatus, "pending").or().eq(ChatFeedback::getFeedbackStatus, "").or().isNull(ChatFeedback::getFeedbackStatus));
            } else {
                wrapper.eq(ChatFeedback::getFeedbackStatus, feedbackStatus);
            }
        }
        wrapper.orderByDesc(ChatFeedback::getFeedbackTime);

        List<ChatFeedback> feedbacks = chatFeedbackMapper.selectList(wrapper);

        List<ChatFeedbackVO> voList = feedbacks.stream().map(fb -> {
            ChatFeedbackVO vo = buildFeedbackVO(fb);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 获取反馈详情
     */
    @GetMapping("/detail/{id}")
    public Result<ChatFeedbackVO> getDetail(@PathVariable Long id) {
        ChatFeedback feedback = chatFeedbackMapper.selectById(id);
        if (feedback == null) {
            return Result.error("反馈记录不存在");
        }
        ChatFeedbackVO vo = buildFeedbackVO(feedback);
        return Result.success(vo);
    }

    /**
     * 处理错误反馈 - 更新分段内容并标记已处理
     */
    @PutMapping("/process")
    public Result<Void> processFeedback(@RequestBody Map<String, Object> params) {
        Long chatFeedbackId = Long.valueOf(params.get("chatFeedbackId").toString());
        String segmentId = (String) params.get("segmentId");
        String datasetId = (String) params.get("datasetId");
        String documentId = (String) params.get("documentId");
        String content = (String) params.get("content");

        // 1. 更新Dify中的分段内容
        try {
            log.info("开始更新dify中知识库中的内容---------------------------------------------");
            difyKnowledgeService.updateSegment(datasetId, documentId, segmentId, content, null, null, null);
        } catch (Exception e) {
            log.warn("更新Dify分段内容失败: {}", e.getMessage());
            return Result.error("更新Dify分段内容失败: " + e.getMessage());
        }

        // 2. 更新本地chat_segment表中的content
        ChatSegment segment = chatSegmentMapper.selectOne(
                new LambdaQueryWrapper<ChatSegment>()
                        .eq(ChatSegment::getSegmentId, segmentId)
                        .last("LIMIT 1")
        );
        if (segment != null) {
            segment.setContent(content);
            chatSegmentMapper.updateById(segment);
        }

        // 3. 更新chat_feedback状态为processed（已处理）
        ChatFeedback feedback = chatFeedbackMapper.selectById(chatFeedbackId);
        if (feedback != null) {
            feedback.setFeedbackStatus("processed");
            feedback.setFeedbackTime(LocalDateTime.now());
            chatFeedbackMapper.updateById(feedback);
        }

        log.info("处理错误反馈: chatFeedbackId={}, segmentId={}, 已更新分段内容并标记为processed", chatFeedbackId, segmentId);
        return Result.success();
    }

    /**
     * 构建ChatFeedbackVO
     */
    private ChatFeedbackVO buildFeedbackVO(ChatFeedback fb) {
        ChatFeedbackVO vo = new ChatFeedbackVO();
        vo.setId(fb.getId());
        vo.setChatHistoryId(fb.getChatHistoryId());
        vo.setFeedbackStatus(fb.getFeedbackStatus());
        vo.setFeedbackTime(fb.getFeedbackTime());
        vo.setUserId(fb.getUserId());

        // 查询chat_history
        ChatHistory history = chatHistoryMapper.selectById(fb.getChatHistoryId());
        if (history != null) {
            vo.setUserQuestion(history.getUserQuestion());
            vo.setAnswerContent(history.getAnswerContent());
            vo.setDeviceModel(history.getDeviceModel());
            vo.setChatCreateTime(history.getCreateTime());
        }

        // 查询文档资源和分段
        List<ChatDocumentResource> docResources = chatDocumentResourceMapper.selectList(
                new LambdaQueryWrapper<ChatDocumentResource>()
                        .eq(ChatDocumentResource::getChatHistoryId, fb.getChatHistoryId())
        );

        List<ChatFeedbackVO.DocumentResourceVO> docVOList = new ArrayList<>();
        for (ChatDocumentResource docRes : docResources) {
            ChatFeedbackVO.DocumentResourceVO docVO = new ChatFeedbackVO.DocumentResourceVO();
            docVO.setId(docRes.getId());
            docVO.setDocumentId(docRes.getDocumentId());
            docVO.setDocumentName(docRes.getDocumentName());
            docVO.setDatasetId(docRes.getDatasetId());
            docVO.setDatasetName(docRes.getDatasetName());

            // 查询分段
            List<ChatSegment> segments = chatSegmentMapper.selectList(
                    new LambdaQueryWrapper<ChatSegment>()
                            .eq(ChatSegment::getDocumentResourceId, docRes.getId())
            );

            List<ChatFeedbackVO.SegmentVO> segVOList = segments.stream().map(seg -> {
                ChatFeedbackVO.SegmentVO segVO = new ChatFeedbackVO.SegmentVO();
                segVO.setId(seg.getId());
                segVO.setSegmentId(seg.getSegmentId());
                segVO.setContent(seg.getContent());
                segVO.setScore(seg.getScore());
                return segVO;
            }).collect(Collectors.toList());

            docVO.setSegments(segVOList);
            docVOList.add(docVO);
        }
        vo.setDocumentResources(docVOList);

        return vo;
    }
}
