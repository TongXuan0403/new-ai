package org.example.aispingboot.controller;

import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.ArticleCreateDTO;
import org.example.aispingboot.DTO.command.ArticleStatusUpdateDTO;
import org.example.aispingboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispingboot.DTO.response.KnowledgePageResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.AuditLogService;
import org.example.aispingboot.service.KnowledgeBaseService;
import org.example.aispingboot.util.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库：学生端公开浏览；管理端审核/维护（/admin/knowledge/** 需管理员）。
 */
@RestController
public class KnowledgeController {
    private final KnowledgeBaseService knowledgeBaseService;
    private final AuditLogService auditLogService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService, AuditLogService auditLogService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/knowledge")
    public Result<KnowledgePageResponseDTO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        return Result.ok(knowledgeBaseService.listPublished(keyword, category, page, pageSize));
    }

    @GetMapping("/knowledge/article/{id}")
    public Result<KnowledgeArticleResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(knowledgeBaseService.getById(id, true));
    }

    @GetMapping("/knowledge/category/tree")
    public Result<Object> categoryTree() {
        return Result.ok(knowledgeBaseService.listPublished("", null, 1, 1).getCategories());
    }

    @GetMapping("/admin/knowledge/article/page")
    public Result<Page<KnowledgeArticleResponseDTO>> adminPage(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        SecurityUtil.requireAdmin();
        return Result.ok(knowledgeBaseService.adminPage(status, keyword, page, pageSize));
    }

    @PostMapping("/admin/knowledge/article")
    public Result<KnowledgeArticleResponseDTO> create(@Valid @RequestBody ArticleCreateDTO dto) {
        SecurityUtil.requireAdmin();
        KnowledgeArticleResponseDTO created = knowledgeBaseService.create(dto, SecurityUtil.getCurrentUserId());
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "CREATE_ARTICLE",
                "knowledge_article", created.getId(), null);
        return Result.ok(created, "文章已创建");
    }

    @PutMapping("/admin/knowledge/article/{id}")
    public Result<KnowledgeArticleResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ArticleCreateDTO dto) {
        SecurityUtil.requireAdmin();
        return Result.ok(knowledgeBaseService.update(id, dto), "文章已更新");
    }

    @DeleteMapping("/admin/knowledge/article/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        SecurityUtil.requireAdmin();
        knowledgeBaseService.delete(id);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin", "DELETE_ARTICLE",
                "knowledge_article", id, null);
        return Result.ok(true, "文章已删除");
    }

    @PutMapping("/admin/knowledge/article/{id}/status")
    public Result<KnowledgeArticleResponseDTO> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody ArticleStatusUpdateDTO dto) {
        SecurityUtil.requireAdmin();
        KnowledgeArticleResponseDTO updated = knowledgeBaseService.updateStatus(id, dto);
        auditLogService.record(SecurityUtil.getCurrentUserId(), "admin",
                "AUDIT_ARTICLE_" + dto.getStatus(), "knowledge_article", id, dto.getAuditRemark());
        return Result.ok(updated, "审核状态已更新");
    }
}
