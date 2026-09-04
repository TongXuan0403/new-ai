package org.example.aispingboot.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.example.aispingboot.DTO.response.DocumentParseVO;
import org.example.aispingboot.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文档导入解析：支持 txt / md / pdf / doc / docx，
 * 从前端拖拽或选择上传的文件中识取正文文本，供写入知识文章使用。
 */
@Service
public class DocumentParseService {

    /** 单个文档大小上限（10MB，与后端 multipart 配置一致） */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    /** 解析文本上限：避免超大文档撑爆接口响应 */
    private static final int MAX_CONTENT_LENGTH = 200_000;

    /**
     * 解析上传文档，返回标题与正文。
     *
     * @param file 上传的文档文件（txt / md / pdf / doc / docx）
     * @return 解析结果
     */
    public DocumentParseVO parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文档");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文档大小不能超过 10MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = extensionOf(originalName);
        if (ext == null) {
            throw new BusinessException("无法识别文件类型，请选择 txt / md / pdf / doc / docx 文档");
        }

        String content;
        try {
            content = switch (ext) {
                case "txt", "md", "markdown" -> parseText(file);
                case "docx" -> parseDocx(file);
                case "doc" -> parseDoc(file);
                case "pdf" -> parsePdf(file);
                default -> throw new BusinessException("不支持的文件类型 ." + ext + "，目前支持 txt / md / pdf / doc / docx");
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文档解析失败：" + e.getMessage());
        }

        if (content == null || content.isBlank()) {
            throw new BusinessException("未从文档中识别到文本内容，请检查文件是否为空或为扫描件图片");
        }

        return DocumentParseVO.builder()
                .title(titleOf(originalName))
                .content(content)
                .fileType(ext)
                .size(file.getSize())
                .build();
    }

    /** 纯文本 / Markdown：按 UTF-8 读取，出现乱码时回退 GBK */
    private String parseText(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (utf8.indexOf('\uFFFD') < 0) {
            return truncate(utf8);
        }
        return truncate(new String(bytes, Charset.forName("GBK")));
    }

    /** Word .docx：按文档顺序提取段落与表格 */
    private String parseDocx(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph p) {
                    String text = p.getText();
                    if (text != null && !text.isBlank()) {
                        sb.append(text).append('\n');
                    }
                } else if (element instanceof XWPFTable table) {
                    for (var row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (var cell : row.getTableCells()) {
                            cells.add(cell.getText().trim());
                        }
                        sb.append(String.join(" | ", cells)).append('\n');
                    }
                }
            }
            return truncate(sb.toString());
        }
    }

    /** Word .doc（旧版二进制格式）：使用 POI HWPF WordExtractor */
    private String parseDoc(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream(); WordExtractor extractor = new WordExtractor(in)) {
            return truncate(extractor.getText());
        }
    }

    /** PDF：使用 PDFBox 提取文本层 */
    private String parsePdf(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return truncate(stripper.getText(doc));
        }
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) : content;
    }

    /** 从文件名取小写扩展名；无法识别返回 null */
    private String extensionOf(String name) {
        if (name == null) {
            return null;
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return null;
        }
        return name.substring(idx + 1).toLowerCase();
    }

    /** 标题取文件名去掉扩展名 */
    private String titleOf(String name) {
        if (name == null || name.isBlank()) {
            return "未命名文档";
        }
        int idx = name.lastIndexOf('.');
        String base = idx > 0 ? name.substring(0, idx) : name;
        return base.isBlank() ? "未命名文档" : base.trim();
    }
}
