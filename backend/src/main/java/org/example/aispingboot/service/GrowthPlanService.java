package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.command.GrowthPlanCreateDTO;
import org.example.aispingboot.DTO.response.GrowthPlanPageVO;
import org.example.aispingboot.DTO.response.GrowthPlanVO;
import org.example.aispingboot.entity.GrowthPlan;
import org.example.aispingboot.entity.GrowthPlanProgress;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.GrowthPlanMapper;
import org.example.aispingboot.mapper.GrowthPlanProgressMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 主题化成长计划服务
 */
@Service
public class GrowthPlanService {

    @Resource
    private GrowthPlanMapper growthPlanMapper;

    @Resource
    private GrowthPlanProgressMapper growthPlanProgressMapper;

    /**
     * 分页查询成长计划。非管理员仅看已发布；管理员可筛选全部状态。
     */
    public GrowthPlanPageVO page(String theme, Integer status, boolean isAdmin, Long userId,
                                 int currentPage, int size) {
        int safePage = Math.max(currentPage, 1);
        int safeSize = Math.max(size, 1);
        Integer effectiveStatus = status;
        if (!isAdmin) {
            effectiveStatus = 1;
        }
        LambdaQueryWrapper<GrowthPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(theme), GrowthPlan::getTheme, theme)
                .eq(effectiveStatus != null, GrowthPlan::getStatus, effectiveStatus)
                .orderByDesc(GrowthPlan::getCreatedAt);
        Page<GrowthPlan> page = growthPlanMapper.selectPage(new Page<>(safePage, safeSize), wrapper);

