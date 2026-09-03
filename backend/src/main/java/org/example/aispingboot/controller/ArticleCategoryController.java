package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.CategoryVO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.ArticleCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/category")
public class ArticleCategoryController {
    @Resource
    private ArticleCategoryService articleCategoryService;

    @GetMapping("/tree")
    public Result<List<CategoryVO>> tree() {
        return Result.ok(articleCategoryService.tree());
    }
}
