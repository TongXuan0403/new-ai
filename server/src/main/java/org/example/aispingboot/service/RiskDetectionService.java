package org.example.aispingboot.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 风险检测服务：MVP 采用“规则 + 输出审核”组合。
 * 输入前/调用前做规则检测，回复后做输出审核；模型分类与人工抽检预留扩展点。
 */
@Service
public class RiskDetectionService {

    public static final String RULE_VERSION = "risk-v0.3";
    public static final String MODEL_VERSION = "classifier-v0.1";

    private static final Pattern CRISIS_PATTERN = Pattern.compile(
            "自杀|杀了自己|结束生命|不想活了|活不下去|正在自伤|已经伤害自己|马上伤害|立即危险|伤害别人|杀人|想死|现在就死|已经割|已经吃药");
    private static final Pattern HARM_OTHERS_PATTERN = Pattern.compile("伤害别人|杀人|报复社会");
    private static final Pattern WARNING_PATTERN = Pattern.compile(
            "自残|伤害自己|撑不住|崩溃|失控|严重失眠|无望|没有意义|活得好累|坚持不下去|想消失");
    private static final Pattern CONCERN_PATTERN = Pattern.compile("持续低落|很无助|最近很痛苦|情绪很低");

    // 输出审核：禁止出现的诊断/用药/承诺/虚假介入等表达
    private static final Pattern FORBIDDEN_DIAGNOSIS = Pattern.compile("你患有|确诊为|诊断为|你就是抑郁症|这是抑郁症");
    private static final Pattern FORBIDDEN_MEDICATION = Pattern.compile("服用|用药|吃药|剂量|处方");
    private static final Pattern FORBIDDEN_PROMISE = Pattern.compile("只要坚持聊|和我聊就好|一定会好|不要告诉任何人|别告诉别人|我会一直陪着你");
    private static final Pattern FORBIDDEN_FAKE_HANDOFF = Pattern.compile("已通知老师|已通知辅导员|已联系专人|有人正在监控|我已帮你联系");

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskResult {
        private int level;
        private String riskType;
        private String actionType;
        private List<String> matchedRules;
    }

    /**
     * 输入风险检测。
     */
    public RiskResult detect(String text) {
        if (!StringUtils.hasText(text)) {
            return new RiskResult(0, "NONE", "NONE", List.of());
        }
        String normalized = text.replaceAll("\\s+", "");
        List<String> matched = new ArrayList<>();
        if (CRISIS_PATTERN.matcher(normalized).find()) {
            matched.add("CRISIS_KEYWORD");
            String type = HARM_OTHERS_PATTERN.matcher(normalized).find() ? "HARM_OTHERS" : "SELF_HARM";
            return new RiskResult(3, type, "SHOW_CRISIS_CARD", matched);
        }
        if (WARNING_PATTERN.matcher(normalized).find()) {
            matched.add("WARNING_KEYWORD");
            return new RiskResult(2, "EMOTIONAL_DISTRESS", "SHOW_GUIDANCE", matched);
        }
        if (CONCERN_PATTERN.matcher(normalized).find()) {
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
}
