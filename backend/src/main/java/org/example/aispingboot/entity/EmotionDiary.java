package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记实体（对应 emotion_diary 表）
 */
@Data
@TableName("emotion_diary")
@Builder
public class EmotionDiary {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("diary_date")
    private LocalDate diaryDate;

    @TableField("mood_score")
    private Integer moodScore;

    @TableField("dominant_emotion")
    private String dominantEmotion;

    @TableField("emotion_triggers")
    private String emotionTriggers;

    @TableField("diary_content")
    private String diaryContent;

    @TableField("sleep_quality")
    private Integer sleepQuality;

    @TableField("stress_level")
    private Integer stressLevel;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
