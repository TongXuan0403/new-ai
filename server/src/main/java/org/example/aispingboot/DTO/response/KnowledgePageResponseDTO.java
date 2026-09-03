package org.example.aispingboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePageResponseDTO {
    private List<KnowledgeArticleResponseDTO> records;
    private List<CategoryNodeDTO> categories;
    private long total;
    private long page;
    private long pageSize;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryNodeDTO {
        private Long id;
        private String name;
        private List<CategoryNodeDTO> children;
    }
}
