package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_consent")
public class UserConsent {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("age_confirmed")
    private Integer ageConfirmed;

    @TableField("privacy_policy_version")
    private String privacyPolicyVersion;

    @TableField("sensitive_info_version")
    private String sensitiveInfoVersion;

    @TableField("product_boundary_version")
    private String productBoundaryVersion;

    @TableField("consented_at")
    private LocalDateTime consentedAt;

    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
