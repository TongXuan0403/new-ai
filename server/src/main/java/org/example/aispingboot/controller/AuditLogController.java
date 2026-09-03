package org.example.aispingboot.controller;

import org.example.aispingboot.DTO.response.AuditLogResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.AuditLogService;
import org.example.aispingboot.util.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    public Result<Page<AuditLogResponseDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        SecurityUtil.requireAdmin();
        return Result.ok(auditLogService.page(page, pageSize));
    }
}
