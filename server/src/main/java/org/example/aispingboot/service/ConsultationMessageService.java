package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultationMessageService {
    private final ConsultationMessageMapper messageMapper;

    public ConsultationMessageService(ConsultationMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public ConsultationMessage saveUserMessage(Long sessionId, String content, Integer riskLevel) {
        ConsultationMessage message = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(1)
                .messageType(1)
                .content(content)
                .riskLevel(riskLevel != null ? riskLevel : 0)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insert(message);
        return message;
    }

    public ConsultationMessage saveAssistantMessage(Long sessionId, String content, String model, Integer riskLevel) {
        ConsultationMessage message = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(2)
                .messageType(1)
                .content(content)
                .aiModel(model)
                .riskLevel(riskLevel != null ? riskLevel : 0)
                .createdAt(LocalDateTime.now())
                .build();
        messageMapper.insert(message);
        return message;
    }

    public List<ConsultationMessage> listMessages(Long sessionId) {
        return messageMapper.selectList(new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getSessionId, sessionId)
                .orderByAsc(ConsultationMessage::getCreatedAt));
    }
}
