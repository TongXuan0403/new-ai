package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("consultation_session")
public class ConsultationSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("session_title")
    private String sessionTitle;

    private String mood;

    private String model;

    private String status;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("last_emotion_analysis")
    private String lastEmotionAnalysis;

    @TableField("last_emotion_updated_at")
    private LocalDateTime lastEmotionUpdatedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
