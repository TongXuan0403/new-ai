package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传返回
 */
@Data
@Builder
public class UploadFileVO {
    /** 可访问的相对路径，如 /uploads/2026/09/xxx.png */
    private String filePath;
    private String originalName;
    private Long size;
}
