package org.example.aispingboot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 风险检测服务：MVP 采用“规则 + 输出审核”组合。
 * P1 起风险关键词规则来自后台生效的 RISK_RULE 版本（system_config_version），
 * 未配置时回退内置默认规则；规则版本可追溯（rule_version 写入风险事件）。
 */
@Service
public class RiskDetectionService {

    public static final String RULE_VERSION = "rule-v1.0";
    public static final String MODEL_VERSION = "classifier-v0.1";

    // 内置默认规则（与 V5 种子 rule-v1.0 保持一致）
    private static final String[] DEFAULT_CRISIS = {
            "自杀", "杀了自己", "结束生命", "不想活了", "活不下去", "正在自伤", "已经伤害自己",
            "马上伤害", "伤害别人", "杀人", "想死", "现在就死", "已经割", "已经吃药"};
    private static final String[] DEFAULT_HARM_OTHERS = {"伤害别人", "杀人", "报复社会"};
    private static final String[] DEFAULT_WARNING = {
            "自残", "伤害自己", "撑不住", "崩溃", "失控", "严重失眠", "无望",
            "没有意义", "活得好累", "坚持不下去", "想消失"};
    private static final String[] DEFAULT_CONCERN = {"持续低落", "很无助", "最近很痛苦", "情绪很低"};

    // 输出审核：禁止出现的诊断/用药/承诺/虚假介入等表达
    private static final Pattern FORBIDDEN_DIAGNOSIS = Pattern.compile("你患有|确诊为|诊断为|你就是抑郁症|这是抑郁症");
    private static final Pattern FORBIDDEN_MEDICATION = Pattern.compile("服用|用药|吃药|剂量|处方");
    private static final Pattern FORBIDDEN_PROMISE = Pattern.compile("只要坚持聊|和我聊就好|一定会好|不要告诉任何人|别告诉别人|我会一直陪着你");
    private static final Pattern FORBIDDEN_FAKE_HANDOFF = Pattern.compile("已通知老师|已通知辅导员|已联系专人|有人正在监控|我已帮你联系");

    private final ConfigVersionService configVersionService;
    private final ObjectMapper objectMapper;
    private final Map<String, RuleSet> compiledCache = new ConcurrentHashMap<>();
    private long lastCompiledAt = 0L;
    private RuleSet compiledRules = null;

