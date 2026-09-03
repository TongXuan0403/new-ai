package org.example.aispingboot.controller;

import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.CrisisResourceDTO;
import org.example.aispingboot.DTO.response.CrisisResourceResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.AuditLogService;
import org.example.aispingboot.service.CrisisResourceService;
import org.example.aispingboot.util.SecurityUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 危机资源：公开查询启用资源；/admin/crisis-resources/** 由管理员维护。
 */
@RestController
public class CrisisResourceController {
    private final CrisisResourceService crisisResourceService;
    private final AuditLogService auditLogService;

    public CrisisResourceController(CrisisResourceService crisisResourceService, AuditLogService auditLogService) {
        this.crisisResourceService = crisisResourceService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/crisis-resources")
    public Result<List<CrisisResourceResponseDTO>> listEnabled() {
        return Result.ok(crisisResourceService.listEnabled());
    }

    @GetMapping("/admin/crisis-resources")
    public Result<List<CrisisResourceResponseDTO>> listAll() {
        SecurityUtil.requireAdmin();
        return Result.ok(crisisResourceService.listAll());
    }

    @PostMapping("/admin/crisis-resources")
    public Result<CrisisResourceResponseDTO> create(@Valid @RequestBody CrisisResourceDTO dto) {
        SecurityUtil.requireAdmin();
        CrisisResourceResponseDTO created = crisisResourceService.create(dto);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "CREATE_CRISIS_RESOURCE",
                "crisis_resource", created.getId(), null);
        return Result.ok(created, "资源已添加");
    }

    @PutMapping("/admin/crisis-resources/{id}")
    public Result<CrisisResourceResponseDTO> update(@PathVariable Long id, @Valid @RequestBody CrisisResourceDTO dto) {
        SecurityUtil.requireAdmin();
        return Result.ok(crisisResourceService.update(id, dto), "资源已更新");
    }

    @DeleteMapping("/admin/crisis-resources/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        crisisResourceService.delete(id);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "DELETE_CRISIS_RESOURCE",
                "crisis_resource", id, null);
        return Result.ok(true, "资源已删除");
    }
}
