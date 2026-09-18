package com.example.equipment.dto;

import lombok.Data;

import java.util.List;

/**
 * AI修正统计 - 反馈统计VO
 */
@Data
public class ChatFeedbackStatsVO {

    /** 总反馈数（correct + incorrect + processed） */
    private Long totalFeedback;

    /** 正确反馈数 */
    private Long correctCount;

    /** 错误反馈数 */
    private Long errorCount;

    /** 已处理数 */
    private Long processedCount;

    /** 待处理数 */
    private Long pendingCount;

    /** 正确率 */
    private Double correctRate;

    /** 按日统计（最近7天） */
    private List<DailyStat> dailyStats;

    /**
     * 按日统计
     */
    @Data
    public static class DailyStat {

        /** 日期，如 "6/5" */
        private String date;

        /** 正确数 */
        private Long correctCount;

        /** 错误数 */
        private Long errorCount;

        /** 已处理数 */
        private Long processedCount;

        /** 待处理数 */
        private Long pendingCount;
    }
}
