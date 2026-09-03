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
public class KnowledgeArticleResponseDTO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String content;
    private String source;
    private String coverUrl;
    private String status;
    private Integer viewCount;
    private Integer minutes;
    private String auditRemark;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
