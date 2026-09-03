package org.example.aispingboot.controller;

import org.example.aispingboot.DTO.response.UploadFileVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<UploadFileVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) String businessField) {
        if (file == null || file.isEmpty()) {
            return Result.error(ResultCode.FILE_NOT_FOUND.getCode(), "请选择要上传的文件", null);
        }
        // 仅支持图片
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(ResultCode.FILE_TYPE_NOT_SUPPORTED.getCode(), "仅支持上传图片文件", null);
        }
        // 限制 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error(ResultCode.FILE_SIZE_EXCEEDED.getCode(), "图片大小不能超过 5MB", null);
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
            if (ext.length() > 10) {
                ext = "";
            }
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = "/uploads/" + datePath + "/" + storedName;

        try {
            Path dir = Paths.get(uploadDir, datePath).normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(storedName));
        } catch (IOException e) {
            return Result.error(ResultCode.FILE_SAVE_FAILED.getCode(), "文件保存失败: " + e.getMessage(), null);
        }

        return Result.ok(UploadFileVO.builder()
                .filePath(relativePath)
                .originalName(originalName)
                .size(file.getSize())
                .build());
    }
}
