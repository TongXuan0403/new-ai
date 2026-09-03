package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识文章实体（对应 knowledge_article 表）
 */
@Data
@TableName("knowledge_article")
@Builder
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("category_id")
    private Long categoryId;

    private String title;

    private String summary;

    private String content;

    @TableField("cover_image")
    private String coverImage;

    /** 标签，逗号分隔 */
    private String tags;

    /** 状态 0草稿 1已发布 2已下线 */
    private Integer status;

    @TableField("read_count")
    private Integer readCount;

    private String author;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
