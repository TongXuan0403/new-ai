package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 成长计划状态变更
 */
@Data
public class GrowthPlanStatusDTO {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
