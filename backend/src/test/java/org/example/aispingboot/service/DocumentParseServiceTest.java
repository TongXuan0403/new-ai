package org.example.aispingboot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.example.aispingboot.DTO.response.DocumentParseVO;
import org.example.aispingboot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文档解析服务单元测试：验证 txt / md / pdf / docx 内容识取与非法文件拦截。
 */
class DocumentParseServiceTest {

    private final DocumentParseService service = new DocumentParseService();

    @Test
    void parseText_utf8() {
        MockMultipartFile file = new MockMultipartFile("file", "说明文档.txt", "text/plain",
                "第一行内容\n第二行内容".getBytes(StandardCharsets.UTF_8));
        DocumentParseVO vo = service.parse(file);
        assertEquals("说明文档", vo.getTitle());
        assertEquals("txt", vo.getFileType());
        assertTrue(vo.getContent().contains("第一行内容"));
    }

    @Test
    void parseMarkdown() {
        MockMultipartFile file = new MockMultipartFile("file", "guide.md", "text/markdown",
                "# 标题\n\n正文段落".getBytes(StandardCharsets.UTF_8));
        DocumentParseVO vo = service.parse(file);
        assertEquals("guide", vo.getTitle());
        assertEquals("md", vo.getFileType());
        assertTrue(vo.getContent().contains("正文段落"));
    }

    @Test
    void parsePdf() throws Exception {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Hello PDF World");
                cs.endText();
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                pdfBytes = baos.toByteArray();
            }
        }
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfBytes);
        DocumentParseVO vo = service.parse(file);
        assertEquals("doc", vo.getTitle());
        assertEquals("pdf", vo.getFileType());
        assertTrue(vo.getContent().contains("Hello PDF World"));
    }

    @Test
    void parseDocx() throws Exception {
        byte[] docxBytes;
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Hello DOCX World");
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.write(baos);
                docxBytes = baos.toByteArray();
            }
        }
        MockMultipartFile file = new MockMultipartFile("file", "doc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);
        DocumentParseVO vo = service.parse(file);
        assertEquals("doc", vo.getTitle());
        assertEquals("docx", vo.getFileType());
        assertTrue(vo.getContent().contains("Hello DOCX World"));
    }

    @Test
    void unsupportedType_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "x.exe", "application/octet-stream", new byte[]{1, 2, 3});
        assertThrows(BusinessException.class, () -> service.parse(file));
    }

    @Test
    void emptyFile_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);
        assertThrows(BusinessException.class, () -> service.parse(file));
    }
}
