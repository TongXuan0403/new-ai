package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增情绪日记：必填仅 主情绪 + 总体状态分。
 */
@Data
public class EmotionDiaryCreateDTO {
    @NotBlank(message = "主情绪不能为空")
    @Size(max = 50, message = "主情绪长度不能超过50个字符")
    private String emotionStatus;

    @NotNull(message = "总体状态分不能为空")
    @Min(value = 1, message = "总体状态分范围为1-10")
    @Max(value = 10, message = "总体状态分范围为1-10")
    private Integer score;

    @Size(max = 1000, message = "触发事件不能超过1000字")
    private String event;

    private String sleepStatus;

    private String energyStatus;
}
