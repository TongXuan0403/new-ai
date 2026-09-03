package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispingboot.DTO.response.EmotionDiaryPageVO;
import org.example.aispingboot.DTO.response.EmotionDiaryVO;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmotionDiaryService {
    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    @Resource
    private UserMapper userMapper;

    public EmotionDiaryVO add(Long userId, String userName, EmotionDiaryCreateDTO dto) {
        LocalDate diaryDate;
        try {
            diaryDate = LocalDate.parse(dto.getDiaryDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            throw new BusinessException("日期格式不正确，应为 YYYY-MM-DD");
        }
        Integer moodScore = dto.getMoodScore();
        if (moodScore == null) {
            moodScore = 5;
        }

        EmotionDiary diary = EmotionDiary.builder()
                .userId(userId)
                .userName(userName)
                .diaryDate(diaryDate)
                .moodScore(moodScore)
                .dominantEmotion(dto.getDominantEmotion())
                .emotionTriggers(dto.getEmotionTriggers())
                .diaryContent(dto.getDiaryContent())
                .sleepQuality(dto.getSleepQuality())
                .stressLevel(dto.getStressLevel())
                .createdAt(LocalDateTime.now())
                .build();
        emotionDiaryMapper.insert(diary);
        return toVO(diary);
    }

    public EmotionDiaryPageVO adminPage(int currentPage, int size, String keyword, String date) {
        int safePage = Math.max(currentPage, 1);
        int safeSize = Math.max(size, 1);
        LambdaQueryWrapper<EmotionDiary> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EmotionDiary::getUserName, keyword)
                    .or().like(EmotionDiary::getDiaryContent, keyword)
                    .or().like(EmotionDiary::getDominantEmotion, keyword));
        }
        if (StringUtils.hasText(date)) {
            try {
                wrapper.eq(EmotionDiary::getDiaryDate, LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception ignore) {
                // 忽略无效日期
            }
        }
        wrapper.orderByDesc(EmotionDiary::getCreatedAt);
        Page<EmotionDiary> page = emotionDiaryMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<EmotionDiaryVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return EmotionDiaryPageVO.builder().records(records).total(page.getTotal()).build();
    }

    public void delete(Long id) {
        if (emotionDiaryMapper.selectById(id) == null) {
            throw new BusinessException("日记不存在");
        }
        emotionDiaryMapper.deleteById(id);
    }
    private static final ObjectMapper AI_MAPPER = new ObjectMapper();

    /**
     * 基于日记内容生成轻量 AI 情绪分析 JSON（字段对齐管理端情绪日志页展示）
     */
    private String buildAiAnalysis(EmotionDiary diary) {
        int score = diary.getMoodScore() == null ? 5 : diary.getMoodScore();
        boolean negative = score <= 4;
        int risk = score <= 2 ? 2 : (score <= 4 ? 1 : 0);
        String dominant = (diary.getDominantEmotion() != null && !diary.getDominantEmotion().trim().isEmpty())
                ? diary.getDominantEmotion()
                : (score >= 7 ? "愉悦" : score >= 5 ? "平稳" : "低落");
        String riskDesc;
        String suggestion;
        List<String> actions = new ArrayList<>();
        if (risk >= 2) {
            riskDesc = "情绪评分偏低，建议关注并及时寻求支持";
            suggestion = "最近您可能感到压力较大。请优先照顾自己，必要时向家人朋友或专业人士倾诉。";
            actions.add("进行5分钟深呼吸放松练习");
            actions.add("给自己一个温暖的小奖励");
            actions.add("记录下此刻真实的想法");
        } else if (risk == 1) {
            riskDesc = "情绪略有波动，建议适当放松";
            suggestion = "试着放慢节奏，给自己一些缓冲的时间，情绪会慢慢平复。";
            actions.add("散步或轻度运动10分钟");
            actions.add("听一首喜欢的音乐");
            actions.add("给朋友发一条问候消息");
        } else {
            riskDesc = "当前情绪状态良好";
            suggestion = "保持这份好心情，把它分享给身边的人吧。";
            actions.add("继续保持规律的作息");
            actions.add("主动记录开心的小事");
            actions.add("将好心情传递给他人");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("primaryEmotion", dominant);
        m.put("emotionScore", score * 10);
        m.put("isNegative", negative);
        m.put("riskLevel", risk);
        m.put("riskDescription", riskDesc);
        m.put("suggestion", suggestion);
        m.put("improvementSuggestions", actions);
        try {
            return AI_MAPPER.writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private EmotionDiaryVO toVO(EmotionDiary diary) {
        String username = null;
        String nickname = null;
        if (diary.getUserId() != null) {
            User user = userMapper.selectById(diary.getUserId());
            if (user != null) {
                username = user.getUsername();
                nickname = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
            }
        }
        return EmotionDiaryVO.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .username(username)
                .nickname(nickname)
                .diaryDate(diary.getDiaryDate())
                .moodScore(diary.getMoodScore())
                .dominantEmotion(diary.getDominantEmotion())
                .emotionTriggers(diary.getEmotionTriggers())
                .diaryContent(diary.getDiaryContent())
                .sleepQuality(diary.getSleepQuality())
                .stressLevel(diary.getStressLevel())
                .createdAt(diary.getCreatedAt())
                .aiEmotionAnalysis(buildAiAnalysis(diary))
                .build();
    }
}
