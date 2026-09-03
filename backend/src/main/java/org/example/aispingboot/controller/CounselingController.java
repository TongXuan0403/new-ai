package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.AppointmentCreateDTO;
import org.example.aispingboot.DTO.command.AppointmentStatusDTO;
import org.example.aispingboot.DTO.command.CounselingResourceCreateDTO;
import org.example.aispingboot.DTO.response.AppointmentPageVO;
import org.example.aispingboot.DTO.response.AppointmentVO;
import org.example.aispingboot.DTO.response.CounselingResourceVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.CounselingService;
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
 * 心理中心预约/转介
 */
@RestController
@RequestMapping("/api/counseling")
public class CounselingController {

    @Resource
    private CounselingService counselingService;

    // ---------- 资源（前台可浏览启用资源） ----------

    @GetMapping("/resources")
    public Result<List<CounselingResourceVO>> listResources() {
        return Result.ok(counselingService.listEnabledResources());
    }

    @GetMapping("/resources/admin/list")
    public Result<List<CounselingResourceVO>> adminResourceList(@RequestParam(required = false) String keyword) {
        requireAdmin();
        return Result.ok(counselingService.adminResourceList(keyword));
    }

    @PostMapping("/resources")
    public Result<CounselingResourceVO> createResource(@Valid @RequestBody CounselingResourceCreateDTO dto) {
        requireAdmin();
        return Result.ok(counselingService.createResource(dto));
    }

    @PutMapping("/resources/{id}")
    public Result<CounselingResourceVO> updateResource(@PathVariable Long id,
                                                       @Valid @RequestBody CounselingResourceCreateDTO dto) {
        requireAdmin();
        return Result.ok(counselingService.updateResource(id, dto));
    }

    @DeleteMapping("/resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        requireAdmin();
        counselingService.deleteResource(id);
        return Result.ok();
    }

    // ---------- 预约申请（需登录） ----------

    @PostMapping("/appointments")
    public Result<AppointmentVO> createAppointment(@Valid @RequestBody AppointmentCreateDTO dto) {
        Long userId = requireLogin();
        return Result.ok(counselingService.createAppointment(userId, UserContext.getCurrentUsername(), dto));
    }

    @GetMapping("/appointments/my")
    public Result<List<AppointmentVO>> myAppointments() {
        Long userId = requireLogin();
        return Result.ok(counselingService.myAppointments(userId));
    }

    @DeleteMapping("/appointments/{id}")
    public Result<Void> cancelAppointment(@PathVariable Long id) {
        Long userId = requireLogin();
        counselingService.cancelAppointment(userId, id);
        return Result.ok();
    }

    // ---------- 管理端 ----------

    @GetMapping("/appointments/admin/page")
    public Result<AppointmentPageVO> adminPage(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        requireAdmin();
        return Result.ok(counselingService.adminPage(currentPage, size, keyword, status));
    }

    @PutMapping("/appointments/{id}/status")
    public Result<AppointmentVO> updateAppointmentStatus(@PathVariable Long id,
                                                         @Valid @RequestBody AppointmentStatusDTO dto) {
        requireAdmin();
        return Result.ok(counselingService.updateStatus(id, dto.getStatus(), dto.getRemark()));
    }

    private Long requireLogin() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        return userId;
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException("无权限操作，仅管理员可管理心理资源与预约");
        }
    }
}
