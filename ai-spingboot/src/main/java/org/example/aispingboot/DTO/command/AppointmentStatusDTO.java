package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端处理预约状态
 */
@Data
public class AppointmentStatusDTO {
    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
