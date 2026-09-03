package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章返回对象
 */
@Data
@Builder
public class ArticleVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    /** 标签，逗号分隔 */
    private String tags;
    /** 状态 0草稿 1已发布 2已下线 */
    private Integer status;
    private Integer readCount;
    private String author;
    /** 作者名（与 author 同值，兼容前端 authorName 字段） */
    private String authorName;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
