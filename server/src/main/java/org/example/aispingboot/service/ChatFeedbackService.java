package org.example.aispingboot.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.command.ChatFeedbackDTO;
import org.example.aispingboot.DTO.response.ChatFeedbackResponseDTO;
import org.example.aispingboot.entity.ChatFeedback;
import org.example.aispingboot.mapper.ChatFeedbackMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatFeedbackService {
    private final ChatFeedbackMapper feedbackMapper;

    public ChatFeedbackService(ChatFeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    public ChatFeedback create(Long userId, ChatFeedbackDTO dto) {
        ChatFeedback feedback = ChatFeedback.builder()
                .userId(userId)
                .sessionId(dto.getSessionId())
                .assistantMessageId(dto.getAssistantMessageId())
                .helpfulness(dto.getHelpfulness())
                .reasonTags(dto.getReasonTags() == null ? null : JSONUtil.toJsonStr(dto.getReasonTags()))
                .comment(dto.getComment())
                .createdAt(LocalDateTime.now())
                .build();
        feedbackMapper.insert(feedback);
        return feedback;
    }

    public Page<ChatFeedbackResponseDTO> adminPage(int page, int pageSize) {
        Page<ChatFeedback> pager = new Page<>(page, Math.min(pageSize, 100));
        Page<ChatFeedback> result = feedbackMapper.selectPage(pager,
                new LambdaQueryWrapper<ChatFeedback>().orderByDesc(ChatFeedback::getCreatedAt));
        Page<ChatFeedbackResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(f -> ChatFeedbackResponseDTO.builder()
                .id(f.getId())
                .userId(f.getUserId())
                .sessionId(f.getSessionId())
                .helpfulness(f.getHelpfulness())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .build()).collect(Collectors.toList()));
        return response;
    }

    public long countPositive() {
        return feedbackMapper.selectCount(new LambdaQueryWrapper<ChatFeedback>()
                .eq(ChatFeedback::getHelpfulness, 1));
    }

    public long countAll() {
        return feedbackMapper.selectCount(null);
    }
}
