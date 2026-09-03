package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleCreateDTO {
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    @Size(max = 500, message = "摘要不能超过500个字符")
    private String summary;

    @Size(max = 100, message = "来源不能超过100个字符")
    private String source;

    private String content;

    private String coverUrl;

    private Integer minutes;

    private String status;
}
