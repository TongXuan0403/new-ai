package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识分类实体（对应 knowledge_category 表）
 */
@Data
@TableName("knowledge_category")
@Builder
public class ArticleCategory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("sort_no")
    private Integer sortNo;

    /** 状态 0禁用 1正常 */
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
