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
@TableName("risk_event")
public class RiskEvent {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("session_id")
    private Long sessionId;

    @TableField("message_id")
    private Long messageId;

    @TableField("risk_level")
    private Integer riskLevel;

    @TableField("risk_type")
    private String riskType;

    @TableField("action_type")
    private String actionType;

    @TableField("matched_rules")
    private String matchedRules;

    @TableField("content_summary")
    private String contentSummary;

    @TableField("rule_version")
    private String ruleVersion;

    @TableField("model_version")
    private String modelVersion;

    private String status;

    @TableField("crisis_card_shown")
    private Integer crisisCardShown;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
