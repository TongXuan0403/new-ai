package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交预约申请
 */
@Data
public class AppointmentCreateDTO {
    @NotNull(message = "请选择心理中心资源")
    private Long resourceId;

    private String appointmentDate;

    private String appointmentTime;

    @Size(max = 500, message = "预约原因最长500字")
    private String reason;

    @Size(max = 100, message = "联系方式最长100字")
    private String contact;
}
