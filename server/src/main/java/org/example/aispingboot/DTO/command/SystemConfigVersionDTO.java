package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemConfigVersionDTO {
    @NotBlank(message = "配置类型不能为空")
    private String configType;

    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称不能超过100个字符")
    private String name;

    @NotBlank(message = "版本号不能为空")
    @Size(max = 50, message = "版本号不能超过50个字符")
    private String version;

    private String content;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
