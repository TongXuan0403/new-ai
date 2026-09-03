package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.aispingboot.DTO.command.CrisisResourceDTO;
import org.example.aispingboot.DTO.response.CrisisResourceResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.CrisisResource;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.CrisisResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CrisisResourceService {
    private final CrisisResourceMapper resourceMapper;

    public CrisisResourceService(CrisisResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    /**
     * 前端可展示的启用资源（登录或公开）。
     */
    public List<CrisisResourceResponseDTO> listEnabled() {
        return resourceMapper.selectList(new LambdaQueryWrapper<CrisisResource>()
                        .eq(CrisisResource::getEnabled, 1)
                        .orderByAsc(CrisisResource::getSortOrder))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 管理端全量资源（含停用）。
     */
    public List<CrisisResourceResponseDTO> listAll() {
        return resourceMapper.selectList(new LambdaQueryWrapper<CrisisResource>()
                        .orderByAsc(CrisisResource::getSortOrder))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CrisisResourceResponseDTO create(CrisisResourceDTO dto) {
        CrisisResource resource = CrisisResource.builder()
                .resourceType(dto.getResourceType())
                .name(dto.getName())
                .phone(dto.getPhone())
                .description(dto.getDescription())
                .region(dto.getRegion())
                .enabled(dto.getEnabled() == null ? 1 : (dto.getEnabled() ? 1 : 0))
                .sortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        resourceMapper.insert(resource);
        return toResponse(resource);
    }

    @Transactional
    public CrisisResourceResponseDTO update(Long id, CrisisResourceDTO dto) {
        CrisisResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        }
        resource.setResourceType(dto.getResourceType());
        resource.setName(dto.getName());
        resource.setPhone(dto.getPhone());
        resource.setDescription(dto.getDescription());
        resource.setRegion(dto.getRegion());
        resource.setEnabled(dto.getEnabled() == null ? resource.getEnabled() : (dto.getEnabled() ? 1 : 0));
        resource.setSortOrder(dto.getSortOrder() == null ? resource.getSortOrder() : dto.getSortOrder());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.updateById(resource);
        return toResponse(resource);
    }

    @Transactional
    public void delete(Long id) {
        resourceMapper.deleteById(id);
    }

    private CrisisResourceResponseDTO toResponse(CrisisResource resource) {
        return CrisisResourceResponseDTO.builder()
                .id(resource.getId())
                .resourceType(resource.getResourceType())
                .name(resource.getName())
                .phone(resource.getPhone())
                .description(resource.getDescription())
                .region(resource.getRegion())
                .enabled(Integer.valueOf(1).equals(resource.getEnabled()))
                .sortOrder(resource.getSortOrder())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
