package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

/**
 * 知识分类返回
 */
@Data
@Builder
public class CategoryVO {
    private Long id;
    private String categoryName;
}
