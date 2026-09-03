package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExerciseCompletionDTO {
    @Size(max = 100, message = "练习后感受不能超过100个字符")
    private String moodAfter;
}
