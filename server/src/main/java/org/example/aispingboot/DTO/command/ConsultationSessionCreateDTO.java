package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationSessionCreateDTO {
    @Size(max = 200, message = "会话标题长度不能超过200个字符")
    private String title;

    private String initialMessage;

    private String model;

    private String mood;
}
