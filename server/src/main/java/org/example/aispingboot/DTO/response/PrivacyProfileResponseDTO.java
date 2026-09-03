package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 隐私与数据管理概览。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyProfileResponseDTO {
    private Long userId;
    private Boolean consentComplete;
    private Boolean consentRevoked;
    private String privacyPolicyVersion;
    private String sensitiveInfoVersion;
    private String productBoundaryVersion;
    private Boolean deletionRequested;
    private String deletionStatus;
    private Long sessionCount;
    private Long diaryCount;
    private List<String> dataScope;
}
