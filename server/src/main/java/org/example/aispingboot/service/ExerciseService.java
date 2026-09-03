package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.command.ExerciseCompletionDTO;
import org.example.aispingboot.DTO.command.ExerciseCreateDTO;
import org.example.aispingboot.DTO.response.ExerciseCompletionResponseDTO;
import org.example.aispingboot.DTO.response.ExerciseResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.Exercise;
import org.example.aispingboot.entity.ExerciseCompletion;
import org.example.aispingboot.entity.KnowledgeCategory;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ExerciseCompletionMapper;
import org.example.aispingboot.mapper.ExerciseMapper;
import org.example.aispingboot.mapper.KnowledgeCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自助练习库：学生端浏览/完成练习，管理端维护练习内容。
 */
@Service
public class ExerciseService {
    private final ExerciseMapper exerciseMapper;
    private final ExerciseCompletionMapper completionMapper;
    private final KnowledgeCategoryMapper categoryMapper;

    public ExerciseService(ExerciseMapper exerciseMapper, ExerciseCompletionMapper completionMapper,
                           KnowledgeCategoryMapper categoryMapper) {
        this.exerciseMapper = exerciseMapper;
        this.completionMapper = completionMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 学生端：仅返回已发布练习，并标注当前用户是否已完成。
     */
    public Page<ExerciseResponseDTO> listPublished(String keyword, String tag, Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Exercise> wrapper = new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getStatus, "PUBLISHED")
                .orderByAsc(Exercise::getSortOrder)
                .orderByDesc(Exercise::getPublishedAt);
        if (StringUtils.hasText(tag)) {
            wrapper.and(w -> w.like(Exercise::getTags, "," + tag + ",")
                    .or().like(Exercise::getTags, tag + ",")
                    .or().like(Exercise::getTags, "," + tag)
                    .or().eq(Exercise::getTags, tag));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Exercise::getTitle, keyword)
                    .or().like(Exercise::getSummary, keyword)
                    .or().like(Exercise::getContent, keyword));
        }
        Page<Exercise> pager = new Page<>(page, Math.min(pageSize, 50));
        Page<Exercise> result = exerciseMapper.selectPage(pager, wrapper);
        Set<Long> completedIds = userId == null ? Set.of() : completedExerciseIds(userId);
        Page<ExerciseResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream()
                .map(e -> toResponse(e, completedIds.contains(e.getId()), null))
                .collect(Collectors.toList()));
        return response;
    }

    public ExerciseResponseDTO detail(Long id, boolean publishedOnly, Long userId) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "练习不存在");
        }
        if (publishedOnly && !"PUBLISHED".equals(exercise.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "练习不存在或未发布");
        }
        boolean completed = userId != null && isCompleted(userId, id);
        LocalDateTime completedAt = null;
        if (completed) {
            completedAt = completionMapper.selectOne(new LambdaQueryWrapper<ExerciseCompletion>()
                    .eq(ExerciseCompletion::getUserId, userId)
                    .eq(ExerciseCompletion::getExerciseId, id)).getCompletedAt();
        }
        return toResponse(exercise, completed, completedAt);
    }

    /**
     * 管理端：全量练习（含草稿/下线）。
     */
    public Page<ExerciseResponseDTO> adminPage(String status, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<Exercise> wrapper = new LambdaQueryWrapper<Exercise>()
                .orderByAsc(Exercise::getSortOrder)
                .orderByDesc(Exercise::getUpdatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Exercise::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Exercise::getTitle, keyword);
        }
        Page<Exercise> pager = new Page<>(page, Math.min(pageSize, 100));
        Page<Exercise> result = exerciseMapper.selectPage(pager, wrapper);
        Page<ExerciseResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream()
                .map(e -> toResponse(e, false, null))
                .collect(Collectors.toList()));
        return response;
    }

    @Transactional
    public ExerciseResponseDTO create(ExerciseCreateDTO dto) {
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "DRAFT";
        Exercise exercise = Exercise.builder()
                .categoryId(dto.getCategoryId() == null ? 4L : dto.getCategoryId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .minutes(dto.getMinutes() == null ? 5 : dto.getMinutes())
                .tags(dto.getTags())
                .sortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .status(status)
                .publishedAt("PUBLISHED".equals(status) ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        exerciseMapper.insert(exercise);
        return toResponse(exercise, false, null);
    }

    @Transactional
    public ExerciseResponseDTO update(Long id, ExerciseCreateDTO dto) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "练习不存在");
        }
        exercise.setCategoryId(dto.getCategoryId() == null ? exercise.getCategoryId() : dto.getCategoryId());
        exercise.setTitle(dto.getTitle());
        exercise.setSummary(dto.getSummary());
        exercise.setContent(dto.getContent());
        exercise.setMinutes(dto.getMinutes() == null ? exercise.getMinutes() : dto.getMinutes());
        exercise.setTags(dto.getTags());
        exercise.setSortOrder(dto.getSortOrder() == null ? exercise.getSortOrder() : dto.getSortOrder());
        exercise.setUpdatedAt(LocalDateTime.now());
        exerciseMapper.updateById(exercise);
        return toResponse(exercise, false, null);
    }

    @Transactional
    public ExerciseResponseDTO updateStatus(Long id, String status) {
        Exercise exercise = exerciseMapper.selectById(id);
        if (exercise == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "练习不存在");
        }
        exercise.setStatus(status);
        if ("PUBLISHED".equals(status) && exercise.getPublishedAt() == null) {
            exercise.setPublishedAt(LocalDateTime.now());
        }
        exercise.setUpdatedAt(LocalDateTime.now());
        exerciseMapper.updateById(exercise);
        return toResponse(exercise, false, null);
    }

    @Transactional
    public void delete(Long id) {
        exerciseMapper.deleteById(id);
        completionMapper.delete(new LambdaQueryWrapper<ExerciseCompletion>()
                .eq(ExerciseCompletion::getExerciseId, id));
    }

    /**
     * 标记练习完成（每用户每练习仅一次，重复提交仅返回既有记录）。
     */
    @Transactional
    public ExerciseCompletion complete(Long userId, Long exerciseId, String moodAfter) {
        detail(exerciseId, true, userId);
        ExerciseCompletion existing = completionMapper.selectOne(new LambdaQueryWrapper<ExerciseCompletion>()
                .eq(ExerciseCompletion::getUserId, userId)
                .eq(ExerciseCompletion::getExerciseId, exerciseId));
        if (existing != null) {
            if (StringUtils.hasText(moodAfter) && !moodAfter.equals(existing.getMoodAfter())) {
                existing.setMoodAfter(moodAfter);
                completionMapper.updateById(existing);
            }
            return existing;
        }
        ExerciseCompletion completion = ExerciseCompletion.builder()
                .userId(userId)
                .exerciseId(exerciseId)
                .moodAfter(moodAfter)
                .completedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        completionMapper.insert(completion);
        return completion;
    }

    /**
     * 我的完成记录（按完成时间倒序）。
     */
    public List<ExerciseCompletionResponseDTO> myCompletions(Long userId) {
        List<ExerciseCompletion> completions = completionMapper.selectList(new LambdaQueryWrapper<ExerciseCompletion>()
                .eq(ExerciseCompletion::getUserId, userId)
                .orderByDesc(ExerciseCompletion::getCompletedAt));
        if (completions.isEmpty()) {
            return List.of();
        }
        List<Long> exerciseIds = completions.stream()
                .map(ExerciseCompletion::getExerciseId).distinct().collect(Collectors.toList());
        Map<Long, Exercise> exerciseMap = exerciseMapper.selectBatchIds(exerciseIds).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e));
        return completions.stream()
                .filter(c -> exerciseMap.containsKey(c.getExerciseId()))
                .map(c -> {
                    Exercise exercise = exerciseMap.get(c.getExerciseId());
                    return ExerciseCompletionResponseDTO.builder()
                            .id(c.getId())
                            .exerciseId(c.getExerciseId())
                            .exerciseTitle(exercise.getTitle())
                            .exerciseSummary(exercise.getSummary())
                            .minutes(exercise.getMinutes())
                            .tags(exercise.getTags())
                            .moodAfter(c.getMoodAfter())
                            .completedAt(c.getCompletedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public long countCompletions() {
        return completionMapper.selectCount(null);
    }

    public long countPublished() {
        return exerciseMapper.selectCount(new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getStatus, "PUBLISHED"));
    }

    private Set<Long> completedExerciseIds(Long userId) {
        return completionMapper.selectList(new LambdaQueryWrapper<ExerciseCompletion>()
                        .eq(ExerciseCompletion::getUserId, userId))
                .stream().map(ExerciseCompletion::getExerciseId).collect(Collectors.toSet());
    }

    private boolean isCompleted(Long userId, Long exerciseId) {
        return completionMapper.selectCount(new LambdaQueryWrapper<ExerciseCompletion>()
                .eq(ExerciseCompletion::getUserId, userId)
                .eq(ExerciseCompletion::getExerciseId, exerciseId)) > 0;
    }

    private ExerciseResponseDTO toResponse(Exercise exercise, boolean completed, LocalDateTime completedAt) {
        String categoryName = "";
        if (exercise.getCategoryId() != null) {
            KnowledgeCategory category = categoryMapper.selectById(exercise.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        return ExerciseResponseDTO.builder()
                .id(exercise.getId())
                .categoryId(exercise.getCategoryId())
                .categoryName(categoryName)
                .title(exercise.getTitle())
                .summary(exercise.getSummary())
                .content(exercise.getContent())
                .minutes(exercise.getMinutes())
                .tags(exercise.getTags())
                .status(exercise.getStatus())
                .sortOrder(exercise.getSortOrder())
                .publishedAt(exercise.getPublishedAt())
                .createdAt(exercise.getCreatedAt())
                .completed(completed)
                .completedAt(completedAt)
                .build();
    }
}
