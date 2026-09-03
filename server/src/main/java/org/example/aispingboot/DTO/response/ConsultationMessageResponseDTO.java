package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationMessageResponseDTO {
    private Long id;
    private Long sessionId;
    private Integer senderType;
    private String senderTypeDesc;
    private Integer messageType;
    private String content;
    private String emotionTag;
    private String aiModel;
    private Integer riskLevel;
    private LocalDateTime createdAt;
}
