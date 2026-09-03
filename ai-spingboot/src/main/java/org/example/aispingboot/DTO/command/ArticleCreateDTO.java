package org.example.aispingboot.DTO.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/更新文章
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleCreateDTO {
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题最多200个字符")
    private String title;

    private Long categoryId;

    @Size(max = 500, message = "摘要最多500个字符")
    private String summary;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private String coverImage;

    private String tags;

    /** 状态 0草稿 1已发布 2已下线 */
    private Integer status;
}
