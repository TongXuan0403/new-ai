package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrisisResourceDTO {
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    @NotBlank(message = "资源名称不能为空")
    @Size(max = 100, message = "资源名称不能超过100个字符")
    private String name;

    @Size(max = 30, message = "电话不能超过30个字符")
    private String phone;

    @Size(max = 500, message = "说明不能超过500个字符")
    private String description;

    @Size(max = 100, message = "地区不能超过100个字符")
    private String region;

    private Boolean enabled;

    private Integer sortOrder;
}
