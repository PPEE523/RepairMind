package com.example.equipment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据统计看板DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    /**
     * 文档总量（已审核通过的案例数）
     */
    private Long totalDocuments;

    /**
     * 检索次数（今日）
     */
    private Long todaySearchCount;

    /**
     * 检索次数（本月）
     */
    private Long monthSearchCount;

    /**
     * 检索次数（总计）
     */
    private Long totalSearchCount;

    /**
     * 案例新增数量（今日）
     */
    private Long todayNewCases;

    /**
     * 案例新增数量（本月）
     */
    private Long monthNewCases;

    /**
     * 待审核案例数
     */
    private Long pendingAuditCount;

    /**
     * AI反馈统计
     */
    private AiFeedbackStats aiFeedbackStats;

    /**
     * 检索趋势（最近7天）
     */
    private List<DailyStats> searchTrend;

    /**
     * AI反馈统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiFeedbackStats {
        /**
         * 总反馈数
         */
        private Long totalFeedback;

        /**
         * 正确数
         */
        private Long correctCount;

        /**
         * 错误数
         */
        private Long errorCount;

        /**
         * 正确率
         */
        private Double correctRate;
    }

    /**
     * 每日统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        /**
         * 日期
         */
        private String date;

        /**
         * 数量
         */
        private Long count;
    }
}