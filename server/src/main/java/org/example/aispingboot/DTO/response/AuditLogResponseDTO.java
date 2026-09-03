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
public class AuditLogResponseDTO {
    private Long id;
    private Long operatorId;
    private String operatorRole;
    private String action;
    private String targetType;
    private Long targetId;
    private String ip;
    private String detail;
    private LocalDateTime createdAt;
}
