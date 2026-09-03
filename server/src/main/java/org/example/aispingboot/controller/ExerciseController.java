package org.example.aispingboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.ExerciseCompletionDTO;
import org.example.aispingboot.DTO.command.ExerciseCreateDTO;
import org.example.aispingboot.DTO.response.ExerciseCompletionResponseDTO;
import org.example.aispingboot.DTO.response.ExerciseResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.entity.ExerciseCompletion;
import org.example.aispingboot.service.AuditLogService;
import org.example.aispingboot.service.ExerciseService;
import org.example.aispingboot.util.SecurityUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自助练习库：学生端浏览/完成；管理端维护（/admin/exercises/** 需管理员）。
 */
@RestController
public class ExerciseController {
    private final ExerciseService exerciseService;
    private final AuditLogService auditLogService;

    public ExerciseController(ExerciseService exerciseService, AuditLogService auditLogService) {
        this.exerciseService = exerciseService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/exercises")
    public Result<Page<ExerciseResponseDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.ok(exerciseService.listPublished(keyword, tag, userId, page, pageSize));
    }

    @GetMapping("/exercises/{id}")
    public Result<ExerciseResponseDTO> detail(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.ok(exerciseService.detail(id, true, userId));
    }

    @PostMapping("/exercises/{id}/complete")
    public Result<ExerciseCompletionResponseDTO> complete(@PathVariable Long id,
                                                          @RequestBody(required = false) ExerciseCompletionDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        String moodAfter = dto == null ? null : dto.getMoodAfter();
        ExerciseCompletion completion = exerciseService.complete(userId, id, moodAfter);
        ExerciseCompletionResponseDTO response = ExerciseCompletionResponseDTO.builder()
                .id(completion.getId())
                .exerciseId(completion.getExerciseId())
                .moodAfter(completion.getMoodAfter())
                .completedAt(completion.getCompletedAt())
                .build();
        return Result.ok(response, "练习已完成，已记录");
    }

    @GetMapping("/exercises/my/completions")
    public Result<List<ExerciseCompletionResponseDTO>> myCompletions() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.ok(exerciseService.myCompletions(userId));
    }

    @GetMapping("/admin/exercises/page")
    public Result<Page<ExerciseResponseDTO>> adminPage(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        SecurityUtil.requireAdmin();
        return Result.ok(exerciseService.adminPage(status, keyword, page, pageSize));
    }

    @PostMapping("/admin/exercises")
    public Result<ExerciseResponseDTO> create(@Valid @RequestBody ExerciseCreateDTO dto) {
        SecurityUtil.requireAdmin();
        ExerciseResponseDTO created = exerciseService.create(dto);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "CREATE_EXERCISE",
                "exercise", created.getId(), null);
        return Result.ok(created, "练习已创建");
    }

    @PutMapping("/admin/exercises/{id}")
    public Result<ExerciseResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ExerciseCreateDTO dto) {
        SecurityUtil.requireAdmin();
        return Result.ok(exerciseService.update(id, dto), "练习已更新");
    }

    @DeleteMapping("/admin/exercises/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        exerciseService.delete(id);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "DELETE_EXERCISE",
                "exercise", id, null);
        return Result.ok(true, "练习已删除");
    }

    @PutMapping("/admin/exercises/{id}/status")
    public Result<ExerciseResponseDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        SecurityUtil.requireAdmin();
        ExerciseResponseDTO updated = exerciseService.updateStatus(id, status);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin",
                "AUDIT_EXERCISE_" + status, "exercise", id, null);
        return Result.ok(updated, "练习状态已更新");
    }
}
