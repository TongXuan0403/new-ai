package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.DTO.response.DataOverviewResponseDTO;
import org.example.aispingboot.entity.ArticleFavorite;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.entity.Exercise;
import org.example.aispingboot.entity.ExerciseCompletion;
import org.example.aispingboot.entity.KnowledgeArticle;
import org.example.aispingboot.entity.RiskEvent;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.mapper.ArticleFavoriteMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.example.aispingboot.mapper.ExerciseCompletionMapper;
import org.example.aispingboot.mapper.ExerciseMapper;
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
    private final ArticleFavoriteMapper favoriteMapper;
    private final ExerciseMapper exerciseMapper;
    private final ExerciseCompletionMapper completionMapper;
    private final RiskEventMapper riskEventMapper;

    public DataAnalyticsService(UserMapper userMapper, ConsultationSessionMapper sessionMapper,
                                EmotionDiaryMapper diaryMapper, KnowledgeArticleMapper articleMapper,
                                ArticleFavoriteMapper favoriteMapper, ExerciseMapper exerciseMapper,
                                ExerciseCompletionMapper completionMapper, RiskEventMapper riskEventMapper) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.diaryMapper = diaryMapper;
        this.articleMapper = articleMapper;
        this.favoriteMapper = favoriteMapper;
        this.exerciseMapper = exerciseMapper;
        this.completionMapper = completionMapper;
        this.riskEventMapper = riskEventMapper;
    }

    public DataOverviewResponseDTO overview() {
        Long totalUsers = userMapper.selectCount(null);
        Long activeUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1).eq(User::getUserType, 1));
        Long totalSessions = sessionMapper.selectCount(null);
        Long sessionUsers = (long) sessionMapper.selectObjs(new LambdaQueryWrapper<ConsultationSession>()
                .select(ConsultationSession::getUserId).groupBy(ConsultationSession::getUserId)).size();
        Long totalDiaries = diaryMapper.selectCount(null);
        Long diaryUsers = (long) diaryMapper.selectObjs(new LambdaQueryWrapper<EmotionDiary>()
                .select(EmotionDiary::getUserId).groupBy(EmotionDiary::getUserId)).size();
        Long publishedArticles = articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
        Long totalViews = articleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()
                .gt(KnowledgeArticle::getViewCount, 0));
        Long totalFavorites = favoriteMapper.selectCount(null);
        Long publishedExercises = exerciseMapper.selectCount(new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getStatus, "PUBLISHED"));
        Long exerciseCompletions = completionMapper.selectCount(null);

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
                .totalFavorites(totalFavorites)
                .publishedExercises(publishedExercises)
                .exerciseCompletions(exerciseCompletions)
                .riskEvents(riskTotal)
                .riskPending(riskPending)
                .riskByLevel(riskByLevel)
                .build();
    }
}
