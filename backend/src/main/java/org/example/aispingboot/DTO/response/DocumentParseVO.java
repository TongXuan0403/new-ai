package org.example.aispingboot.DTO.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文档导入解析结果：从上传的文档中识取标题与正文文本
 */
@Data
@Builder
public class DocumentParseVO {
    /** 从文件名推导的标题（不含扩展名） */
    private String title;
    /** 解析出的正文纯文本 */
    private String content;
    /** 文件类型：txt / md / pdf / doc / docx */
    private String fileType;
    /** 文件大小（字节） */
    private Long size;
}
