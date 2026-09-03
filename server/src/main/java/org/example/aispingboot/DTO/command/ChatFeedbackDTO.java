package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatFeedbackDTO {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    private Long assistantMessageId;

    @NotNull(message = "帮助度不能为空")
    @Min(value = 1, message = "帮助度范围为1-3")
    @Max(value = 3, message = "帮助度范围为1-3")
    private Integer helpfulness;

    private java.util.List<String> reasonTags;

    @Size(max = 500, message = "反馈内容不能超过500字")
    private String comment;
}
