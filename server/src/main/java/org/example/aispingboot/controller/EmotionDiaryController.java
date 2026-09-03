package org.example.aispingboot.controller;

import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispingboot.DTO.command.EmotionDiaryUpdateDTO;
import org.example.aispingboot.DTO.response.DiaryTrendResponseDTO;
import org.example.aispingboot.DTO.response.EmotionDiaryResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.ConsentService;
import org.example.aispingboot.service.EmotionDiaryService;
import org.example.aispingboot.util.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emotion-diary")
public class EmotionDiaryController {
    private final EmotionDiaryService diaryService;
    private final ConsentService consentService;

    public EmotionDiaryController(EmotionDiaryService diaryService, ConsentService consentService) {
        this.diaryService = diaryService;
        this.consentService = consentService;
    }

    @PostMapping
    public Result<EmotionDiaryResponseDTO> create(@Valid @RequestBody EmotionDiaryCreateDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        consentService.ensureConsented(userId);
        return Result.ok(diaryService.create(userId, dto), "日记已保存");
    }

    @GetMapping("/page")
    public Result<Page<EmotionDiaryResponseDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(diaryService.page(SecurityUtil.getCurrentUserId(), page, pageSize));
    }

    @GetMapping("/trend")
    public Result<DiaryTrendResponseDTO> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(diaryService.trend(SecurityUtil.getCurrentUserId(), days));
    }

    @GetMapping("/{id}")
    public Result<EmotionDiaryResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(diaryService.getById(SecurityUtil.getCurrentUserId(), id));
    }

    @PutMapping("/{id}")
    public Result<EmotionDiaryResponseDTO> update(@PathVariable Long id, @Valid @RequestBody EmotionDiaryUpdateDTO dto) {
        return Result.ok(diaryService.update(SecurityUtil.getCurrentUserId(), id, dto), "日记已更新");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        diaryService.delete(SecurityUtil.getCurrentUserId(), id);
        return Result.ok(true, "日记已删除");
    }
}
