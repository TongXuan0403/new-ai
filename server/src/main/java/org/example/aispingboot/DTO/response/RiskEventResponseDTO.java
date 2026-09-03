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
public class RiskEventResponseDTO {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Integer riskLevel;
    private String riskType;
    private String actionType;
    private String contentSummary;
    private String ruleVersion;
    private String modelVersion;
    private String status;
    private Boolean crisisCardShown;
    private LocalDateTime createdAt;
}
