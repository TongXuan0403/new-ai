package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrisisResourceResponseDTO {
    private Long id;
    private String resourceType;
    private String name;
    private String phone;
    private String description;
    private String region;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime updatedAt;
}
