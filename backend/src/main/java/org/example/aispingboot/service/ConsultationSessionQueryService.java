package org.example.aispingboot.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.SessionEmotionVO;
import org.example.aispingboot.DTO.response.SessionListItemVO;
import org.example.aispingboot.DTO.response.SessionMessageVO;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsultationSessionQueryService {
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;

    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    @Resource
    private UserMapper userMapper;

    private static final List<String> CRISIS_WORDS = Arrays.asList("想死", "自杀", "轻生", "不想活", "结束生命", "活不下去", "一了百了");
    private static final List<String> SEVERE_NEGATIVE_WORDS = Arrays.asList("绝望", "崩溃", "撑不下去", "受不了", "彻底没救");
    private static final List<String> ANXIETY_WORDS = Arrays.asList("焦虑", "紧张", "担心", "害怕", "不安", "心慌", "失眠", "睡不着", "压力大", "压力好大");
    private static final List<String> LOW_MOOD_WORDS = Arrays.asList("难过", "伤心", "委屈", "低落", "没意思", "空虚", "孤独", "寂寞", "好累", "疲惫", "心烦", "抑郁");
    private static final List<String> POSITIVE_WORDS = Arrays.asList("开心", "高兴", "愉快", "轻松", "顺利", "不错", "好多了", "满意", "幸福", "治愈");

    /**
     * 会话列表。管理员查看全部，普通用户查看自己的。
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<SessionListItemVO> listSessions(
            Long userId, boolean isAdmin, int currentPage, int size) {
        int safePage = Math.max(currentPage, 1);
        int safeSize = Math.max(size, 1);

        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(ConsultationSession::getUserId, userId);
        }
        wrapper.orderByDesc(ConsultationSession::getStartedAt);

        Page<ConsultationSession> page = consultationSessionMapper.selectPage(new Page<>(safePage, safeSize), wrapper);

        List<SessionListItemVO> records = page.getRecords().stream().map(session -> {
            // 用户消息数（AI 消息也计入，这里统计全部消息数）
            Long messageCount = consultationMessageMapper.selectCount(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .eq(ConsultationMessage::getSessionId, session.getId()));
            // 最后一条消息
            ConsultationMessage last = consultationMessageMapper.selectOne(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .eq(ConsultationMessage::getSessionId, session.getId())
                            .orderByDesc(ConsultationMessage::getCreatedAt)
                            .last("limit 1"));
            // 第一条消息时间用于估算时长
            ConsultationMessage first = consultationMessageMapper.selectOne(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .eq(ConsultationMessage::getSessionId, session.getId())
                            .orderByAsc(ConsultationMessage::getCreatedAt)
                            .last("limit 1"));
            long durationMinutes = 0;
            if (first != null && last != null && first.getCreatedAt() != null && last.getCreatedAt() != null) {
                durationMinutes = Math.max(0, Duration.between(first.getCreatedAt(), last.getCreatedAt()).toMinutes());
            }
            String username = "";
            if (session.getUserId() != null) {
                User user = userMapper.selectById(session.getUserId());
                if (user != null) {
                    username = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
                }
            }
            return SessionListItemVO.builder()
                    .id(session.getId())
                    .sessionTitle(session.getSessionTitle())
                    .username(username)
                    .userNickname(username)
                    .startedAt(session.getStartedAt())
                    .lastMessageContent(last != null ? excerpt(last.getContent(), 80) : null)
                    .lastMessageTime(last != null ? last.getCreatedAt() : session.getStartedAt())
                    .messageCount(messageCount.intValue())
                    .durationMinutes(durationMinutes)
                    .build();
        }).collect(Collectors.toList());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SessionListItemVO> result = new Page<>(safePage, safeSize, page.getTotal());
        result.setRecords(records);
        return result;
    }

    public List<SessionMessageVO> listMessages(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = validateAccess(sessionId, userId, isAdmin);
        LambdaQueryWrapper<ConsultationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationMessage::getSessionId, session.getId())
                .orderByAsc(ConsultationMessage::getCreatedAt);
        return consultationMessageMapper.selectList(wrapper).stream()
                .map(m -> SessionMessageVO.builder()
                        .id(m.getId())
                        .senderType(m.getSenderType())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public void deleteSession(Long sessionId, Long userId, boolean isAdmin) {
        validateAccess(sessionId, userId, isAdmin);
        // 删除会话及消息
        consultationMessageMapper.delete(
                new LambdaQueryWrapper<ConsultationMessage>().eq(ConsultationMessage::getSessionId, sessionId));
        consultationSessionMapper.deleteById(sessionId);
    }

    /**
     * 会话情绪分析（基于会话内用户消息的关键词规则）
     */
    public SessionEmotionVO analyzeEmotion(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = validateAccess(sessionId, userId, isAdmin);

        List<ConsultationMessage> userMessages = consultationMessageMapper.selectList(
                new LambdaQueryWrapper<ConsultationMessage>()
                        .eq(ConsultationMessage::getSessionId, session.getId())
                        .eq(ConsultationMessage::getSenderType, 1)
                        .orderByAsc(ConsultationMessage::getCreatedAt));

        StringBuilder textBuilder = new StringBuilder();
        for (ConsultationMessage m : userMessages) {
            textBuilder.append(m.getContent()).append("\n");
        }
        String text = textBuilder.toString().toLowerCase();

        SessionEmotionVO result = analyzeText(text);

        // 缓存分析结果到会话表
        ConsultationSession update = ConsultationSession.builder()
                .id(session.getId())
                .lastEmotionAnalysis(JSONUtil.toJsonStr(result))
                .lastEmotionUpdatedAt(LocalDateTime.now())
                .build();
        consultationSessionMapper.updateById(update);

        return result;
    }

    private SessionEmotionVO analyzeText(String text) {
        if (!StringUtils.hasText(text)) {
            return SessionEmotionVO.builder()
                    .primaryEmotion("平静")
                    .emotionScore(6)
                    .isNegative(false)
                    .riskLevel(0)
                    .suggestion("感谢你的分享，随时都可以找我聊聊。")
                    .improvementSuggestions(Arrays.asList("尝试记录每天的小确幸", "保持规律的作息"))
                    .riskDescription("未检测到明显心理风险。")
                    .build();
        }

        boolean crisis = containsAny(text, CRISIS_WORDS);
        boolean severe = containsAny(text, SEVERE_NEGATIVE_WORDS);
        boolean anxiety = containsAny(text, ANXIETY_WORDS);
        boolean lowMood = containsAny(text, LOW_MOOD_WORDS);
        boolean positive = containsAny(text, POSITIVE_WORDS);

        String primaryEmotion;
        int score;
        int riskLevel;
        boolean isNegative;

        if (crisis) {
            primaryEmotion = "情绪危机";
            score = 2;
            riskLevel = 3;
            isNegative = true;
        } else if (severe) {
            primaryEmotion = "深度低落";
            score = 3;
            riskLevel = 2;
            isNegative = true;
        } else if (anxiety) {
            primaryEmotion = "焦虑不安";
            score = 4;
            riskLevel = 1;
            isNegative = true;
        } else if (lowMood) {
            primaryEmotion = "情绪低落";
            score = 4;
            riskLevel = 1;
            isNegative = true;
        } else if (positive) {
            primaryEmotion = "状态良好";
            score = 8;
            riskLevel = 0;
            isNegative = false;
        } else {
            primaryEmotion = "平稳";
            score = 6;
            riskLevel = 0;
            isNegative = false;
        }

        // 若同时存在正面词，略微上调分数
        if (positive && (anxiety || lowMood || severe)) {
            score = Math.min(10, score + 1);
        }

        String suggestion;
        List<String> improvements;
        String riskDescription;

        switch (riskLevel) {
            case 3 -> {
                suggestion = "我注意到你提到了很严重的困扰，你的安全和健康是第一位的。请立即联系你信任的人，或拨打心理援助热线（如全国24小时心理援助热线 12356），也可以联系学校心理咨询中心。";
                improvements = Arrays.asList("不要一个人扛着，尽快联系身边的人", "必要时寻求专业心理帮助或危机干预");
                riskDescription = "检测到可能危及自身安全的表述，建议立即寻求专业帮助。";
            }
            case 2 -> {
                suggestion = "你现在承受的压力比较大，请给自己一些空间，不要苛责自己。建议尽快寻求学校心理咨询中心或专业咨询师的帮助。";
                improvements = Arrays.asList("尝试深呼吸放松练习", "把心里的烦恼写下来", "联系信任的朋友或老师");
                riskDescription = "检测到较强烈的负面情绪，建议关注并及时寻求支持。";
            }
            case 1 -> {
                suggestion = "我能感受到你现在的困扰。试着把问题拆小，一步一步来，也可以先做几次深呼吸让自己平静下来。";
                improvements = Arrays.asList("每天给自己 15 分钟放空时间", "适度运动帮助缓解焦虑", "保持规律睡眠，减少熬夜");
                riskDescription = "存在一定程度的焦虑或压力，可通过自我调节改善。";
            }
            default -> {
                suggestion = "很高兴看到你状态不错，继续保持积极的生活方式，有什么想聊的都可以随时找我。";
                improvements = Arrays.asList("保持规律的作息和运动", "多与朋友交流分享", "记录每日情绪变化");
                riskDescription = "情绪状态平稳，未检测到明显风险。";
            }
        }

        return SessionEmotionVO.builder()
                .primaryEmotion(primaryEmotion)
                .emotionScore(score)
                .isNegative(isNegative)
                .riskLevel(riskLevel)
                .suggestion(suggestion)
                .improvementSuggestions(improvements)
                .riskDescription(riskDescription)
                .build();
    }

    private boolean containsAny(String text, List<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private ConsultationSession validateAccess(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!isAdmin && (userId == null || !userId.equals(session.getUserId()))) {
            throw new BusinessException("无权访问该会话");
        }
        return session;
    }

    private String excerpt(String html, int maxLength) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
