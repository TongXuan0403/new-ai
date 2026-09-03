package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.command.AppointmentCreateDTO;
import org.example.aispingboot.DTO.command.CounselingResourceCreateDTO;
import org.example.aispingboot.DTO.response.AppointmentPageVO;
import org.example.aispingboot.DTO.response.AppointmentVO;
import org.example.aispingboot.DTO.response.CounselingResourceVO;
import org.example.aispingboot.entity.AppointmentRequest;
import org.example.aispingboot.entity.CounselingResource;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.AppointmentRequestMapper;
import org.example.aispingboot.mapper.CounselingResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 心理中心预约/转介服务
 */
@Service
public class CounselingService {

    @Resource
    private CounselingResourceMapper counselingResourceMapper;

    @Resource
    private AppointmentRequestMapper appointmentRequestMapper;

    // ---------- 心理中心资源 ----------

    /**
     * 前台资源列表（仅启用）
     */
    public List<CounselingResourceVO> listEnabledResources() {
        List<CounselingResource> resources = counselingResourceMapper.selectList(
                new LambdaQueryWrapper<CounselingResource>()
                        .eq(CounselingResource::getEnabled, 1)
                        .orderByAsc(CounselingResource::getSortNo)
                        .orderByAsc(CounselingResource::getId));
        return resources.stream().map(this::toResourceVO).collect(Collectors.toList());
    }

