package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispingboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispingboot.DTO.response.ConsultationSessionResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsultationSessionService {
    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationMessageMapper messageMapper;

    public ConsultationSessionService(ConsultationSessionMapper sessionMapper, ConsultationMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional
    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO dto) {
        ConsultationSession session = ConsultationSession.builder()
                .userId(userId)
                .sessionTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : "新的倾诉")
                .mood(dto.getMood())
                .model(dto.getModel())
                .status("active")
                .riskLevel(0)
                .startedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        sessionMapper.insert(session);
        return session;
    }

    public List<ConsultationSessionResponseDTO> listSessionsForUser(Long userId, String keyword) {
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<ConsultationSession>()
                .eq(ConsultationSession::getUserId, userId)
                .eq(ConsultationSession::getStatus, "active")
                .orderByDesc(ConsultationSession::getStartedAt);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ConsultationSession::getSessionTitle, keyword);
        }
        return sessionMapper.selectList(wrapper).stream()
                .map(session -> toResponse(session, false))
                .collect(Collectors.toList());
    }

    public ConsultationSessionResponseDTO getSessionDetail(Long sessionId, User user) {
        validateSessionOwnership(sessionId, user.getId());
        ConsultationSession session = sessionMapper.selectById(sessionId);
        return toResponse(session, true);
    }

    @Transactional
    public void deleteSession(Long sessionId, User user) {
        validateSessionOwnership(sessionId, user.getId());
        sessionMapper.deleteById(sessionId);
        messageMapper.delete(new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getSessionId, sessionId));
    }

    public void validateSessionOwnership(Long sessionId, Long userId) {
        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RESOURCE_FORBIDDEN, "无权访问该会话");
        }
    }

    public ConsultationSession getEntity(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    public void updateRiskLevel(Long sessionId, Integer riskLevel) {
        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setRiskLevel(riskLevel);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    public void autoTitle(Long sessionId) {
        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session == null || StringUtils.hasText(session.getSessionTitle())
                && !"新的倾诉".equals(session.getSessionTitle())) {
            return;
        }
        session.setSessionTitle("正在梳理的一件事");
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    private ConsultationSessionResponseDTO toResponse(ConsultationSession session, boolean withMessages) {
        List<ConsultationMessageResponseDTO> messages = null;
        if (withMessages) {
            messages = messageMapper.selectList(new LambdaQueryWrapper<ConsultationMessage>()
                            .eq(ConsultationMessage::getSessionId, session.getId())
                            .orderByAsc(ConsultationMessage::getCreatedAt))
                    .stream().map(this::toMessageResponse).collect(Collectors.toList());
        }
        return ConsultationSessionResponseDTO.builder()
                .id(session.getId())
                .sessionTitle(session.getSessionTitle())
                .mood(session.getMood())
                .model(session.getModel())
                .status(session.getStatus())
                .riskLevel(session.getRiskLevel())
                .startedAt(session.getStartedAt())
                .updatedAt(session.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private ConsultationMessageResponseDTO toMessageResponse(ConsultationMessage message) {
        return ConsultationMessageResponseDTO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .senderType(message.getSenderType())
                .senderTypeDesc(message.getSenderType() != null && message.getSenderType() == 1 ? "用户" : "AI助手")
                .messageType(message.getMessageType())
                .content(message.getContent())
                .emotionTag(message.getEmotionTag())
                .aiModel(message.getAiModel())
                .riskLevel(message.getRiskLevel())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
