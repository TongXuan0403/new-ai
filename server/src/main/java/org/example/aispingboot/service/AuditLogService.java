package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.response.AuditLogResponseDTO;
import org.example.aispingboot.entity.AuditLog;
import org.example.aispingboot.mapper.AuditLogMapper;
import org.example.aispingboot.util.JwtTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class AuditLogService {
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记录审计日志（敏感操作：查看原文/导出/改状态/改资源/处理删除申请等）。
     */
    public void record(Long operatorId, String operatorRole, String action,
                       String targetType, Long targetId, String detail) {
        AuditLog log = AuditLog.builder()
                .operatorId(operatorId)
                .operatorRole(StringUtils.hasText(operatorRole) ? operatorRole : "admin")
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .ip(resolveIp())
                .userAgent(resolveUserAgent())
                .detail(detail)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogMapper.insert(log);
    }

    private String resolveIp() {
        try {
            var request = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (request instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveUserAgent() {
        try {
            var request = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (request instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                String ua = attrs.getRequest().getHeader("User-Agent");
                return ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public Page<AuditLogResponseDTO> page(int page, int pageSize) {
        Page<AuditLog> pager = new Page<>(page, Math.min(pageSize, 100));
        Page<AuditLog> result = auditLogMapper.selectPage(pager,
                new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getCreatedAt));
        Page<AuditLogResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(log -> AuditLogResponseDTO.builder()
                .id(log.getId())
                .operatorId(log.getOperatorId())
                .operatorRole(log.getOperatorRole())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .ip(log.getIp())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build()).collect(Collectors.toList()));
        return response;
    }
}
