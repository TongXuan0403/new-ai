package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心理中心资源返回
 */
@Data
@Builder
public class CounselingResourceVO {
    private Long id;
    private String name;
    private String resourceType;
    private String phone;
    private String address;
    private String workTime;
    private String description;
    private Integer sortNo;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
