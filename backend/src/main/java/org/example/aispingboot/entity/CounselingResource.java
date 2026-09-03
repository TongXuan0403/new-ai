package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心理中心资源实体（对应 counseling_resource 表）
 */
@Data
@TableName("counseling_resource")
@Builder
public class CounselingResource {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("resource_type")
    private String resourceType;

    @TableField("phone")
    private String phone;

    @TableField("address")
    private String address;

    @TableField("work_time")
    private String workTime;

    @TableField("description")
    private String description;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("enabled")
    private Integer enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
