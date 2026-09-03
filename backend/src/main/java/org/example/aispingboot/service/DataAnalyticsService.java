package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.AnalyticsOverviewVO;
import org.example.aispingboot.DTO.response.CampusReportVO;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.entity.GrowthPlan;
import org.example.aispingboot.entity.AppointmentRequest;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.example.aispingboot.mapper.GrowthPlanMapper;
import org.example.aispingboot.mapper.AppointmentRequestMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataAnalyticsService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private ConsultationSessionMapper consultationSessionMapper;

    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    @Resource
    private GrowthPlanMapper growthPlanMapper;

    @Resource
    private AppointmentRequestMapper appointmentRequestMapper;

    private static final int DAYS = 7;

    public AnalyticsOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(DAYS - 1);

        // ---- 全量统计 ----
        long totalUsers = userMapper.selectCount(null);
        long totalDiaries = emotionDiaryMapper.selectCount(null);
        long totalSessions = consultationSessionMapper.selectCount(null);

        long todayNewDiaries = emotionDiaryMapper.selectCount(
                new LambdaQueryWrapper<EmotionDiary>().ge(EmotionDiary::getDiaryDate, today));
        long todayNewSessions = consultationSessionMapper.selectCount(
                new LambdaQueryWrapper<ConsultationSession>().ge(ConsultationSession::getStartedAt, today.atStartOfDay()));

        // 全部日记，算情绪评分均值
        List<EmotionDiary> allDiaries = emotionDiaryMapper.selectList(null);
        double avgMoodScore = allDiaries.stream()
                .map(EmotionDiary::getMoodScore)
                .filter(s -> s != null)
                .mapToInt(Integer::intValue)
                .average().orElse(0);

        // ---- 最近 7 天明细 ----
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            dates.add(start.plusDays(i));
        }

        List<User> recentUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, start.atStartOfDay()));
        List<ConsultationSession> recentSessions = consultationSessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>().ge(ConsultationSession::getStartedAt, start.atStartOfDay()));
        List<EmotionDiary> recentDiaries = emotionDiaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>().ge(EmotionDiary::getDiaryDate, start));

        // 按天聚合
        Map<LocalDate, Long> newUsersByDay = recentUsers.stream()
                .collect(Collectors.groupingBy(u -> toDate(u.getCreatedAt()), Collectors.counting()));
        Map<LocalDate, List<Long>> sessionUsersByDay = recentSessions.stream()
                .collect(Collectors.groupingBy(s -> toDate(s.getStartedAt()),
                        Collectors.mapping(ConsultationSession::getUserId, Collectors.toList())));
        Map<LocalDate, Long> sessionCountByDay = recentSessions.stream()
                .collect(Collectors.groupingBy(s -> toDate(s.getStartedAt()), Collectors.counting()));
        Map<LocalDate, List<Long>> diaryUsersByDay = recentDiaries.stream()
                .collect(Collectors.groupingBy(d -> d.getDiaryDate(),
                        Collectors.mapping(EmotionDiary::getUserId, Collectors.toList())));
        Map<LocalDate, Long> diaryCountByDay = recentDiaries.stream()
                .collect(Collectors.groupingBy(EmotionDiary::getDiaryDate, Collectors.counting()));
        Map<LocalDate, List<Integer>> moodByDay = recentDiaries.stream()
                .filter(d -> d.getMoodScore() != null)
                .collect(Collectors.groupingBy(EmotionDiary::getDiaryDate,
                        Collectors.mapping(EmotionDiary::getMoodScore, Collectors.toList())));

        List<AnalyticsOverviewVO.TrendPoint> dailyTrend = new ArrayList<>();
        List<AnalyticsOverviewVO.TrendPoint> emotionTrend = new ArrayList<>();
        List<AnalyticsOverviewVO.UserActivityPoint> userActivity = new ArrayList<>();

        for (LocalDate date : dates) {
            List<Long> sessionUsers = sessionUsersByDay.getOrDefault(date, new ArrayList<>());
            List<Long> diaryUsers = diaryUsersByDay.getOrDefault(date, new ArrayList<>());
            long sessionCount = sessionCountByDay.getOrDefault(date, 0L);
            long diaryCount = diaryCountByDay.getOrDefault(date, 0L);
            long newUsers = newUsersByDay.getOrDefault(date, 0L);

            Set<Long> active = new HashSet<>();
            active.addAll(sessionUsers);
            active.addAll(diaryUsers);

            List<Integer> moods = moodByDay.getOrDefault(date, new ArrayList<>());
            double avg = moods.stream().mapToInt(Integer::intValue).average().orElse(0);

            dailyTrend.add(AnalyticsOverviewVO.TrendPoint.builder()
                    .date(date.toString())
                    .sessionCount(sessionCount)
                    .userCount((long) new HashSet<>(sessionUsers).size())
                    .build());
            emotionTrend.add(AnalyticsOverviewVO.TrendPoint.builder()
                    .date(date.toString())
                    .avgMoodScore(avg)
                    .recordCount(diaryCount)
                    .build());
            userActivity.add(AnalyticsOverviewVO.UserActivityPoint.builder()
                    .date(date.toString())
                    .activeUsers((long) active.size())
                    .newUsers(newUsers)
                    .diaryUsers((long) new HashSet<>(diaryUsers).size())
                    .consultationUsers((long) new HashSet<>(sessionUsers).size())
                    .build());
        }

        // 最近7天会话平均时长（基于会话首末消息时间差）
        double avgDurationMinutes = avgSessionDurationMinutes(recentSessions);

        AnalyticsOverviewVO.SystemOverview systemOverview = AnalyticsOverviewVO.SystemOverview.builder()
                .totalUsers(totalUsers)
                .activeUsers(recentActiveUsers(recentSessions, recentDiaries))
                .totalDiaries(totalDiaries)
                .todayNewDiaries(todayNewDiaries)
                .totalSessions(totalSessions)
                .todayNewSessions(todayNewSessions)
                .avgMoodScore(round2(avgMoodScore))
                .build();

        AnalyticsOverviewVO.ConsultationStats consultationStats = AnalyticsOverviewVO.ConsultationStats.builder()
                .totalSessions(totalSessions)
                .avgDurationMinutes(round2(avgDurationMinutes))
                .activeUsers(recentActiveUsers(recentSessions, recentDiaries))
                .dailyTrend(dailyTrend)
                .build();

        return AnalyticsOverviewVO.builder()
                .systemOverview(systemOverview)
                .consultationStats(consultationStats)
                .emotionTrend(emotionTrend)
                .userActivity(userActivity)
                .build();
    }

    private long recentActiveUsers(List<ConsultationSession> sessions, List<EmotionDiary> diaries) {
        Set<Long> users = new HashSet<>();
        sessions.forEach(s -> {
            if (s.getUserId() != null) users.add(s.getUserId());
        });
        diaries.forEach(d -> {
            if (d.getUserId() != null) users.add(d.getUserId());
        });
        return users.size();
    }

    private double avgSessionDurationMinutes(List<ConsultationSession> sessions) {
        if (sessions.isEmpty()) {
            return 0;
        }
        long totalSeconds = 0;
        long count = 0;
        for (ConsultationSession session : sessions) {
            ConsultationMessage first = consultationMessageMapper.selectOne(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .eq(ConsultationMessage::getSessionId, session.getId())
                            .orderByAsc(ConsultationMessage::getCreatedAt)
                            .last("limit 1"));
            ConsultationMessage last = consultationMessageMapper.selectOne(
                    new LambdaQueryWrapper<ConsultationMessage>()
                            .eq(ConsultationMessage::getSessionId, session.getId())
                            .orderByDesc(ConsultationMessage::getCreatedAt)
                            .last("limit 1"));
            if (first != null && last != null && first.getCreatedAt() != null && last.getCreatedAt() != null) {
                totalSeconds += Math.max(0, Duration.between(first.getCreatedAt(), last.getCreatedAt()).getSeconds());
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return totalSeconds / 60.0 / count;
    }

    private LocalDate toDate(LocalDateTime time) {
        return time == null ? null : time.toLocalDate();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 匿名聚合校园心理健康报告（纯聚合，不含任何个人数据）
     */
    public CampusReportVO campusReport() {
        LocalDate today = LocalDate.now();
        LocalDate start7 = today.minusDays(6);

        long totalUsers = userMapper.selectCount(null);
        long totalSessions = consultationSessionMapper.selectCount(null);
        long totalMessages = consultationMessageMapper.selectCount(null);
        long totalDiaries = emotionDiaryMapper.selectCount(null);
        long totalGrowthPlans = growthPlanMapper.selectCount(null);
        long totalAppointments = appointmentRequestMapper.selectCount(null);

        List<ConsultationSession> recentSessions = consultationSessionMapper.selectList(
                new LambdaQueryWrapper<ConsultationSession>().ge(ConsultationSession::getStartedAt, start7.atStartOfDay()));
        List<EmotionDiary> recentDiaries = emotionDiaryMapper.selectList(
                new LambdaQueryWrapper<EmotionDiary>().ge(EmotionDiary::getDiaryDate, start7));
        long activeUsers7d = recentActiveUsers(recentSessions, recentDiaries);

        // 全量日记：均值 + 主导情绪分布 + 评分区间分布 + 低情绪占比
        List<EmotionDiary> allDiaries = emotionDiaryMapper.selectList(null);
        double avgMood = allDiaries.stream()
                .map(EmotionDiary::getMoodScore).filter(s -> s != null)
                .mapToInt(Integer::intValue).average().orElse(0);

        Map<String, Long> emotionDist = new HashMap<>();
        for (EmotionDiary d : allDiaries) {
            if (d.getDominantEmotion() != null && !d.getDominantEmotion().trim().isEmpty()) {
                emotionDist.merge(d.getDominantEmotion().trim(), 1L, Long::sum);
            }
        }
        List<CampusReportVO.NameCount> emotionList = emotionDist.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> CampusReportVO.NameCount.builder().name(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        long[] moodBuckets = new long[5];
        long lowMoodCount = 0;
        long moodTotal = 0;
        for (EmotionDiary d : allDiaries) {
            Integer s = d.getMoodScore();
            if (s == null) continue;
            moodTotal++;
            int idx = s <= 2 ? 0 : (s <= 4 ? 1 : (s <= 6 ? 2 : (s <= 8 ? 3 : 4)));
            moodBuckets[idx]++;
            if (s <= 4) lowMoodCount++;
        }
        String[] bucketNames = {"0-2（低落）", "3-4（偏低）", "5-6（平稳）", "7-8（良好）", "9-10（愉悦）"};
        List<CampusReportVO.NameCount> moodList = new ArrayList<>();
        for (int i = 0; i < moodBuckets.length; i++) {
            moodList.add(CampusReportVO.NameCount.builder()
                    .name(bucketNames[i]).count(moodBuckets[i]).build());
        }

        // 近 7 天逐日趋势
        Map<LocalDate, Long> sessionByDay = recentSessions.stream()
                .collect(Collectors.groupingBy(s -> toDate(s.getStartedAt()), Collectors.counting()));
        Map<LocalDate, List<Long>> sessionUsersByDay = recentSessions.stream()
                .collect(Collectors.groupingBy(s -> toDate(s.getStartedAt()),
                        Collectors.mapping(ConsultationSession::getUserId, Collectors.toList())));
        Map<LocalDate, Long> diaryByDay = recentDiaries.stream()
                .collect(Collectors.groupingBy(EmotionDiary::getDiaryDate, Collectors.counting()));
        Map<LocalDate, List<Long>> diaryUsersByDay = recentDiaries.stream()
                .collect(Collectors.groupingBy(EmotionDiary::getDiaryDate,
                        Collectors.mapping(EmotionDiary::getUserId, Collectors.toList())));

        List<CampusReportVO.DailyPoint> dailyTrend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = start7.plusDays(i);
            Set<Long> active = new HashSet<>();
            if (sessionUsersByDay.get(date) != null) active.addAll(sessionUsersByDay.get(date));
            if (diaryUsersByDay.get(date) != null) active.addAll(diaryUsersByDay.get(date));
            dailyTrend.add(CampusReportVO.DailyPoint.builder()
                    .date(date.toString())
                    .sessionCount(sessionByDay.getOrDefault(date, 0L))
                    .diaryCount(diaryByDay.getOrDefault(date, 0L))
                    .activeUsers((long) active.size())
                    .build());
        }

        CampusReportVO.ReportOverview overview = CampusReportVO.ReportOverview.builder()
                .totalUsers(totalUsers)
                .totalSessions(totalSessions)
                .totalMessages(totalMessages)
                .totalDiaries(totalDiaries)
                .activeUsers7d(activeUsers7d)
                .avgMoodScore(round2(avgMood))
                .totalGrowthPlans(totalGrowthPlans)
                .totalAppointments(totalAppointments)
                .build();

        double lowMoodRatio = moodTotal == 0 ? 0 : round2(lowMoodCount * 100.0 / moodTotal);

        return CampusReportVO.builder()
                .generatedAt(LocalDateTime.now())
                .overview(overview)
                .emotionDistribution(emotionList)
                .moodDistribution(moodList)
                .dailyTrend(dailyTrend)
                .lowMoodRatio(lowMoodRatio)
                .build();
    }
}
