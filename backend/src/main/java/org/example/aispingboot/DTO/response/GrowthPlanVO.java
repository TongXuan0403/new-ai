package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成长计划返回
 */
@Data
@Builder
public class GrowthPlanVO {
    private Long id;
    private String title;
    private String summary;
    private String theme;
    private String content;
    private Integer durationDays;
    private String reviewer;
    private LocalDateTime reviewedAt;
    private Integer status;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /**
     * 当前用户进度（0-100），未开始为 null
     */
    private Integer myProgress;
    /**
     * 当前用户是否已完成
     */
    private Boolean myCompleted;
}
