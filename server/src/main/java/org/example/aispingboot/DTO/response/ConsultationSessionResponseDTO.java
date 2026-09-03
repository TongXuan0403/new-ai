package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSessionResponseDTO {
    private Long id;
    private String sessionTitle;
    private String mood;
    private String model;
    private String status;
    private Integer riskLevel;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private List<ConsultationMessageResponseDTO> messages;
}
