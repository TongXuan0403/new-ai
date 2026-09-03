package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约申请实体（对应 appointment_request 表）
 */
@Data
@TableName("appointment_request")
@Builder
public class AppointmentRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("resource_id")
    private Long resourceId;

    @TableField("resource_name")
    private String resourceName;

    @TableField("appointment_date")
    private LocalDate appointmentDate;

    @TableField("appointment_time")
    private String appointmentTime;

    @TableField("reason")
    private String reason;

    @TableField("contact")
    private String contact;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
