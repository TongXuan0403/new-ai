package org.example.aispingboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.SystemConfigVersionDTO;
import org.example.aispingboot.DTO.response.SystemConfigVersionResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.AuditLogService;
import org.example.aispingboot.service.ConfigVersionService;
import org.example.aispingboot.util.SecurityUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置版本管理：提示词 / 模型 / 风险规则（仅管理员，写操作均写审计日志）。
 */
@RestController
public class SystemConfigController {
    private final ConfigVersionService configVersionService;
    private final AuditLogService auditLogService;

    public SystemConfigController(ConfigVersionService configVersionService, AuditLogService auditLogService) {
        this.configVersionService = configVersionService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/admin/system-config/versions")
    public Result<Page<SystemConfigVersionResponseDTO>> page(
            @RequestParam(required = false) String configType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        SecurityUtil.requireAdmin();
        return Result.ok(configVersionService.page(configType, page, pageSize));
    }

    @GetMapping("/admin/system-config/versions/{id}")
    public Result<SystemConfigVersionResponseDTO> detail(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        return Result.ok(configVersionService.getById(id));
    }

    @PostMapping("/admin/system-config/versions")
    public Result<SystemConfigVersionResponseDTO> create(@Valid @RequestBody SystemConfigVersionDTO dto) {
        SecurityUtil.requireAdmin();
        SystemConfigVersionResponseDTO created = configVersionService.create(dto, SecurityUtil.getCurrentUserId());
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "CREATE_CONFIG_VERSION",
                "system_config_version", created.getId(), created.getConfigType() + ":" + created.getVersion());
        return Result.ok(created, "配置版本已创建（草稿）");
    }

    @PutMapping("/admin/system-config/versions/{id}")
    public Result<SystemConfigVersionResponseDTO> update(@PathVariable Long id,
                                                         @Valid @RequestBody SystemConfigVersionDTO dto) {
        SecurityUtil.requireAdmin();
        SystemConfigVersionResponseDTO updated = configVersionService.update(id, dto);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "UPDATE_CONFIG_VERSION",
                "system_config_version", id, updated.getConfigType() + ":" + updated.getVersion());
        return Result.ok(updated, "配置版本已更新");
    }

    @PostMapping("/admin/system-config/versions/{id}/activate")
    public Result<SystemConfigVersionResponseDTO> activate(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        SystemConfigVersionResponseDTO updated = configVersionService.activate(id);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "ACTIVATE_CONFIG_VERSION",
                "system_config_version", id, updated.getConfigType() + ":" + updated.getVersion());
        return Result.ok(updated, "配置版本已生效");
    }

    @PostMapping("/admin/system-config/versions/{id}/disable")
    public Result<SystemConfigVersionResponseDTO> disable(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        SystemConfigVersionResponseDTO updated = configVersionService.disable(id);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "DISABLE_CONFIG_VERSION",
                "system_config_version", id, updated.getConfigType() + ":" + updated.getVersion());
        return Result.ok(updated, "配置版本已停用");
    }

    @DeleteMapping("/admin/system-config/versions/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        configVersionService.delete(id);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "DELETE_CONFIG_VERSION",
                "system_config_version", id, null);
        return Result.ok(true, "配置版本已删除");
    }
}
