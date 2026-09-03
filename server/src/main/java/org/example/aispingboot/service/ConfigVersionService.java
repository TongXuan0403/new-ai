package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.command.SystemConfigVersionDTO;
import org.example.aispingboot.DTO.response.SystemConfigVersionResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.SystemConfigVersion;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.SystemConfigVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 提示词 / 模型 / 风险规则 后台版本化。
 * 每类配置同时仅一条 ACTIVE；运行期通过 getActiveVersion 读取生效版本（带 5 秒缓存），
 * 写操作后立即失效对应类型的缓存。
 */
@Service
public class ConfigVersionService {

    public static final String TYPE_PROMPT = "PROMPT";
    public static final String TYPE_MODEL = "MODEL";
    public static final String TYPE_RISK_RULE = "RISK_RULE";

    private static final long CACHE_TTL_MS = 5000L;

    private final SystemConfigVersionMapper versionMapper;
    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

    public ConfigVersionService(SystemConfigVersionMapper versionMapper) {
        this.versionMapper = versionMapper;
    }

    // ------------------------------------------------------------------
    // 管理端
    // ------------------------------------------------------------------

    public Page<SystemConfigVersionResponseDTO> page(String configType, int page, int pageSize) {
        LambdaQueryWrapper<SystemConfigVersion> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configType)) {
            wrapper.eq(SystemConfigVersion::getConfigType, configType);
        }
        wrapper.orderByAsc(SystemConfigVersion::getConfigType)
                .orderByDesc(SystemConfigVersion::getUpdatedAt);
        Page<SystemConfigVersion> pager = new Page<>(page, Math.min(pageSize, 100));
        Page<SystemConfigVersion> result = versionMapper.selectPage(pager, wrapper);
        Page<SystemConfigVersionResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return response;
    }

    public SystemConfigVersionResponseDTO getById(Long id) {
        SystemConfigVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置版本不存在");
        }
        return toResponse(version);
    }

    @Transactional
    public SystemConfigVersionResponseDTO create(SystemConfigVersionDTO dto, Long operatorId) {
        validateType(dto.getConfigType());
        long exists = versionMapper.selectCount(new LambdaQueryWrapper<SystemConfigVersion>()
                .eq(SystemConfigVersion::getConfigType, dto.getConfigType())
                .eq(SystemConfigVersion::getVersion, dto.getVersion()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                    "该类型下版本号已存在：" + dto.getVersion());
        }
        SystemConfigVersion version = SystemConfigVersion.builder()
                .configType(dto.getConfigType())
                .name(dto.getName())
                .version(dto.getVersion())
                .content(dto.getContent())
                .status("DRAFT")
                .remark(dto.getRemark())
                .createdBy(operatorId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        versionMapper.insert(version);
        evict(dto.getConfigType());
        return toResponse(version);
    }

    @Transactional
    public SystemConfigVersionResponseDTO update(Long id, SystemConfigVersionDTO dto) {
        SystemConfigVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置版本不存在");
        }
        if (!"DRAFT".equals(version.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "仅草稿状态可编辑");
        }
        if (StringUtils.hasText(dto.getVersion()) && !dto.getVersion().equals(version.getVersion())) {
            long exists = versionMapper.selectCount(new LambdaQueryWrapper<SystemConfigVersion>()
                    .eq(SystemConfigVersion::getConfigType, version.getConfigType())
                    .eq(SystemConfigVersion::getVersion, dto.getVersion()));
            if (exists > 0) {
                throw new BusinessException(ResultCode.PARAM_INVALID,
                        "该类型下版本号已存在：" + dto.getVersion());
            }
        }
        version.setName(dto.getName());
        version.setVersion(dto.getVersion());
        version.setContent(dto.getContent());
        version.setRemark(dto.getRemark());
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        evict(version.getConfigType());
        return toResponse(version);
    }

    /**
     * 生效：同类型其他版本置为 DISABLED，本版本置为 ACTIVE。
     */
    @Transactional
    public SystemConfigVersionResponseDTO activate(Long id) {
        SystemConfigVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置版本不存在");
        }
        List<SystemConfigVersion> sameType = versionMapper.selectList(new LambdaQueryWrapper<SystemConfigVersion>()
                .eq(SystemConfigVersion::getConfigType, version.getConfigType())
                .eq(SystemConfigVersion::getStatus, "ACTIVE"));
        for (SystemConfigVersion other : sameType) {
            if (!other.getId().equals(id)) {
                other.setStatus("DISABLED");
                other.setUpdatedAt(LocalDateTime.now());
                versionMapper.updateById(other);
            }
        }
        version.setStatus("ACTIVE");
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        evict(version.getConfigType());
        return toResponse(version);
    }

    @Transactional
    public SystemConfigVersionResponseDTO disable(Long id) {
        SystemConfigVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置版本不存在");
        }
        if ("ACTIVE".equals(version.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "生效中的版本不能直接停用，请先让其他版本生效");
        }
        version.setStatus("DISABLED");
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        evict(version.getConfigType());
        return toResponse(version);
    }

    @Transactional
    public void delete(Long id) {
        SystemConfigVersion version = versionMapper.selectById(id);
        if (version == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置版本不存在");
        }
        if ("ACTIVE".equals(version.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "生效中的版本不能删除，请先让其他版本生效");
        }
        versionMapper.deleteById(id);
        evict(version.getConfigType());
    }

    // ------------------------------------------------------------------
    // 运行期读取（带短缓存）
    // ------------------------------------------------------------------

    /**
     * 获取某类型的生效版本；无生效版本时返回 null（调用方回退内置默认）。
     */
    public SystemConfigVersion getActiveVersion(String configType) {
        CachedEntry entry = cache.get(configType);
        long now = System.currentTimeMillis();
        if (entry != null && now - entry.loadedAt < CACHE_TTL_MS) {
            return entry.version;
        }
        List<SystemConfigVersion> list = versionMapper.selectList(new LambdaQueryWrapper<SystemConfigVersion>()
                .eq(SystemConfigVersion::getConfigType, configType)
                .eq(SystemConfigVersion::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        SystemConfigVersion version = list.isEmpty() ? null : list.get(0);
        cache.put(configType, new CachedEntry(version, now));
        return version;
    }

    /**
     * 获取生效版本内容；无生效版本时返回 fallback。
     */
    public String getActiveContent(String configType, String fallback) {
        SystemConfigVersion version = getActiveVersion(configType);
        if (version == null || !StringUtils.hasText(version.getContent())) {
            return fallback;
        }
        return version.getContent();
    }

    /**
     * 获取生效版本号（用于消息/风险事件可追溯）；无生效版本时返回 fallback。
     */
    public String getActiveVersionLabel(String configType, String fallback) {
        SystemConfigVersion version = getActiveVersion(configType);
        return version == null ? fallback : version.getVersion();
    }

    private void evict(String configType) {
        cache.remove(configType);
    }

    private void validateType(String configType) {
        if (!TYPE_PROMPT.equals(configType) && !TYPE_MODEL.equals(configType)
                && !TYPE_RISK_RULE.equals(configType)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "配置类型仅支持 PROMPT/MODEL/RISK_RULE");
        }
    }

    private SystemConfigVersionResponseDTO toResponse(SystemConfigVersion version) {
        return SystemConfigVersionResponseDTO.builder()
                .id(version.getId())
                .configType(version.getConfigType())
                .name(version.getName())
                .version(version.getVersion())
                .content(version.getContent())
                .status(version.getStatus())
                .remark(version.getRemark())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .build();
    }

    private static final class CachedEntry {
        final SystemConfigVersion version;
        final long loadedAt;

        CachedEntry(SystemConfigVersion version, long loadedAt) {
            this.version = version;
            this.loadedAt = loadedAt;
        }
    }
}