        Map<Long, GrowthPlanProgress> myProgress = loadMyProgress(userId, page.getRecords());
        List<GrowthPlanVO> records = page.getRecords().stream()
                .map(p -> toVO(p, myProgress.get(p.getId())))
                .collect(Collectors.toList());
        return GrowthPlanPageVO.builder().records(records).total(page.getTotal()).build();
    }

    /**
     * 计划详情（前台仅可查看已发布；increaseView 为 true 时增加浏览量）
     */
    public GrowthPlanVO detail(Long id, Long userId, boolean increaseView) {
        GrowthPlan plan = growthPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (increaseView && plan.getStatus() != null && plan.getStatus() == 1) {
            GrowthPlan update = GrowthPlan.builder()
                    .id(id)
                    .viewCount((plan.getViewCount() == null ? 0 : plan.getViewCount()) + 1)
                    .build();
            growthPlanMapper.updateById(update);
        }
        GrowthPlanProgress progress = findProgress(userId, id);
        return toVO(plan, progress);
    }

    public GrowthPlanVO create(GrowthPlanCreateDTO dto) {
        GrowthPlan plan = GrowthPlan.builder()
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .theme(dto.getTheme())
                .content(dto.getContent())
                .durationDays(dto.getDurationDays())
                .reviewer(dto.getReviewer())
                .status(dto.getStatus() == null ? 0 : dto.getStatus())
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        if (plan.getStatus() == 1) {
            plan.setReviewedAt(LocalDateTime.now());
        }
        growthPlanMapper.insert(plan);
        return toVO(plan, null);
    }

    public GrowthPlanVO update(Long id, GrowthPlanCreateDTO dto) {
        GrowthPlan exist = growthPlanMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("计划不存在");
        }
        GrowthPlan.GrowthPlanBuilder ub = GrowthPlan.builder()
                .id(id)
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .theme(dto.getTheme())
                .content(dto.getContent())
                .durationDays(dto.getDurationDays())
                .reviewer(dto.getReviewer())
                .updatedAt(LocalDateTime.now());
        if (dto.getStatus() != null) {
            ub.status(dto.getStatus());
            // 从草稿变为已发布时记录审核时间
            if (dto.getStatus() == 1 && (exist.getStatus() == null || exist.getStatus() != 1)) {
                ub.reviewedAt(LocalDateTime.now());
            }
        }
        growthPlanMapper.updateById(ub.build());
        return detail(id, null, false);
    }

    public void updateStatus(Long id, Integer status) {
        GrowthPlan exist = growthPlanMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("计划不存在");
        }
        GrowthPlan.GrowthPlanBuilder ub = GrowthPlan.builder().id(id).status(status);
        if (status != null && status == 1 && (exist.getStatus() == null || exist.getStatus() != 1)) {
            ub.reviewedAt(LocalDateTime.now());
        }
        growthPlanMapper.updateById(ub.build());
    }

    public void delete(Long id) {
        if (growthPlanMapper.selectById(id) == null) {
            throw new BusinessException("计划不存在");
        }
        growthPlanMapper.deleteById(id);
    }

    /**
     * 用户更新计划进度；达到 100 视为完成
     */
    public GrowthPlanProgress updateProgress(Long userId, Long planId, Integer progress) {
        if (growthPlanMapper.selectById(planId) == null) {
            throw new BusinessException("计划不存在");
        }
        GrowthPlanProgress exist = findProgress(userId, planId);
        int safeProgress = progress == null ? 0 : Math.max(0, Math.min(100, progress));
        boolean completed = safeProgress >= 100;

        GrowthPlanProgress update;
        if (exist == null) {
            update = GrowthPlanProgress.builder()
                    .userId(userId)
                    .planId(planId)
                    .progress(safeProgress)
                    .completed(completed ? 1 : 0)
                    .completedAt(completed ? LocalDateTime.now() : null)
                    .updatedAt(LocalDateTime.now())
                    .build();
            growthPlanProgressMapper.insert(update);
            return update;
        }
        update = GrowthPlanProgress.builder()
                .id(exist.getId())
                .progress(safeProgress)
                .completed(completed ? 1 : (exist.getCompleted() != null && exist.getCompleted() == 1 ? 0 : exist.getCompleted()))
                .completedAt(completed ? LocalDateTime.now() : null)
                .updatedAt(LocalDateTime.now())
                .build();
        growthPlanProgressMapper.updateById(update);
        return growthPlanProgressMapper.selectById(exist.getId());
    }

    /**
     * 我的已完成/进行中计划列表（前台"我的成长"）
     */
    public List<GrowthPlanVO> myPlans(Long userId) {
        List<GrowthPlanProgress> progresses = growthPlanProgressMapper.selectList(
                new LambdaQueryWrapper<GrowthPlanProgress>()
                        .eq(GrowthPlanProgress::getUserId, userId)
                        .orderByDesc(GrowthPlanProgress::getUpdatedAt));
        List<Long> planIds = progresses.stream().map(GrowthPlanProgress::getPlanId).collect(Collectors.toList());
        if (planIds.isEmpty()) {
            return List.of();
        }
        Map<Long, GrowthPlanProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(GrowthPlanProgress::getPlanId, Function.identity(), (a, b) -> a));
        List<GrowthPlan> plans = growthPlanMapper.selectBatchIds(planIds);
        return plans.stream().map(p -> toVO(p, progressMap.get(p.getId()))).collect(Collectors.toList());
    }

    // ---------- 内部工具 ----------

    private GrowthPlanProgress findProgress(Long userId, Long planId) {
        if (userId == null) {
            return null;
        }
        return growthPlanProgressMapper.selectOne(
                new LambdaQueryWrapper<GrowthPlanProgress>()
                        .eq(GrowthPlanProgress::getUserId, userId)
                        .eq(GrowthPlanProgress::getPlanId, planId)
                        .last("limit 1"));
    }

    private Map<Long, GrowthPlanProgress> loadMyProgress(Long userId, List<GrowthPlan> plans) {
        if (userId == null || plans == null || plans.isEmpty()) {
            return Map.of();
        }
        List<Long> planIds = plans.stream().map(GrowthPlan::getId).collect(Collectors.toList());
        List<GrowthPlanProgress> progresses = growthPlanProgressMapper.selectList(
                new LambdaQueryWrapper<GrowthPlanProgress>()
                        .eq(GrowthPlanProgress::getUserId, userId)
                        .in(GrowthPlanProgress::getPlanId, planIds));
        return progresses.stream()
                .collect(Collectors.toMap(GrowthPlanProgress::getPlanId, Function.identity(), (a, b) -> a));
    }

    private GrowthPlanVO toVO(GrowthPlan plan, GrowthPlanProgress progress) {
        return GrowthPlanVO.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .summary(plan.getSummary())
                .theme(plan.getTheme())
                .content(plan.getContent())
                .durationDays(plan.getDurationDays())
                .reviewer(plan.getReviewer())
                .reviewedAt(plan.getReviewedAt())
                .status(plan.getStatus())
                .viewCount(plan.getViewCount())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .myProgress(progress == null ? null : progress.getProgress())
                .myCompleted(progress == null ? Boolean.FALSE
                        : (progress.getCompleted() != null && progress.getCompleted() == 1))
                .build();
    }
}
