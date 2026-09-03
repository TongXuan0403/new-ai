package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户更新成长计划进度
 */
@Data
public class GrowthPlanProgressDTO {
    @NotNull(message = "进度不能为空")
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private Integer progress;
}
