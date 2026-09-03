package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记返回对象
 */
@Data
@Builder
public class EmotionDiaryVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private LocalDate diaryDate;
    private Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    private Integer sleepQuality;
    private Integer stressLevel;
    private LocalDateTime createdAt;
    /** AI 情绪分析 JSON（管理端情绪日志页展示用） */
    private String aiEmotionAnalysis;
}
