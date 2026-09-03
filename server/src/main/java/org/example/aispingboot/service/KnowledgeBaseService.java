package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.command.ArticleCreateDTO;
import org.example.aispingboot.DTO.command.ArticleStatusUpdateDTO;
import org.example.aispingboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispingboot.DTO.response.KnowledgePageResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.KnowledgeArticle;
import org.example.aispingboot.entity.KnowledgeCategory;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.KnowledgeArticleMapper;
import org.example.aispingboot.mapper.KnowledgeCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {
    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeCategoryMapper categoryMapper;

    public KnowledgeBaseService(KnowledgeArticleMapper articleMapper, KnowledgeCategoryMapper categoryMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * 学生端：仅返回已发布文章 + 分类树。
     */
    public KnowledgePageResponseDTO listPublished(String keyword, String category, int page, int pageSize) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .orderByDesc(KnowledgeArticle::getPublishedAt);
        if (StringUtils.hasText(category) && !"全部".equals(category)) {
            List<Long> categoryIds = categoryMapper.selectList(new LambdaQueryWrapper<KnowledgeCategory>()
                            .like(KnowledgeCategory::getName, category))
                    .stream().map(KnowledgeCategory::getId).collect(Collectors.toList());
            if (categoryIds.isEmpty()) {
                wrapper.eq(KnowledgeArticle::getCategoryId, -1L);
            } else {
                wrapper.in(KnowledgeArticle::getCategoryId, categoryIds);
            }
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(KnowledgeArticle::getTitle, keyword)
                    .or().like(KnowledgeArticle::getSummary, keyword)
                    .or().like(KnowledgeArticle::getContent, keyword));
        }
        Page<KnowledgeArticle> pager = new Page<>(page, Math.min(pageSize, 50));
        Page<KnowledgeArticle> result = articleMapper.selectPage(pager, wrapper);
        List<KnowledgeArticleResponseDTO> records = result.getRecords().stream()
                .map(a -> toResponse(a, true)).collect(Collectors.toList());
        return KnowledgePageResponseDTO.builder()
                .records(records)
                .categories(buildCategoryTree())
                .total(result.getTotal())
                .page(result.getCurrent())
                .pageSize(result.getSize())
                .build();
    }

    /**
     * 管理端：全量文章（含草稿/待审）。
     */
    public Page<KnowledgeArticleResponseDTO> adminPage(String status, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<KnowledgeArticle>()
                .orderByDesc(KnowledgeArticle::getUpdatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(KnowledgeArticle::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(KnowledgeArticle::getTitle, keyword);
        }
        Page<KnowledgeArticle> pager = new Page<>(page, Math.min(pageSize, 100));
        Page<KnowledgeArticle> result = articleMapper.selectPage(pager, wrapper);
        Page<KnowledgeArticleResponseDTO> response = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        response.setRecords(result.getRecords().stream().map(a -> toResponse(a, false)).collect(Collectors.toList()));
        return response;
    }

    public KnowledgeArticleResponseDTO getById(Long id, boolean publishedOnly) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        if (publishedOnly && !"PUBLISHED".equals(article.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在或未发布");
        }
        return toResponse(article, publishedOnly);
    }

    @Transactional
    public KnowledgeArticleResponseDTO create(ArticleCreateDTO dto, Long authorId) {
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "DRAFT";
        KnowledgeArticle article = KnowledgeArticle.builder()
                .categoryId(dto.getCategoryId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .source(dto.getSource())
                .coverUrl(dto.getCoverUrl())
                .authorId(authorId)
                .status(status)
                .viewCount(0)
                .minutes(dto.getMinutes() == null ? 5 : dto.getMinutes())
                .publishedAt("PUBLISHED".equals(status) ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        articleMapper.insert(article);
        return toResponse(article, false);
    }

    @Transactional
    public KnowledgeArticleResponseDTO update(Long id, ArticleCreateDTO dto) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        article.setCategoryId(dto.getCategoryId());
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setSource(dto.getSource());
        article.setCoverUrl(dto.getCoverUrl());
        article.setMinutes(dto.getMinutes() == null ? article.getMinutes() : dto.getMinutes());
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return toResponse(article, false);
    }

    @Transactional
    public KnowledgeArticleResponseDTO updateStatus(Long id, ArticleStatusUpdateDTO dto) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        article.setStatus(dto.getStatus());
        article.setAuditRemark(dto.getAuditRemark());
        if ("PUBLISHED".equals(dto.getStatus()) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        if (("REJECTED".equals(dto.getStatus()) || "DRAFT".equals(dto.getStatus())
                || "OFFLINE".equals(dto.getStatus())) && article.getPublishedAt() != null) {
            // 下线/驳回后保留发布时间作为历史，不再回写
        }
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return toResponse(article, false);
    }

    @Transactional
    public void delete(Long id) {
        articleMapper.deleteById(id);
    }

    /**
     * 构建 AI 知识上下文：按标题/摘要关键词简单匹配，只返回已审核文章，不伪造出处。
     */
    public String buildKnowledgeContext(String userMessage, int topK) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }
        List<KnowledgeArticle> articles = articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .orderByDesc(KnowledgeArticle::getPublishedAt));
        List<String> hits = new ArrayList<>();
        for (KnowledgeArticle article : articles) {
            String title = article.getTitle() == null ? "" : article.getTitle();
            String summary = article.getSummary() == null ? "" : article.getSummary();
            if (containsAny(title, summary, userMessage)) {
                hits.add("《" + title + "》" + (StringUtils.hasText(summary) ? "：" + summary : ""));
                if (hits.size() >= topK) {
                    break;
                }
            }
        }
        if (hits.isEmpty()) {
            return "";
        }
        return "可参考的已审核知识内容：\n" + String.join("\n", hits);
    }

    private boolean containsAny(String title, String summary, String query) {
        if (!StringUtils.hasText(query)) {
            return false;
        }
        String t = title == null ? "" : title;
        String s = summary == null ? "" : summary;
        String[] words = query.split("\\s+");
        for (String word : words) {
            if (word.length() >= 2 && (t.contains(word) || s.contains(word))) {
                return true;
            }
        }
        return false;
    }

    private List<KnowledgePageResponseDTO.CategoryNodeDTO> buildCategoryTree() {
        List<KnowledgeCategory> roots = categoryMapper.selectList(new LambdaQueryWrapper<KnowledgeCategory>()
                .eq(KnowledgeCategory::getParentId, 0L)
                .eq(KnowledgeCategory::getStatus, 1)
                .orderByAsc(KnowledgeCategory::getSortNo));
        List<KnowledgePageResponseDTO.CategoryNodeDTO> tree = new ArrayList<>();
        for (KnowledgeCategory root : roots) {
            List<KnowledgeCategory> children = categoryMapper.selectList(new LambdaQueryWrapper<KnowledgeCategory>()
                    .eq(KnowledgeCategory::getParentId, root.getId())
                    .eq(KnowledgeCategory::getStatus, 1)
                    .orderByAsc(KnowledgeCategory::getSortNo));
            tree.add(KnowledgePageResponseDTO.CategoryNodeDTO.builder()
                    .id(root.getId())
                    .name(root.getName())
                    .children(children.stream().map(c -> KnowledgePageResponseDTO.CategoryNodeDTO.builder()
                            .id(c.getId()).name(c.getName()).children(List.of()).build())
                            .collect(Collectors.toList()))
                    .build());
        }
        return tree;
    }

    private KnowledgeArticleResponseDTO toResponse(KnowledgeArticle article, boolean publishedOnly) {
        String categoryName = "";
        if (article.getCategoryId() != null) {
            KnowledgeCategory category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }
        return KnowledgeArticleResponseDTO.builder()
                .id(article.getId())
                .categoryId(article.getCategoryId())
                .categoryName(categoryName)
                .title(article.getTitle())
                .summary(article.getSummary())
                .content(publishedOnly ? article.getContent() : article.getContent())
                .source(article.getSource())
                .coverUrl(article.getCoverUrl())
                .status(article.getStatus())
                .viewCount(article.getViewCount())
                .minutes(article.getMinutes())
                .auditRemark(article.getAuditRemark())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .build();
    }
}
