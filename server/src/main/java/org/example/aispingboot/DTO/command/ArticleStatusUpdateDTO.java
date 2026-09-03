package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleStatusUpdateDTO {
    @NotBlank(message = "状态不能为空")
    private String status;

    private String auditRemark;
}
