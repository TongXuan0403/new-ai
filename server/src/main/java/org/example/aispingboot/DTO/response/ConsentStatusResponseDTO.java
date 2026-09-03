package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 同意状态：complete 为 true 表示已满足使用条件；否则返回需要确认的版本。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentStatusResponseDTO {
    private Boolean complete;
    private Boolean ageConfirmed;
    private String privacyPolicyVersion;
    private String sensitiveInfoVersion;
    private String productBoundaryVersion;
    private Boolean revoked;
    private LocalDateTime consentedAt;
    private LocalDateTime revokedAt;
}
