package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConsentSubmitDTO {
    @AssertTrue(message = "必须确认已满18周岁")
    private Boolean ageConfirmed;

    @NotBlank(message = "隐私政策版本不能为空")
    private String privacyPolicyVersion;

    @NotBlank(message = "敏感信息同意版本不能为空")
    private String sensitiveInfoVersion;

    @NotBlank(message = "产品边界版本不能为空")
    private String productBoundaryVersion;
}
