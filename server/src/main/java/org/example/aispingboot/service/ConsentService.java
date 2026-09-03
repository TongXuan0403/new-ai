package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.DTO.command.ConsentSubmitDTO;
import org.example.aispingboot.DTO.response.ConsentStatusResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.UserConsent;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.UserConsentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsentService {
    private final UserConsentMapper userConsentMapper;

    @Value("${app.versions.privacy-policy:privacy-v1.0}")
    private String privacyPolicyVersion;

    @Value("${app.versions.sensitive-info:sensitive-v1.0}")
    private String sensitiveInfoVersion;

    @Value("${app.versions.product-boundary:boundary-v1.0}")
    private String productBoundaryVersion;

    public ConsentService(UserConsentMapper userConsentMapper) {
        this.userConsentMapper = userConsentMapper;
    }

    /**
     * 最新一条同意记录（含已撤回），无则返回 null。
     */
    private UserConsent latest(Long userId) {
        List<UserConsent> list = userConsentMapper.selectList(new LambdaQueryWrapper<UserConsent>()
                .eq(UserConsent::getUserId, userId)
                .orderByDesc(UserConsent::getCreatedAt)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 查询当前同意状态。
     */
    public ConsentStatusResponseDTO getStatus(Long userId) {
        UserConsent latest = latest(userId);
        boolean complete = latest != null
                && Integer.valueOf(1).equals(latest.getAgeConfirmed())
                && latest.getRevokedAt() == null;
        return ConsentStatusResponseDTO.builder()
                .complete(complete)
                .ageConfirmed(latest != null && Integer.valueOf(1).equals(latest.getAgeConfirmed()))
                .privacyPolicyVersion(latest != null ? latest.getPrivacyPolicyVersion() : privacyPolicyVersion)
                .sensitiveInfoVersion(latest != null ? latest.getSensitiveInfoVersion() : sensitiveInfoVersion)
                .productBoundaryVersion(latest != null ? latest.getProductBoundaryVersion() : productBoundaryVersion)
                .revoked(latest != null && latest.getRevokedAt() != null)
                .consentedAt(latest != null ? latest.getConsentedAt() : null)
                .revokedAt(latest != null ? latest.getRevokedAt() : null)
                .build();
    }

    /**
     * 提交首次同意。若已同意且未撤回则幂等返回。
     */
    public ConsentStatusResponseDTO submit(Long userId, ConsentSubmitDTO dto) {
        ConsentStatusResponseDTO current = getStatus(userId);
        if (Boolean.TRUE.equals(current.getComplete())) {
            return current;
        }
        UserConsent consent = UserConsent.builder()
                .userId(userId)
                .ageConfirmed(Boolean.TRUE.equals(dto.getAgeConfirmed()) ? 1 : 0)
                .privacyPolicyVersion(dto.getPrivacyPolicyVersion())
                .sensitiveInfoVersion(dto.getSensitiveInfoVersion())
                .productBoundaryVersion(dto.getProductBoundaryVersion())
                .consentedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userConsentMapper.insert(consent);
        return getStatus(userId);
    }

    /**
     * 撤回非必要授权：将最新记录标记为已撤回。
     */
    public ConsentStatusResponseDTO revoke(Long userId) {
        UserConsent latest = latest(userId);
        if (latest != null && latest.getRevokedAt() == null) {
            latest.setRevokedAt(LocalDateTime.now());
            latest.setUpdatedAt(LocalDateTime.now());
            userConsentMapper.updateById(latest);
        }
        return getStatus(userId);
    }

    /**
     * 校验用户已完成必要同意，否则抛 CONSENT_REQUIRED。
     */
    public void ensureConsented(Long userId) {
        ConsentStatusResponseDTO status = getStatus(userId);
        if (!Boolean.TRUE.equals(status.getComplete())) {
            throw new BusinessException(ResultCode.CONSENT_REQUIRED);
        }
    }

    /**
     * 校验用户已确认满 18 岁。
     */
    public void ensureAgeConfirmed(Long userId) {
        ConsentStatusResponseDTO status = getStatus(userId);
        if (!Boolean.TRUE.equals(status.getAgeConfirmed())) {
            throw new BusinessException(ResultCode.AGE_NOT_SUPPORTED);
        }
    }
}
