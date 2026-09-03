package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 数据看板总览
 */
@Data
@Builder
public class AnalyticsOverviewVO {
    private SystemOverview systemOverview;
    private ConsultationStats consultationStats;
    private List<TrendPoint> emotionTrend;
    private List<UserActivityPoint> userActivity;

    @Data
    @Builder
    public static class SystemOverview {
        private Long totalUsers;
        private Long activeUsers;
        private Long totalDiaries;
        private Long todayNewDiaries;
        private Long totalSessions;
        private Long todayNewSessions;
        private Double avgMoodScore;
    }

    @Data
    @Builder
    public static class ConsultationStats {
        private Long totalSessions;
        private Double avgDurationMinutes;
        private Long activeUsers;
        private List<TrendPoint> dailyTrend;
    }

    @Data
    @Builder
    public static class TrendPoint {
        private String date;
        private Long sessionCount;
        private Long userCount;
        private Double avgMoodScore;
        private Long recordCount;
    }

    @Data
    @Builder
    public static class UserActivityPoint {
        private String date;
        private Long activeUsers;
        private Long newUsers;
        private Long diaryUsers;
        private Long consultationUsers;
    }
}
