package org.example.aispingboot.controller;

import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.RiskEventStatusDTO;
import org.example.aispingboot.DTO.response.RiskEventResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.AuditLogService;
import org.example.aispingboot.service.RiskEventService;
import org.example.aispingboot.util.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风险事件中心（仅管理员）。默认返回脱敏摘要；查看需复核。
 */
@RestController
@RequestMapping("/admin/risk-events")
public class RiskEventController {
    private final RiskEventService riskEventService;
    private final AuditLogService auditLogService;

    public RiskEventController(RiskEventService riskEventService, AuditLogService auditLogService) {
        this.riskEventService = riskEventService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    public Result<Page<RiskEventResponseDTO>> page(
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        SecurityUtil.requireAdmin();
        return Result.ok(riskEventService.adminPage(level, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<RiskEventResponseDTO> detail(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        RiskEventResponseDTO event = riskEventService.getById(id);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "VIEW_RISK_EVENT",
                "risk_event", id, null);
        return Result.ok(event);
    }

    @PutMapping("/{id}/status")
    public Result<RiskEventResponseDTO> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody RiskEventStatusDTO dto) {
        SecurityUtil.requireAdmin();
        RiskEventResponseDTO updated = riskEventService.updateStatus(id, dto.getStatus());
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin",
                "UPDATE_RISK_EVENT_STATUS", "risk_event", id, dto.getStatus());
        return Result.ok(updated, "处理状态已更新");
    }
}
