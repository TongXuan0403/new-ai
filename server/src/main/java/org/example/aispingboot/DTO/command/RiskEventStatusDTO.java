package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RiskEventStatusDTO {
    @NotBlank(message = "处理状态不能为空")
    private String status;
}
