package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExerciseCreateDTO {
    private Long categoryId;

    @NotBlank(message = "练习名称不能为空")
    @Size(max = 200, message = "练习名称不能超过200个字符")
    private String title;

    @Size(max = 500, message = "简介不能超过500个字符")
    private String summary;

    private String content;

    private Integer minutes;

    @Size(max = 500, message = "标签不能超过500个字符")
    private String tags;

    private Integer sortOrder;

    private String status;
}
