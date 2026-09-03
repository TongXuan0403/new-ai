package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文章分页返回
 */
@Data
@Builder
public class ArticlePageVO {
    private List<ArticleVO> records;
    private Long total;
}
