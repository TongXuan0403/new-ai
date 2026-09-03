package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主题化成长计划实体（对应 growth_plan 表）
 */
@Data
@TableName("growth_plan")
@Builder
public class GrowthPlan {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("theme")
    private String theme;

    @TableField("content")
    private String content;

    @TableField("duration_days")
    private Integer durationDays;

    @TableField("reviewer")
    private String reviewer;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("status")
    private Integer status;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
