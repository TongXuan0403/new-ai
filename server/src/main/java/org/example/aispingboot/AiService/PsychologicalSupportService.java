package org.example.aispingboot.AiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aispingboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispingboot.DTO.response.ConsultationSessionResponseDTO;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.RiskEvent;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.service.AiConfigService;
import org.example.aispingboot.service.ConsultationMessageService;
import org.example.aispingboot.service.ConsultationSessionService;
import org.example.aispingboot.service.KnowledgeBaseService;
import org.example.aispingboot.service.RiskDetectionService;
import org.example.aispingboot.service.RiskEventService;
import org.example.aispingboot.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 心理支持服务：AI 回复生成 + 风险检测集成 + 输出审核。
 * 流程：校验归属 -> 输入风险检测 -> 用户消息落库 -> 危机分支/模型分支生成回复
 *      -> 输出审核 -> AI 消息落库 -> 风险事件记录 -> 会话风险等级更新。
 */
@Service
public class PsychologicalSupportService {
    private static final Pattern SPLIT_PATTERN = Pattern.compile("(?<=[。！？!?；;，,\\n])");

    private final UserService userService;
    private final ConsultationSessionService consultationSessionService;
    private final ConsultationMessageService consultationMessageService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final AiConfigService aiConfigService;
    private final RiskDetectionService riskDetectionService;
    private final RiskEventService riskEventService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public PsychologicalSupportService(UserService userService,
                                       ConsultationSessionService consultationSessionService,
                                       ConsultationMessageService consultationMessageService,
                                       KnowledgeBaseService knowledgeBaseService,
                                       AiConfigService aiConfigService,
                                       RiskDetectionService riskDetectionService,
                                       RiskEventService riskEventService,
                                       ObjectMapper objectMapper) {
        this.userService = userService;
        this.consultationSessionService = consultationSessionService;
        this.consultationMessageService = consultationMessageService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.aiConfigService = aiConfigService;
        this.riskDetectionService = riskDetectionService;
        this.riskEventService = riskEventService;
        this.objectMapper = objectMapper;
    }

    public static class ChatResult {
        public Long userMessageId;
        public Long assistantMessageId;
        public String reply;
        public int riskLevel;
        public String riskType;
        public String actionType;
        public Long riskEventId;
        public String model;
        public List<String> auditHits;
    }

    /**
     * 二阶段流程第一步+第二步（MVP 合并实现）：
     * 提交用户消息并生成 AI 回复，全部落库，返回结构化结果。
     */
    public ChatResult submitAndGenerate(Long userId, Long sessionId, String userMessage, String requestedModel) {
        consultationSessionService.validateSessionOwnership(sessionId, userId);

        RiskDetectionService.RiskResult risk = riskDetectionService.detect(userMessage);
        ConsultationMessage userMsg = consultationMessageService.saveUserMessage(sessionId, userMessage, risk.getLevel());

        String model = aiConfigService.normalizeRequestedModel(requestedModel);
        String reply;
        List<String> auditHits = new ArrayList<>();
        Long riskEventId = null;
        boolean crisisCard = risk.getLevel() >= 3;

        if (risk.getLevel() >= 3) {
            // 危机分支：不调用模型，只输出稳定安全引导
            reply = riskDetectionService.crisisReply();
        } else {
            reply = generateReply(userId, sessionId, userMessage, model);
            auditHits = riskDetectionService.auditOutput(reply);
            if (!auditHits.isEmpty()) {
                // 输出越界时替换为安全兜底
                reply = buildFallbackReply(userMessage, model);
            }
        }

        RiskEvent event = riskEventService.create(userId, sessionId, userMsg.getId(), risk, userMessage, crisisCard);
        if (event != null) {
            riskEventId = event.getId();
        }

        ConsultationMessage aiMsg = consultationMessageService.saveAssistantMessage(sessionId, reply, model, risk.getLevel());
        consultationSessionService.updateRiskLevel(sessionId, risk.getLevel());

        // 有足够上下文后更新默认标题
        if (consultationMessageService.listMessages(sessionId).stream()
                .filter(m -> m.getSenderType() != null && m.getSenderType() == 1).count() >= 2) {
            consultationSessionService.autoTitle(sessionId);
        }

        ChatResult result = new ChatResult();
        result.userMessageId = userMsg.getId();
        result.assistantMessageId = aiMsg.getId();
        result.reply = reply;
        result.riskLevel = risk.getLevel();
        result.riskType = risk.getRiskType();
        result.actionType = risk.getActionType();
        result.riskEventId = riskEventId;
        result.model = model;
        result.auditHits = auditHits;
        return result;
    }

