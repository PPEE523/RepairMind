package com.example.equipment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiFeedbackStatsDTO {

    private Long totalFeedback;

    private Long correctCount;

    private Long errorCount;

    private Double correctRate;

    private List<DailyFeedback> dailyStats;

    private List<ErrorCategory> errorCategories;

    @Data
    @Builder
    public static class DailyFeedback {
        private String date;
        private Long correctCount;
        private Long errorCount;
    }

    @Data
    @Builder
    public static class ErrorCategory {
        private String category;
        private Long count;
        private Double percentage;
    }
}