    /**
     * 管理端资源分页（含停用）
     */
    public List<CounselingResourceVO> adminResourceList(String keyword) {
        LambdaQueryWrapper<CounselingResource> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CounselingResource::getName, keyword)
                    .or().like(CounselingResource::getDescription, keyword));
        }
        wrapper.orderByAsc(CounselingResource::getSortNo).orderByAsc(CounselingResource::getId);
        return counselingResourceMapper.selectList(wrapper).stream()
                .map(this::toResourceVO).collect(Collectors.toList());
    }

    public CounselingResourceVO createResource(CounselingResourceCreateDTO dto) {
        CounselingResource resource = CounselingResource.builder()
                .name(dto.getName())
                .resourceType(StringUtils.hasText(dto.getResourceType()) ? dto.getResourceType() : "SCHOOL")
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .workTime(dto.getWorkTime())
                .description(dto.getDescription())
                .sortNo(dto.getSortNo() == null ? 0 : dto.getSortNo())
                .enabled(dto.getEnabled() == null ? 1 : dto.getEnabled())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        counselingResourceMapper.insert(resource);
        return toResourceVO(resource);
    }

    public CounselingResourceVO updateResource(Long id, CounselingResourceCreateDTO dto) {
        CounselingResource exist = counselingResourceMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("资源不存在");
        }
        CounselingResource resource = CounselingResource.builder()
                .id(id)
                .name(dto.getName())
                .resourceType(StringUtils.hasText(dto.getResourceType()) ? dto.getResourceType() : exist.getResourceType())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .workTime(dto.getWorkTime())
                .description(dto.getDescription())
                .sortNo(dto.getSortNo() == null ? exist.getSortNo() : dto.getSortNo())
                .enabled(dto.getEnabled() == null ? exist.getEnabled() : dto.getEnabled())
                .updatedAt(LocalDateTime.now())
                .build();
        counselingResourceMapper.updateById(resource);
        return toResourceVO(counselingResourceMapper.selectById(id));
    }

    public void deleteResource(Long id) {
        if (counselingResourceMapper.selectById(id) == null) {
            throw new BusinessException("资源不存在");
        }
        counselingResourceMapper.deleteById(id);
    }

    // ---------- 预约申请 ----------

    public AppointmentVO createAppointment(Long userId, String userName, AppointmentCreateDTO dto) {
        CounselingResource resource = counselingResourceMapper.selectById(dto.getResourceId());
        if (resource == null) {
            throw new BusinessException("所选心理资源不存在");
        }
        if (resource.getEnabled() == null || resource.getEnabled() != 1) {
            throw new BusinessException("该资源暂未开放预约");
        }
        LocalDate date = null;
        if (StringUtils.hasText(dto.getAppointmentDate())) {
            try {
                date = LocalDate.parse(dto.getAppointmentDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                throw new BusinessException("预约日期格式不正确，应为 YYYY-MM-DD");
            }
        }
        AppointmentRequest request = AppointmentRequest.builder()
                .userId(userId)
                .userName(userName)
                .resourceId(resource.getId())
                .resourceName(resource.getName())
                .appointmentDate(date)
                .appointmentTime(dto.getAppointmentTime())
                .reason(dto.getReason())
                .contact(dto.getContact())
                .status(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        appointmentRequestMapper.insert(request);
        return toAppointmentVO(request);
    }

    /**
     * 我的预约列表
     */
    public List<AppointmentVO> myAppointments(Long userId) {
        List<AppointmentRequest> list = appointmentRequestMapper.selectList(
                new LambdaQueryWrapper<AppointmentRequest>()
                        .eq(AppointmentRequest::getUserId, userId)
                        .orderByDesc(AppointmentRequest::getCreatedAt));
        return list.stream().map(this::toAppointmentVO).collect(Collectors.toList());
    }

    /**
     * 用户取消自己的预约（仅待处理/已确认可取消）
     */
    public void cancelAppointment(Long userId, Long id) {
        AppointmentRequest exist = appointmentRequestMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("预约记录不存在");
        }
        if (!userId.equals(exist.getUserId())) {
            throw new BusinessException("只能操作自己的预约记录");
        }
        if (exist.getStatus() != null && (exist.getStatus() == 2 || exist.getStatus() == 3)) {
            throw new BusinessException("该预约已结束，无法取消");
        }
        AppointmentRequest update = AppointmentRequest.builder()
                .id(id)
                .status(2)
                .remark("用户取消")
                .updatedAt(LocalDateTime.now())
                .build();
        appointmentRequestMapper.updateById(update);
    }

    /**
     * 管理端预约分页
     */
    public AppointmentPageVO adminPage(int currentPage, int size, String keyword, Integer status) {
        int safePage = Math.max(currentPage, 1);
        int safeSize = Math.max(size, 1);
        LambdaQueryWrapper<AppointmentRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AppointmentRequest::getUserName, keyword)
                    .or().like(AppointmentRequest::getResourceName, keyword)
                    .or().like(AppointmentRequest::getReason, keyword));
        }
        wrapper.eq(status != null, AppointmentRequest::getStatus, status);
        wrapper.orderByDesc(AppointmentRequest::getCreatedAt);
        Page<AppointmentRequest> page = appointmentRequestMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        List<AppointmentVO> records = page.getRecords().stream().map(this::toAppointmentVO).collect(Collectors.toList());
        return AppointmentPageVO.builder().records(records).total(page.getTotal()).build();
    }

    /**
     * 管理端更新预约状态
     */
    public AppointmentVO updateStatus(Long id, Integer status, String remark) {
        AppointmentRequest exist = appointmentRequestMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("预约记录不存在");
        }
        AppointmentRequest update = AppointmentRequest.builder()
                .id(id)
                .status(status)
                .remark(remark)
                .updatedAt(LocalDateTime.now())
                .build();
        appointmentRequestMapper.updateById(update);
        return toAppointmentVO(appointmentRequestMapper.selectById(id));
    }

    // ---------- 转换 ----------

    private CounselingResourceVO toResourceVO(CounselingResource r) {
        return CounselingResourceVO.builder()
                .id(r.getId())
                .name(r.getName())
                .resourceType(r.getResourceType())
                .phone(r.getPhone())
                .address(r.getAddress())
                .workTime(r.getWorkTime())
                .description(r.getDescription())
                .sortNo(r.getSortNo())
                .enabled(r.getEnabled())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private AppointmentVO toAppointmentVO(AppointmentRequest a) {
        return AppointmentVO.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .userName(a.getUserName())
                .resourceId(a.getResourceId())
                .resourceName(a.getResourceName())
                .appointmentDate(a.getAppointmentDate())
                .appointmentTime(a.getAppointmentTime())
                .reason(a.getReason())
                .contact(a.getContact())
                .status(a.getStatus())
                .remark(a.getRemark())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
