package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispingboot.DTO.command.EmotionDiaryUpdateDTO;
import org.example.aispingboot.DTO.response.DiaryTrendResponseDTO;
import org.example.aispingboot.DTO.response.EmotionDiaryResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmotionDiaryService {
    private final EmotionDiaryMapper diaryMapper;

    public EmotionDiaryService(EmotionDiaryMapper diaryMapper) {
        this.diaryMapper = diaryMapper;
    }

    @Transactional
    public EmotionDiaryResponseDTO create(Long userId, EmotionDiaryCreateDTO dto) {
        LocalDate today = LocalDate.now();
        // 一天一条：当天已有则更新，避免重复记录
        EmotionDiary existing = findByUserAndDate(userId, today);
        EmotionDiary diary;
        if (existing != null) {
            existing.setEmotionStatus(dto.getEmotionStatus());
            existing.setScore(dto.getScore());
            existing.setEvent(dto.getEvent());
            existing.setSleepStatus(dto.getSleepStatus());
            existing.setEnergyStatus(dto.getEnergyStatus());
            existing.setUpdatedAt(LocalDateTime.now());
            diaryMapper.updateById(existing);
            diary = existing;
        } else {
            diary = EmotionDiary.builder()
                    .userId(userId)
                    .emotionStatus(dto.getEmotionStatus())
                    .score(dto.getScore())
                    .event(dto.getEvent())
                    .sleepStatus(dto.getSleepStatus())
                    .energyStatus(dto.getEnergyStatus())
                    .logDate(today)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            diaryMapper.insert(diary);
        }
        return toResponse(diary);
    }

    public Page<EmotionDiaryResponseDTO> page(Long userId, int page, int pageSize) {
        Page<EmotionDiary> pager = new Page<>(page, Math.min(pageSize, 100));
        Page<EmotionDiary> result = diaryMapper.selectPage(pager, new LambdaQueryWrapper<EmotionDiary>()
                .eq(EmotionDiary::getUserId, userId)
                .orderByDesc(EmotionDiary::getLogDate));
        Page<EmotionDiaryResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return response;
    }

    public EmotionDiaryResponseDTO getById(Long userId, Long id) {
        EmotionDiary diary = getOwned(userId, id);
        return toResponse(diary);
    }

    @Transactional
    public EmotionDiaryResponseDTO update(Long userId, Long id, EmotionDiaryUpdateDTO dto) {
        EmotionDiary diary = getOwned(userId, id);
        diary.setEmotionStatus(dto.getEmotionStatus());
        diary.setScore(dto.getScore());
        diary.setEvent(dto.getEvent());
        diary.setSleepStatus(dto.getSleepStatus());
        diary.setEnergyStatus(dto.getEnergyStatus());
        diary.setUpdatedAt(LocalDateTime.now());
        diaryMapper.updateById(diary);
        return toResponse(diary);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        EmotionDiary diary = getOwned(userId, id);
        diaryMapper.deleteById(diary.getId());
    }

    public DiaryTrendResponseDTO trend(Long userId, int days) {
        if (days != 7 && days != 30) {
            days = 7;
        }
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<EmotionDiary> list = diaryMapper.selectList(new LambdaQueryWrapper<EmotionDiary>()
                .eq(EmotionDiary::getUserId, userId)
                .ge(EmotionDiary::getLogDate, start)
                .orderByAsc(EmotionDiary::getLogDate));
        List<DiaryTrendResponseDTO.Point> points = list.stream()
                .map(d -> DiaryTrendResponseDTO.Point.builder()
                        .date(d.getLogDate())
                        .score(d.getScore())
                        .emotionStatus(d.getEmotionStatus())
                        .build())
                .collect(Collectors.toList());
        return DiaryTrendResponseDTO.builder()
                .days(days)
                .recordCount((long) list.size())
                .points(points)
                .build();
    }

    private EmotionDiary getOwned(Long userId, Long id) {
        EmotionDiary diary = diaryMapper.selectById(id);
        if (diary == null || !diary.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.RESOURCE_FORBIDDEN, "日记不存在或无权访问");
        }
        return diary;
    }

    private EmotionDiary findByUserAndDate(Long userId, LocalDate date) {
        return diaryMapper.selectOne(new LambdaQueryWrapper<EmotionDiary>()
                .eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getLogDate, date)
                .last("LIMIT 1"));
    }

    private EmotionDiaryResponseDTO toResponse(EmotionDiary diary) {
        return EmotionDiaryResponseDTO.builder()
                .id(diary.getId())
                .emotionStatus(diary.getEmotionStatus())
                .score(diary.getScore())
                .event(diary.getEvent())
                .sleepStatus(diary.getSleepStatus())
                .energyStatus(diary.getEnergyStatus())
                .logDate(diary.getLogDate())
                .createdAt(diary.getCreatedAt())
                .build();
    }
}
