package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文章状态更新
 */
@Data
public class ArticleStatusDTO {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