    /**
     * 生成 AI 回复：结合历史与知识上下文调用模型，无 Key 时本地兜底。
     */
    private String generateReply(Long userId, Long sessionId, String userMessage, String model) {
        var config = aiConfigService.resolveRuntimeConfig();
        String knowledgeContext = knowledgeBaseService.buildKnowledgeContext(userMessage, 3);
        String reply = callModel(userMessage, sessionId, knowledgeContext, config, model);
        if (!StringUtils.hasText(reply)) {
            reply = buildFallbackReply(userMessage, model);
        }
        return reply;
    }

    private String callModel(String userMessage, Long sessionId, String knowledgeContext,
                             org.example.aispingboot.entity.AiRuntimeConfig config, String model) {
        if (!StringUtils.hasText(config.getApiKey()) || !StringUtils.hasText(config.getBaseUrl())) {
            return buildFallbackReply(userMessage, model);
        }
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT + "\n\n" + knowledgeContext
            ));
            // 追加最近的历史（最近 6 条，最多 3 轮）
            List<ConsultationMessage> history = consultationMessageService.listMessages(sessionId);
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                ConsultationMessage m = history.get(i);
                String role = (m.getSenderType() != null && m.getSenderType() == 1) ? "user" : "assistant";
                if (StringUtils.hasText(m.getContent())) {
                    messages.add(Map.of("role", role, "content", m.getContent()));
                }
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("messages", messages);
            payload.put("stream", false);
            payload.put("temperature", 0.6);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(config.getBaseUrl()) + "/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return buildFallbackReply(userMessage, model);
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = contentNode.isTextual() ? contentNode.asText() : "";
            if (!StringUtils.hasText(content)) {
                return buildFallbackReply(userMessage, model);
            }
            return content.trim();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildFallbackReply(userMessage, model);
        } catch (Exception e) {
            return buildFallbackReply(userMessage, model);
        }
    }

    /**
     * 本地兜底回复：无 Key 或调用失败时，基于关键词给出安全、克制的回应。
     */
    public String buildFallbackReply(String userMessage, String model) {
        String lower = String.valueOf(userMessage).toLowerCase(Locale.ROOT);
        String knowledgeHint = "我会结合已审核的知识内容和你描述的现状一起看。";
        if (lower.contains("考试") || lower.contains("成绩") || lower.contains("学业")) {
            return "先把考试拆成可控制的两步：" + knowledgeHint + "今晚只做复盘，明天只关注第一道题。先让身体慢下来，再处理想法。（模型：" + model + "）";
        }
        if (lower.contains("睡") || lower.contains("失眠") || lower.contains("睡不着")) {
            return "睡不着的时候，越要求自己马上入睡，身体越容易保持警觉。今晚可以先把目标改成“让身体休息十分钟”，把明天要处理的事写下来，再做三轮不刻意用力的慢呼吸。" + knowledgeHint + "（模型：" + model + "）";
        }
        if (lower.contains("关系") || lower.contains("冲突") || lower.contains("朋友")) {
            return "关系里的冲突往往先卡在“没有被说清楚”的部分。可以从事实、感受、需要、请求四步开始，先只把事件和对你的影响说清楚。" + knowledgeHint + "（模型：" + model + "）";
        }
        if (lower.contains("焦虑") || lower.contains("紧张") || lower.contains("担心")) {
            return "我听到你正在担心一件还没有发生的事，身体也许已经先进入了警觉状态。可以先问自己：现在最担心的具体结果是什么？然后只选一个十五分钟内能做的小动作。" + knowledgeHint + "（模型：" + model + "）";
        }
        return "我听到你正在努力把事情撑住，同时又有些难受。我们可以先不急着解决全部问题，只把当下最重的一部分拿出来看：这件事里，最让你担心的结果是什么？如果愿意，也可以先让肩膀放低一点，做一次缓慢的呼气。" + knowledgeHint + "（模型：" + model + "）";
    }

    /**
     * 将完整回复按句子切分为流式片段。
     */
    public String[] splitReply(String reply) {
        if (!StringUtils.hasText(reply)) {
            return new String[]{""};
        }
        String[] parts = SPLIT_PATTERN.split(reply);
        return parts.length == 0 ? new String[]{reply} : parts;
    }

    public Long extractSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        String rawId = sessionId.startsWith("session_") ? sessionId.substring("session_".length()) : sessionId;
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
