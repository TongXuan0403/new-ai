package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 后台运营概览（仅聚合脱敏数据）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataOverviewResponseDTO {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalSessions;
    private Long sessionUsers;
    private Long totalDiaries;
    private Long diaryUsers;
    private Long publishedArticles;
    private Long totalViews;
    private Long totalFavorites;
    private Long publishedExercises;
    private Long exerciseCompletions;
    private Long riskEvents;
    private Long riskPending;
    private Map<String, Long> riskByLevel;
}
