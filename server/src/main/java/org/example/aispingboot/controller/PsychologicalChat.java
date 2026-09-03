package org.example.aispingboot.controller;

import cn.hutool.json.JSONUtil;
import jakarta.validation.Valid;
import org.example.aispingboot.AiService.PsychologicalSupportService;
import org.example.aispingboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispingboot.DTO.response.ConsultationSessionResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.ConsentService;
import org.example.aispingboot.service.ConsultationMessageService;
import org.example.aispingboot.service.ConsultationSessionService;
import org.example.aispingboot.service.RiskDetectionService;
import org.example.aispingboot.service.UserService;
import org.example.aispingboot.util.SecurityUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 心理咨询会话控制器：二阶段流程。
 * 1. POST /messages 提交消息并生成回复（落库，返回结构化风险字段）
 * 2. GET /stream 基于 assistantMessageId 流式输出回复（断线重试不重复写消息）
 */
@RestController
@RequestMapping("/psychological-chat")
public class PsychologicalChat {
    private final PsychologicalSupportService psychologicalSupportService;
    private final ConsultationSessionService consultationSessionService;
    private final ConsultationMessageService consultationMessageService;
    private final UserService userService;
    private final ConsentService consentService;
    private final RiskDetectionService riskDetectionService;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public PsychologicalChat(PsychologicalSupportService psychologicalSupportService,
                             ConsultationSessionService consultationSessionService,
                             ConsultationMessageService consultationMessageService,
                             UserService userService,
                             ConsentService consentService,
                             RiskDetectionService riskDetectionService) {
        this.psychologicalSupportService = psychologicalSupportService;
        this.consultationSessionService = consultationSessionService;
        this.consultationMessageService = consultationMessageService;
        this.userService = userService;
        this.consentService = consentService;
        this.riskDetectionService = riskDetectionService;
    }

    @PostMapping("/session/start")
    public Result<ConsultationSessionResponseDTO> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        Long userId = SecurityUtil.getCurrentUserId();
        consentService.ensureConsented(userId);
        ConsultationSession session = consultationSessionService.createSession(userId, createDTO);
        if (createDTO.getInitialMessage() != null && !createDTO.getInitialMessage().isBlank()) {
            psychologicalSupportService.submitAndGenerate(userId, session.getId(), createDTO.getInitialMessage(), createDTO.getModel());
        }
        return Result.ok(consultationSessionService.getSessionDetail(session.getId(), userService.getEntityById(userId)));
    }

    @GetMapping("/sessions")
    public Result<List<ConsultationSessionResponseDTO>> listSessions(@RequestParam(required = false) String keyword) {
        return Result.ok(consultationSessionService.listSessionsForUser(SecurityUtil.getCurrentUserId(), keyword));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<ConsultationSessionResponseDTO> getSession(@PathVariable String sessionId) {
        return Result.ok(consultationSessionService.getSessionDetail(
                parseSessionId(sessionId), userService.getEntityById(SecurityUtil.getCurrentUserId())));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Boolean> deleteSession(@PathVariable String sessionId) {
        consultationSessionService.deleteSession(parseSessionId(sessionId), userService.getEntityById(SecurityUtil.getCurrentUserId()));
        return Result.ok(true, "删除成功");
    }

    /**
     * 提交用户消息：风险检测 + 落库 + 生成回复，返回结构化风险字段。
     */
    @PostMapping("/messages")
    public Result<Map<String, Object>> sendMessage(@RequestBody Map<String, String> body) {
        String sessionIdValue = body.get("sessionId");
        String content = body.get("content");
        String model = body.get("model");
        if (sessionIdValue == null || sessionIdValue.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "会话ID不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "消息内容不能为空");
        }
        if (content.length() > 2000) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "消息内容不能超过2000字");
        }
        Long userId = SecurityUtil.getCurrentUserId();
        consentService.ensureConsented(userId);
        Long dbSessionId = parseSessionId(sessionIdValue);
        PsychologicalSupportService.ChatResult result =
                psychologicalSupportService.submitAndGenerate(userId, dbSessionId, content, model);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userMessageId", result.userMessageId);
        payload.put("assistantMessageId", result.assistantMessageId);
        payload.put("riskLevel", result.riskLevel);
        payload.put("riskType", result.riskType);
        payload.put("actionType", result.actionType);
        payload.put("riskEventId", result.riskEventId);
        payload.put("ruleVersion", result.ruleVersion);
        payload.put("model", result.model);
        payload.put("reply", result.reply);
        return Result.ok(payload);
    }

    /**
     * SSE 流式输出：基于 assistantMessageId 读取已生成回复，分段下发。
     * 事件：risk -> delta* -> done / error
     * 认证与数据读取在请求线程完成（SecurityContext 仅对请求线程可见），
     * 异步线程只负责发送事件。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String sessionId, @RequestParam Long assistantMessageId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long dbSessionId = parseSessionId(sessionId);
        consultationSessionService.validateSessionOwnership(dbSessionId, userId);
        ConsultationMessage aiMessage = consultationMessageService.listMessages(dbSessionId).stream()
                .filter(m -> m.getId().equals(assistantMessageId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "回复消息不存在"));

        ConsultationSession session = consultationSessionService.getEntity(dbSessionId);
        int riskLevel = session != null && session.getRiskLevel() != null ? session.getRiskLevel() : 0;
        String actionType = riskLevel >= 3 ? "SHOW_CRISIS_CARD"
                : riskLevel == 2 ? "SHOW_GUIDANCE" : "NONE";

        Map<String, Object> riskEvent = new LinkedHashMap<>();
        riskEvent.put("riskLevel", riskLevel);
        riskEvent.put("actionType", actionType);
        riskEvent.put("ruleVersion", riskDetectionService.currentRuleVersion());

        String[] fragments = psychologicalSupportService.splitReply(aiMessage.getContent());
        Long aiId = aiMessage.getId();
        String aiModel = aiMessage.getAiModel();

        SseEmitter emitter = new SseEmitter(30000L);
        sseExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("risk").data(JSONUtil.toJsonStr(riskEvent)));
                for (String fragment : fragments) {
                    emitter.send(SseEmitter.event().name("delta")
                            .data(JSONUtil.toJsonStr(Map.of("content", fragment))));
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                emitter.send(SseEmitter.event().name("done").data(JSONUtil.toJsonStr(Map.of(
                        "assistantMessageId", aiId,
                        "model", aiModel == null ? "" : aiModel))));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(JSONUtil.toJsonStr(Result.error(ResultCode.SYSTEM_ERROR))));
                } catch (IOException ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private Long parseSessionId(String sessionId) {
        Long dbSessionId = psychologicalSupportService.extractSessionId(sessionId);
        if (dbSessionId == null) {
            throw new BusinessException("会话ID格式错误");
        }
        return dbSessionId;
    }
}
