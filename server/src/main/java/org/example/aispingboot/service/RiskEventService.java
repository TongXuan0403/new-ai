package org.example.aispingboot.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.response.RiskEventResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.RiskEvent;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.RiskEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RiskEventService {
    private final RiskEventMapper riskEventMapper;

    public RiskEventService(RiskEventMapper riskEventMapper) {
        this.riskEventMapper = riskEventMapper;
    }

    /**
     * 创建风险事件（level >= 2 时）。返回事件，或 null（未达记录阈值）。
     */
    @Transactional
    public RiskEvent create(Long userId, Long sessionId, Long messageId,
                            RiskDetectionService.RiskResult risk,
                            String rawContent, boolean crisisCardShown) {
        if (risk.getLevel() < 2) {
            return null;
        }
        String summary = buildSummary(risk, rawContent);
        RiskEvent event = RiskEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .riskLevel(risk.getLevel())
                .riskType(risk.getRiskType())
                .actionType(risk.getActionType())
                .matchedRules(JSONUtil.toJsonStr(risk.getMatchedRules()))
                .contentSummary(summary)
                .ruleVersion(RiskDetectionService.RULE_VERSION)
                .modelVersion(RiskDetectionService.MODEL_VERSION)
                .status("待复核")
                .crisisCardShown(crisisCardShown ? 1 : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        riskEventMapper.insert(event);
        return event;
    }

    private String buildSummary(RiskDetectionService.RiskResult risk, String rawContent) {
        if (risk.getLevel() >= 3) {
            return "高风险表达，已展示危机卡";
        }
        if (!StringUtils.hasText(rawContent)) {
            return "风险表达（脱敏）";
        }
        String trimmed = rawContent.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) + "…" : trimmed;
    }

    public Page<RiskEventResponseDTO> adminPage(Integer level, String status, int page, int pageSize) {
        LambdaQueryWrapper<RiskEvent> wrapper = new LambdaQueryWrapper<RiskEvent>()
                .orderByDesc(RiskEvent::getCreatedAt);
        if (level != null) {
            wrapper.eq(RiskEvent::getRiskLevel, level);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(RiskEvent::getStatus, status);
        }
        Page<RiskEvent> pager = new Page<>(page, Math.min(pageSize, 100));
        Page<RiskEvent> result = riskEventMapper.selectPage(pager, wrapper);
        Page<RiskEventResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return response;
    }

    public RiskEventResponseDTO getById(Long id) {
        RiskEvent event = riskEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "风险事件不存在");
        }
        return toResponse(event);
    }

    @Transactional
    public RiskEventResponseDTO updateStatus(Long id, String status) {
        RiskEvent event = riskEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "风险事件不存在");
        }
        event.setStatus(status);
        event.setUpdatedAt(LocalDateTime.now());
        riskEventMapper.updateById(event);
        return toResponse(event);
    }

    public List<RiskEvent> listByUser(Long userId) {
        return riskEventMapper.selectList(new LambdaQueryWrapper<RiskEvent>()
                .eq(RiskEvent::getUserId, userId)
                .orderByDesc(RiskEvent::getCreatedAt));
    }

    private RiskEventResponseDTO toResponse(RiskEvent event) {
        return RiskEventResponseDTO.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .riskLevel(event.getRiskLevel())
                .riskType(event.getRiskType())
                .actionType(event.getActionType())
                .contentSummary(event.getContentSummary())
                .ruleVersion(event.getRuleVersion())
                .modelVersion(event.getModelVersion())
                .status(event.getStatus())
                .crisisCardShown(Integer.valueOf(1).equals(event.getCrisisCardShown()))
                .createdAt(event.getCreatedAt())
                .build();
    }
}
