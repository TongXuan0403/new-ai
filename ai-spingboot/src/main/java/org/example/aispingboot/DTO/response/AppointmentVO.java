package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约申请返回
 */
@Data
@Builder
public class AppointmentVO {
    private Long id;
    private Long userId;
    private String userName;
    private Long resourceId;
    private String resourceName;
    private LocalDate appointmentDate;
    private String appointmentTime;
    private String reason;
    private String contact;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
