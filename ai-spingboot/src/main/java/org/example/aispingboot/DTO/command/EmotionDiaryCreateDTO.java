package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增情绪日记
 */
@Data
public class EmotionDiaryCreateDTO {
    /** 日期 YYYY-MM-DD */
    @NotBlank(message = "日期不能为空")
    private String diaryDate;

    /** 情绪评分 0-10 */
    private Integer moodScore;

    private String dominantEmotion;

    private String emotionTriggers;

    private String diaryContent;

    private Integer sleepQuality;

    private Integer stressLevel;
}
