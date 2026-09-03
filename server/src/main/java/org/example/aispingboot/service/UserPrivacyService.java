package org.example.aispingboot.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.DTO.response.PrivacyProfileResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.ArticleFavorite;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.entity.ExerciseCompletion;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.entity.UserConsent;
import org.example.aispingboot.entity.UserDeletionRequest;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ArticleFavoriteMapper;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.example.aispingboot.mapper.ExerciseCompletionMapper;
import org.example.aispingboot.mapper.UserConsentMapper;
import org.example.aispingboot.mapper.UserDeletionRequestMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserPrivacyService {
    private final UserMapper userMapper;
    private final UserConsentMapper userConsentMapper;
    private final UserDeletionRequestMapper deletionRequestMapper;
    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationMessageMapper messageMapper;
    private final EmotionDiaryMapper diaryMapper;
    private final ArticleFavoriteMapper favoriteMapper;
    private final ExerciseCompletionMapper completionMapper;
    private final ConsentService consentService;

    public UserPrivacyService(UserMapper userMapper, UserConsentMapper userConsentMapper,
                              UserDeletionRequestMapper deletionRequestMapper,
                              ConsultationSessionMapper sessionMapper,
                              ConsultationMessageMapper messageMapper,
                              EmotionDiaryMapper diaryMapper,
                              ArticleFavoriteMapper favoriteMapper,
                              ExerciseCompletionMapper completionMapper,
                              ConsentService consentService) {
        this.userMapper = userMapper;
        this.userConsentMapper = userConsentMapper;
        this.deletionRequestMapper = deletionRequestMapper;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.diaryMapper = diaryMapper;
        this.favoriteMapper = favoriteMapper;
        this.completionMapper = completionMapper;
        this.consentService = consentService;
    }

    public PrivacyProfileResponseDTO getProfile(Long userId) {
        var consent = consentService.getStatus(userId);
        UserDeletionRequest deletion = latestDeletion(userId);
        Long sessionCount = sessionMapper.selectCount(new LambdaQueryWrapper<ConsultationSession>()
                .eq(ConsultationSession::getUserId, userId));
        Long diaryCount = diaryMapper.selectCount(new LambdaQueryWrapper<EmotionDiary>()
                .eq(EmotionDiary::getUserId, userId));
        return PrivacyProfileResponseDTO.builder()
                .userId(userId)
                .consentComplete(consent.getComplete())
                .consentRevoked(consent.getRevoked())
                .privacyPolicyVersion(consent.getPrivacyPolicyVersion())
                .sensitiveInfoVersion(consent.getSensitiveInfoVersion())
                .productBoundaryVersion(consent.getProductBoundaryVersion())
                .deletionRequested(deletion != null && !"已取消".equals(deletion.getStatus()))
                .deletionStatus(deletion != null ? deletion.getStatus() : null)
                .sessionCount(sessionCount)
                .diaryCount(diaryCount)
                .dataScope(List.of("会话与消息", "情绪日记", "同意记录", "风险事件", "文章收藏", "练习完成记录"))
                .build();
    }

    @Transactional
    public UserDeletionRequest submitDeletion(Long userId, String reason) {
        UserDeletionRequest existing = latestDeletion(userId);
        if (existing != null && !"已取消".equals(existing.getStatus()) && !"已完成".equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.DATA_DELETION_PENDING);
        }
        UserDeletionRequest request = UserDeletionRequest.builder()
                .userId(userId)
                .status("待处理")
                .reason(reason)
                .requestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        deletionRequestMapper.insert(request);
        return request;
    }

    public UserDeletionRequest getDeletionStatus(Long userId) {
        return latestDeletion(userId);
    }

    @Transactional
    public boolean cancelDeletion(Long userId) {
        UserDeletionRequest request = latestDeletion(userId);
        if (request == null || "已完成".equals(request.getStatus())) {
            return false;
        }
        if (!"已取消".equals(request.getStatus())) {
            request.setStatus("已取消");
            request.setCanceledAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());
            deletionRequestMapper.updateById(request);
        }
        return true;
    }

    private UserDeletionRequest latestDeletion(Long userId) {
        List<UserDeletionRequest> list = deletionRequestMapper.selectList(new LambdaQueryWrapper<UserDeletionRequest>()
                .eq(UserDeletionRequest::getUserId, userId)
                .orderByDesc(UserDeletionRequest::getCreatedAt)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 导出本人数据（会话/消息/日记/同意/删除申请），返回 JSON 字符串。
     */
    public String exportData(Long userId) {
        User user = userMapper.selectById(userId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportedAt", LocalDateTime.now().toString());
        payload.put("user", Map.of("id", user.getId(), "username", user.getUsername(),
                "nickname", user.getDisplayName(), "userType", user.getUserType()));

        List<UserConsent> consents = userConsentMapper.selectList(new LambdaQueryWrapper<UserConsent>()
                .eq(UserConsent::getUserId, userId).orderByAsc(UserConsent::getCreatedAt));
        payload.put("consents", consents);

        List<ConsultationSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<ConsultationSession>()
                .eq(ConsultationSession::getUserId, userId).orderByAsc(ConsultationSession::getStartedAt));
        Map<Long, List<ConsultationMessage>> messagesBySession = messageMapper.selectList(
                        new LambdaQueryWrapper<ConsultationMessage>()
                                .in(ConsultationMessage::getSessionId,
                                        sessions.stream().map(ConsultationSession::getId).collect(Collectors.toList())))
                .stream().collect(Collectors.groupingBy(ConsultationMessage::getSessionId));
        Map<String, Object> sessionData = sessions.stream().collect(Collectors.toMap(
                s -> String.valueOf(s.getId()),
                s -> Map.of("sessionId", s.getId(), "title", s.getSessionTitle(),
                        "startedAt", s.getStartedAt(),
                        "messages", messagesBySession.getOrDefault(s.getId(), List.of()))));
        payload.put("sessions", sessionData);

        List<EmotionDiary> diaries = diaryMapper.selectList(new LambdaQueryWrapper<EmotionDiary>()
                .eq(EmotionDiary::getUserId, userId).orderByAsc(EmotionDiary::getLogDate));
        payload.put("diaries", diaries);

        UserDeletionRequest deletion = latestDeletion(userId);
        payload.put("deletionRequest", deletion);

        List<ArticleFavorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, userId).orderByAsc(ArticleFavorite::getCreatedAt));
        payload.put("articleFavorites", favorites);

        List<ExerciseCompletion> completions = completionMapper.selectList(new LambdaQueryWrapper<ExerciseCompletion>()
                .eq(ExerciseCompletion::getUserId, userId).orderByAsc(ExerciseCompletion::getCompletedAt));
        payload.put("exerciseCompletions", completions);

        return JSONUtil.toJsonPrettyStr(payload);
    }
}
