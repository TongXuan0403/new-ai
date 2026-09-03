package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.aispingboot.DTO.command.ArticleCreateDTO;
import org.example.aispingboot.DTO.command.ArticleStatusUpdateDTO;
import org.example.aispingboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispingboot.DTO.response.KnowledgePageResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.ArticleFavorite;
import org.example.aispingboot.entity.KnowledgeArticle;
import org.example.aispingboot.entity.KnowledgeCategory;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ArticleFavoriteMapper;
import org.example.aispingboot.mapper.KnowledgeArticleMapper;
import org.example.aispingboot.mapper.KnowledgeCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {
    private final KnowledgeArticleMapper articleMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final ArticleFavoriteMapper favoriteMapper;

    public KnowledgeBaseService(KnowledgeArticleMapper articleMapper, KnowledgeCategoryMapper categoryMapper,
                                ArticleFavoriteMapper favoriteMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.favoriteMapper = favoriteMapper;
    }

    /**
     * 学生端：仅返回已发布文章 + 分类树。支持关键词、分类、标签筛选。
     */
    public KnowledgePageResponseDTO listPublished(String keyword, String category, String tag, int page, int pageSize) {
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
        if (StringUtils.hasText(tag)) {
            wrapper.and(w -> w.like(KnowledgeArticle::getTags, "," + tag + ",")
                    .or().like(KnowledgeArticle::getTags, tag + ",")
                    .or().like(KnowledgeArticle::getTags, "," + tag)
                    .or().eq(KnowledgeArticle::getTags, tag));
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

    /**
     * 学生端详情：返回已发布文章并累计浏览量。
     */
    @Transactional
    public KnowledgeArticleResponseDTO detailWithView(Long id) {
        KnowledgeArticleResponseDTO dto = getById(id, true);
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);
            articleMapper.updateById(article);
        }
        return dto;
    }

    @Transactional
    public KnowledgeArticleResponseDTO create(ArticleCreateDTO dto, Long authorId) {
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "DRAFT";
        KnowledgeArticle article = KnowledgeArticle.builder()
                .categoryId(dto.getCategoryId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .tags(dto.getTags())
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
        article.setTags(dto.getTags());
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
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        return toResponse(article, false);
    }

    @Transactional
    public void delete(Long id) {
        articleMapper.deleteById(id);
        favoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getArticleId, id));
    }

    // ------------------------------------------------------------------
    // 标签
    // ------------------------------------------------------------------

    /**
     * 已发布文章的全部标签（去重、排序），供前端筛选。
     */
    public List<String> listTags() {
        List<KnowledgeArticle> articles = articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .select(KnowledgeArticle::getTags)
                .isNotNull(KnowledgeArticle::getTags));
        Set<String> tags = new LinkedHashSet<>();
        for (KnowledgeArticle article : articles) {
            if (StringUtils.hasText(article.getTags())) {
                tags.addAll(splitTags(article.getTags()));
            }
        }
        return tags.stream().sorted().collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // 收藏
    // ------------------------------------------------------------------

    @Transactional
    public boolean addFavorite(Long userId, Long articleId) {
        if (isFavorited(userId, articleId)) {
            return false;
        }
        // 仅可收藏已发布文章
        getById(articleId, true);
        favoriteMapper.insert(ArticleFavorite.builder()
                .userId(userId)
                .articleId(articleId)
                .createdAt(LocalDateTime.now())
                .build());
        return true;
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long articleId) {
        return favoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, userId)
                .eq(ArticleFavorite::getArticleId, articleId)) > 0;
    }

    public boolean isFavorited(Long userId, Long articleId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getUserId, userId)
                .eq(ArticleFavorite::getArticleId, articleId)) > 0;
    }

    public List<Long> favoriteArticleIds(Long userId) {
        return favoriteMapper.selectList(new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .orderByDesc(ArticleFavorite::getCreatedAt))
                .stream().map(ArticleFavorite::getArticleId).collect(Collectors.toList());
    }

    /**
     * 我的收藏：仅返回已发布文章（被下线的自动不展示）。
     */
    public KnowledgePageResponseDTO myFavorites(Long userId, int page, int pageSize) {
        List<Long> ids = favoriteMapper.selectList(new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .orderByDesc(ArticleFavorite::getCreatedAt))
                .stream().map(ArticleFavorite::getArticleId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return KnowledgePageResponseDTO.builder().records(List.of()).categories(buildCategoryTree())
                    .total(0L).page((long) page).pageSize((long) pageSize).build();
        }
        Page<KnowledgeArticle> pager = new Page<>(page, Math.min(pageSize, 50));
        Page<KnowledgeArticle> result = articleMapper.selectPage(pager, new LambdaQueryWrapper<KnowledgeArticle>()
                .in(KnowledgeArticle::getId, ids)
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .orderByDesc(KnowledgeArticle::getPublishedAt));
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

    // ------------------------------------------------------------------
    // 个性化推荐
    // ------------------------------------------------------------------

    /**
     * 个性化推荐：优先推荐与用户收藏文章同分类/同标签的已发布文章；
     * 无收藏或不足时用热门文章兜底。推荐内容一律来自已审核知识库，不伪造来源。
     */
    public List<KnowledgeArticleResponseDTO> recommend(Long userId, int limit) {
        int top = Math.min(Math.max(limit, 1), 20);
        Set<Long> excluded = new HashSet<>();
        List<String> preferCategories = new ArrayList<>();
        Set<String> preferTags = new HashSet<>();
        if (userId != null) {
            List<ArticleFavorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getUserId, userId).orderByDesc(ArticleFavorite::getCreatedAt)
                    .last("LIMIT 20"));
            for (ArticleFavorite fav : favorites) {
                KnowledgeArticle article = articleMapper.selectById(fav.getArticleId());
                if (article == null || !"PUBLISHED".equals(article.getStatus())) {
                    continue;
                }
                excluded.add(article.getId());
                KnowledgeCategory category = categoryMapper.selectById(article.getCategoryId());
                if (category != null) {
                    preferCategories.add(category.getName());
                }
                if (StringUtils.hasText(article.getTags())) {
                    preferTags.addAll(splitTags(article.getTags()));
                }
            }
        }
        if (!preferCategories.isEmpty() || !preferTags.isEmpty()) {
            List<KnowledgeArticle> candidates = articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                    .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                    .orderByDesc(KnowledgeArticle::getViewCount)
                    .last("LIMIT 200"));
            List<KnowledgeArticle> scored = new ArrayList<>();
            for (KnowledgeArticle article : candidates) {
                if (excluded.contains(article.getId())) {
                    continue;
                }
                int score = 0;
                KnowledgeCategory category = categoryMapper.selectById(article.getCategoryId());
                if (category != null && preferCategories.contains(category.getName())) {
                    score += 3;
                }
                if (StringUtils.hasText(article.getTags())) {
                    Set<String> tags = splitTags(article.getTags());
                    for (String tag : preferTags) {
                        if (tags.contains(tag)) {
                            score += 2;
                            break;
                        }
                    }
                }
                if (score > 0) {
                    scored.add(article);
                }
            }
            scored.sort((a, b) -> Integer.compare(
                    scoreOf(b, preferCategories, preferTags), scoreOf(a, preferCategories, preferTags)));
            if (!scored.isEmpty()) {
                return scored.stream().limit(top).map(a -> toResponse(a, true)).collect(Collectors.toList());
            }
        }
        // 兜底：热门已发布文章
        List<KnowledgeArticle> hot = articleMapper.selectList(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .orderByDesc(KnowledgeArticle::getViewCount)
                .last("LIMIT " + top));
        return hot.stream().map(a -> toResponse(a, true)).collect(Collectors.toList());
    }

    private int scoreOf(KnowledgeArticle article, List<String> preferCategories, Set<String> preferTags) {
        int score = 0;
        KnowledgeCategory category = categoryMapper.selectById(article.getCategoryId());
        if (category != null && preferCategories.contains(category.getName())) {
            score += 3;
        }
        if (StringUtils.hasText(article.getTags())) {
            for (String tag : splitTags(article.getTags())) {
                if (preferTags.contains(tag)) {
                    score += 2;
                    break;
                }
            }
        }
        return score;
    }

    private Set<String> splitTags(String tags) {
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
                .tags(article.getTags())
                .status(article.getStatus())
                .viewCount(article.getViewCount())
                .minutes(article.getMinutes())
                .auditRemark(article.getAuditRemark())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .build();
    }
}
