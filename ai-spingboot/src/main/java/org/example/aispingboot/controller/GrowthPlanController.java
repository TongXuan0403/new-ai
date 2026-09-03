package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.GrowthPlanCreateDTO;
import org.example.aispingboot.DTO.command.GrowthPlanProgressDTO;
import org.example.aispingboot.DTO.command.GrowthPlanStatusDTO;
import org.example.aispingboot.DTO.response.GrowthPlanPageVO;
import org.example.aispingboot.DTO.response.GrowthPlanVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.GrowthPlanService;
import org.example.aispingboot.util.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 主题化成长计划
 */
@RestController
@RequestMapping("/api/growth-plan")
public class GrowthPlanController {

    @Resource
    private GrowthPlanService growthPlanService;

    /**
     * 计划分页（前台仅已发布；管理员可查全部）
     */
    @GetMapping("/page")
    public Result<GrowthPlanPageVO> page(
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size) {
        boolean isAdmin = UserContext.isAdmin();
        Long userId = UserContext.getCurrentUserId();
        return Result.ok(growthPlanService.page(theme, status, isAdmin, userId, currentPage, size));
    }

    /**
     * 计划详情
     */
    @GetMapping("/{id}")
    public Result<GrowthPlanVO> detail(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.ok(growthPlanService.detail(id, userId, true));
    }

    /**
     * 我的成长（登录后可看自己的进度/完成计划）
     */
    @GetMapping("/my")
    public Result<List<GrowthPlanVO>> myPlans() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.ok(List.of());
        }
        return Result.ok(growthPlanService.myPlans(userId));
    }

    /**
     * 用户更新计划进度（登录）
     */
    @PutMapping("/{id}/progress")
    public Result<Void> updateProgress(@PathVariable Long id,
                                       @Valid @RequestBody GrowthPlanProgressDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("请先登录后再更新进度");
        }
        growthPlanService.updateProgress(userId, id, dto.getProgress());
        return Result.ok();
    }

    // ---------- 管理端 ----------

    @PostMapping
    public Result<GrowthPlanVO> create(@Valid @RequestBody GrowthPlanCreateDTO dto) {
        requireAdmin();
        return Result.ok(growthPlanService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<GrowthPlanVO> update(@PathVariable Long id, @Valid @RequestBody GrowthPlanCreateDTO dto) {
        requireAdmin();
        return Result.ok(growthPlanService.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody GrowthPlanStatusDTO dto) {
        requireAdmin();
        growthPlanService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        growthPlanService.delete(id);
        return Result.ok();
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException("无权限操作，仅管理员可管理成长计划");
        }
    }
}
