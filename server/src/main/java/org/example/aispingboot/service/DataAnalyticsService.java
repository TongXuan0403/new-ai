package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.DTO.response.DataOverviewResponseDTO;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.entity.KnowledgeArticle;
import org.example.aispingboot.entity.RiskEvent;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.example.aispingboot.mapper.KnowledgeArticleMapper;
import org.example.aispingboot.mapper.RiskEventMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台运营概览：仅聚合脱敏数据，不返回私密原文。
 */
@Service
public class DataAnalyticsService {
    private final UserMapper userMapper;
    private final ConsultationSessionMapper sessionMapper;
    private final EmotionDiaryMapper diaryMapper;
    private final KnowledgeArticleMapper articleMapper;
    private final RiskEventMapper riskEventMapper;

    public DataAnalyticsService(UserMapper userMapper, ConsultationSessionMapper sessionMapper,
                                EmotionDiaryMapper diaryMapper, KnowledgeArticleMapper articleMapper,
                                RiskEventMapper riskEventMapper) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.diaryMapper = diaryMapper;
        this.articleMapper = articleMapper;
        this.riskEventMapper = riskEventMapper;
    }

    public DataOverviewResponseDTO overview() {
        Long totalUsers = userMapper.selectCount(null);
        Long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1).eq(User::getUserType, 1));
        Long totalSessions = sessionMapper.selectCount(null);
        Long sessionUsers = sessionMapper.selectCount(new LambdaQueryWrapper<ConsultationSession>()
                .groupBy(ConsultationSession::getUserId).last("HAVING COUNT(*) >= 0"));
        Long totalDiaries = diaryMapper.selectCount(null);
        Long diaryUsers = diaryMapper.selectCount(new LambdaQueryWrapper<EmotionDiary>()
                .groupBy(EmotionDiary::getUserId).last("HAVING COUNT(*) >= 0"));
        Long publishedArticles = articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
        Long totalViews = articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
                .gt(KnowledgeArticle::getViewCount, 0));

        List<RiskEvent> riskEvents = riskEventMapper.selectList(null);
        long riskTotal = riskEvents.size();
        long riskPending = riskEvents.stream().filter(e -> "待复核".equals(e.getStatus())).count();
        Map<String, Long> riskByLevel = new HashMap<>();
        for (RiskEvent e : riskEvents) {
            String key = "level" + e.getRiskLevel();
            riskByLevel.merge(key, 1L, Long::sum);
        }

        return DataOverviewResponseDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalSessions(totalSessions)
                .sessionUsers(sessionUsers)
                .totalDiaries(totalDiaries)
                .diaryUsers(diaryUsers)
                .publishedArticles(publishedArticles)
                .totalViews(totalViews)
                .riskEvents(riskTotal)
                .riskPending(riskPending)
                .riskByLevel(riskByLevel)
                .build();
    }
}
