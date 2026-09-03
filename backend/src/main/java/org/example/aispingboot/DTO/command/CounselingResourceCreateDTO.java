package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 心理中心资源创建/更新
 */
@Data
public class CounselingResourceCreateDTO {
    @NotBlank(message = "资源名称不能为空")
    @Size(max = 100, message = "资源名称最长100字")
    private String name;

    private String resourceType;

    @Size(max = 50, message = "电话最长50字")
    private String phone;

    @Size(max = 255, message = "地址最长255字")
    private String address;

    @Size(max = 100, message = "服务时间最长100字")
    private String workTime;

    @Size(max = 500, message = "说明最长500字")
    private String description;

    private Integer sortNo;

    private Integer enabled;
}
