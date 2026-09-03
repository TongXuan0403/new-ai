package org.example.aispingboot.DTO.response;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeArticleResponseDTO {
    private String id;
    private String title;
    private String category;
    private String summary;
    private String content;
    private String cover;
    private String author;
    private List<String> tags;
    private String status;
    private String source;
    private String createdAt;
    private String updatedAt;
    private Integer views;
    private Integer likes;
}
