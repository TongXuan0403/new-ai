package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 成长计划创建/更新
 */
@Data
public class GrowthPlanCreateDTO {
    @NotBlank(message = "计划标题不能为空")
    @Size(max = 200, message = "标题最长200字")
    private String title;

    @Size(max = 500, message = "摘要最长500字")
    private String summary;

    @Size(max = 50, message = "主题最长50字")
    private String theme;

    private String content;

    private Integer durationDays;

    @Size(max = 50, message = "审核人最长50字")
    private String reviewer;

    private Integer status;
}