    public RiskDetectionService(ConfigVersionService configVersionService, ObjectMapper objectMapper) {
        this.configVersionService = configVersionService;
        this.objectMapper = objectMapper;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskResult {
        private int level;
        private String riskType;
        private String actionType;
        private List<String> matchedRules;
    }

    @Data
    private static class RuleSet {
        private Pattern crisis;
        private Pattern harmOthers;
        private Pattern warning;
        private Pattern concern;
        private String source;
    }

    /**
     * 当前生效规则版本号；无生效版本时回退内置默认。
     */
    public String currentRuleVersion() {
        return configVersionService.getActiveVersionLabel(ConfigVersionService.TYPE_RISK_RULE, RULE_VERSION);
    }

    /**
     * 输入风险检测。
     */
    public RiskResult detect(String text) {
        if (!StringUtils.hasText(text)) {
            return new RiskResult(0, "NONE", "NONE", List.of());
        }
        String normalized = text.replaceAll("\\s+", "");
        RuleSet rules = resolveRules();
        List<String> matched = new ArrayList<>();
        if (rules.getCrisis().matcher(normalized).find()) {
            matched.add("CRISIS_KEYWORD");
            String type = rules.getHarmOthers().matcher(normalized).find() ? "HARM_OTHERS" : "SELF_HARM";
            return new RiskResult(3, type, "SHOW_CRISIS_CARD", matched);
        }
        if (rules.getWarning().matcher(normalized).find()) {
            matched.add("WARNING_KEYWORD");
            return new RiskResult(2, "EMOTIONAL_DISTRESS", "SHOW_GUIDANCE", matched);
        }
        if (rules.getConcern().matcher(normalized).find()) {
            matched.add("CONCERN_KEYWORD");
            return new RiskResult(1, "EMOTIONAL_DISTRESS", "SHOW_GUIDANCE", matched);
        }
        return new RiskResult(0, "NONE", "NONE", List.of());
    }

    /**
     * 输出审核：检查 AI 回复是否越界。
     */
    public List<String> auditOutput(String reply) {
        if (!StringUtils.hasText(reply)) {
            return List.of();
        }
        List<String> hits = new ArrayList<>();
        if (FORBIDDEN_DIAGNOSIS.matcher(reply).find()) {
            hits.add("DIAGNOSIS_CLAIM");
        }
        if (FORBIDDEN_MEDICATION.matcher(reply).find()) {
            hits.add("MEDICATION_ADVICE");
        }
        if (FORBIDDEN_PROMISE.matcher(reply).find()) {
            hits.add("UNSAFE_PROMISE");
        }
        if (FORBIDDEN_FAKE_HANDOFF.matcher(reply).find()) {
            hits.add("FAKE_HANDOFF");
        }
        return hits;
    }

    /**
     * 危机场景的稳定安全引导（不承诺、不保密、不替代救援）。
     */
    public String crisisReply() {
        return "听起来你现在承受得非常重。请先把安全放在第一位：如果你可能伤害自己或他人，或已经处于立即危险，请立刻拨打 120、110 或 12356，并联系身边可信任的人陪你一起处理。";
    }

    /**
     * 预警场景的引导。
     */
    public String guidanceReply() {
        return "我听到你已经累到有些撑不住了。先不要求自己把所有事都解决，我们可以只看接下来的十分钟：找一个相对安全、有人在附近的地方，喝几口水，然后联系一位你信任的人。这样的困扰如果持续或加重，建议联系校内心理中心或专业人员。";
    }

    // ------------------------------------------------------------------
    // 规则解析：生效版本优先，内置默认兜底；5 秒缓存已编译规则
    // ------------------------------------------------------------------

    private RuleSet resolveRules() {
        String content = configVersionService.getActiveContent(ConfigVersionService.TYPE_RISK_RULE, "");
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(content)) {
            String key = "cfg|" + content;
            RuleSet cached = compiledCache.get(key);
            if (cached != null) {
                return cached;
            }
            RuleSet parsed = parseRuleContent(content);
            if (parsed != null) {
                if (compiledCache.size() > 20) {
                    compiledCache.clear();
                }
                compiledCache.put(key, parsed);
                return parsed;
            }
            // 解析失败：回退内置
            return fallbackRules(now);
        }
        return fallbackRules(now);
    }

    private RuleSet fallbackRules(long now) {
        if (compiledRules == null || now - lastCompiledAt > 5000L) {
            compiledRules = RuleSetHolder.buildDefault();
            lastCompiledAt = now;
        }
        return compiledRules;
    }

    private RuleSet parseRuleContent(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            List<String> crisis = readArray(root, "crisis");
            List<String> harmOthers = readArray(root, "harmOthers");
            List<String> warning = readArray(root, "warning");
            List<String> concern = readArray(root, "concern");
            RuleSet rules = new RuleSet();
            rules.setCrisis(compile(crisis.isEmpty() ? List.of(DEFAULT_CRISIS) : crisis));
            rules.setHarmOthers(compile(harmOthers.isEmpty() ? List.of(DEFAULT_HARM_OTHERS) : harmOthers));
            rules.setWarning(compile(warning.isEmpty() ? List.of(DEFAULT_WARNING) : warning));
            rules.setConcern(compile(concern.isEmpty() ? List.of(DEFAULT_CONCERN) : concern));
            rules.setSource("db:" + currentRuleVersion());
            return rules;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readArray(JsonNode root, String field) {
        JsonNode node = root.path(field);
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                if (item.isTextual() && StringUtils.hasText(item.asText())) {
                    result.add(item.asText().trim());
                }
            });
        }
        return result;
    }

    private Pattern compile(List<String> keywords) {
        String joined = keywords.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        return Pattern.compile(joined);
    }

    private static final class RuleSetHolder {
        static RuleSet buildDefault() {
            RuleSet rules = new RuleSet();
            rules.setCrisis(compile(List.of(DEFAULT_CRISIS)));
            rules.setHarmOthers(compile(List.of(DEFAULT_HARM_OTHERS)));
            rules.setWarning(compile(List.of(DEFAULT_WARNING)));
            rules.setConcern(compile(List.of(DEFAULT_CONCERN)));
            rules.setSource("builtin:" + RULE_VERSION);
            return rules;
        }

        private static Pattern compile(List<String> keywords) {
            String joined = keywords.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));
            return Pattern.compile(joined);
        }
    }
}
