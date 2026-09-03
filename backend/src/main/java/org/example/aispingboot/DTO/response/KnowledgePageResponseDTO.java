package org.example.aispingboot.DTO.response;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgePageResponseDTO {
    private List<KnowledgeArticleResponseDTO> list;
    private long total;
    private int page;
    private int pageSize;
    private List<String> categories;
}
