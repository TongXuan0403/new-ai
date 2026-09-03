package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionDiaryResponseDTO {
    private Long id;
    private String emotionStatus;
    private Integer score;
    private String event;
    private String sleepStatus;
    private String energyStatus;
    private LocalDate logDate;
    private LocalDateTime createdAt;
}
