package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.CategoryVO;
import org.example.aispingboot.entity.ArticleCategory;
import org.example.aispingboot.mapper.ArticleCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleCategoryService {
    @Resource
    private ArticleCategoryMapper articleCategoryMapper;

    public List<CategoryVO> tree() {
        LambdaQueryWrapper<ArticleCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ArticleCategory::getStatus, 1)
                .orderByAsc(ArticleCategory::getSortNo);
        return articleCategoryMapper.selectList(wrapper).stream()
                .map(c -> CategoryVO.builder()
                        .id(c.getId())
                        .categoryName(c.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
