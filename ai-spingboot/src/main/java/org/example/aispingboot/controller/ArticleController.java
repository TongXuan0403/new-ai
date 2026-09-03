package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispingboot.DTO.command.ArticleCreateDTO;
import org.example.aispingboot.DTO.command.ArticleStatusDTO;
import org.example.aispingboot.DTO.response.ArticlePageVO;
import org.example.aispingboot.DTO.response.ArticleVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.ArticleService;
import org.example.aispingboot.util.UserContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge/article")
public class ArticleController {
    @Resource
    private ArticleService articleService;

    /**
     * 文章分页。前台（未登录/普通用户）仅看已发布；管理员可筛选全部状态。
     */
    @GetMapping("/page")
    public Result<ArticlePageVO> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        boolean isAdmin = UserContext.isAdmin();
        return Result.ok(articleService.page(categoryId, title, status, isAdmin, currentPage, size, sortField, sortDirection));
    }

    @PostMapping
    public Result<ArticleVO> create(@Valid @RequestBody ArticleCreateDTO dto) {
        requireAdmin();
        return Result.ok(articleService.create(dto));
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> detail(@PathVariable Long id) {
        return Result.ok(articleService.getDetail(id, true));
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new BusinessException("无权限操作，仅管理员可管理文章");
        }
    }

    @PutMapping("/{id}")
    public Result<ArticleVO> update(@PathVariable Long id, @Valid @RequestBody ArticleCreateDTO dto) {
        requireAdmin();
        return Result.ok(articleService.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ArticleStatusDTO dto) {
        requireAdmin();
        articleService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        articleService.delete(id);
        return Result.ok();
    }
}
