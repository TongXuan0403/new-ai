package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 匿名聚合校园心理健康报告（纯聚合、无个人数据）
 */
@Data
@Builder
public class CampusReportVO {

    private LocalDateTime generatedAt;

    private ReportOverview overview;

    /** 主导情绪分布（前 N） */
    private List<NameCount> emotionDistribution;

    /** 情绪评分区间分布（0-2 / 3-4 / 5-6 / 7-8 / 9-10） */
    private List<NameCount> moodDistribution;

    /** 近 7 天趋势 */
    private List<DailyPoint> dailyTrend;

    /** 低情绪记录占比（评分≤4 的记录占全部记录比例，百分比 0-100） */
    private Double lowMoodRatio;

    @Data
    @Builder
    public static class ReportOverview {
        private Long totalUsers;
        private Long totalSessions;
        private Long totalMessages;
        private Long totalDiaries;
        private Long activeUsers7d;
        private Double avgMoodScore;
        private Long totalGrowthPlans;
        private Long totalAppointments;
    }

    @Data
    @Builder
    public static class NameCount {
        private String name;
        private Long count;
    }

    @Data
    @Builder
    public static class DailyPoint {
        private String date;
        private Long sessionCount;
        private Long diaryCount;
        private Long activeUsers;
    }
}
