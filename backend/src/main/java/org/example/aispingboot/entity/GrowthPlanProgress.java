package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成长计划进度实体（对应 growth_plan_progress 表）
 */
@Data
@TableName("growth_plan_progress")
@Builder
public class GrowthPlanProgress {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("plan_id")
    private Long planId;

    @TableField("progress")
    private Integer progress;

    @TableField("completed")
    private Integer completed;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
