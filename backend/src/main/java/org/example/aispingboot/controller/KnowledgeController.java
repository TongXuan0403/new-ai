package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispingboot.DTO.response.KnowledgePageResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public Result<KnowledgePageResponseDTO> listKnowledge(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(knowledgeBaseService.searchKnowledgeArticles(keyword, category, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeArticleResponseDTO> getKnowledgeDetail(@PathVariable("id") String id) {
        return Result.ok(knowledgeBaseService.getKnowledgeArticleById(id));
    }

    @GetMapping("/categories")
    public Result<List<String>> listCategories() {
        return Result.ok(knowledgeBaseService.listKnowledgeCategories());
    }
}
