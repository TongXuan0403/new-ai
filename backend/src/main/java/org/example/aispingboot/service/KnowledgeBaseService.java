package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispingboot.DTO.response.KnowledgePageResponseDTO;
import org.example.aispingboot.entity.Article;
import org.example.aispingboot.entity.ArticleCategory;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ArticleCategoryMapper;
import org.example.aispingboot.mapper.ArticleMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识库检索服务：供 AI 对话（MCP 工具）与 /api/knowledge 公开接口使用。
 *
 * 数据源：MySQL knowledge_article 表（status = 1 已发布），与后台「知识文章管理」完全打通。
 * - 管理后台新增 / 编辑 / 发布 / 下线文章后，调用 {@link #invalidateCache()} 使内存缓存失效；
 * - 缓存懒加载，首次访问或失效后重新查询，保证 AI 能实时检索到最新内容。
 *
 * 检索：混合排序 = 关键词相关度（标题/摘要/正文/标签打分） + 语义相似度（EmbeddingService，BGE-M3）。
 * - 语义 embedding 未配置或调用失败时自动退化为纯关键词检索；
 * - 语义命中可覆盖"换种说法查不到"的场景（如「脑子停不下来」命中「睡前如何降低反刍」）。
 */
@Service
public class KnowledgeBaseService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_RETRIEVAL_LIMIT = 3;

    /** 语义分加权的相关度阈值与权重（余弦 [0,1]） */
    private static final double SEMANTIC_THRESHOLD = 0.5;
    private static final double SEMANTIC_WEIGHT = 12.0;
    private static final int QUERY_EMBEDDING_CACHE_SIZE = 200;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private ArticleCategoryMapper articleCategoryMapper;

    @Resource
    private EmbeddingService embeddingService;

    /** 内存缓存：已发布文章列表（懒加载，null 表示未加载或已失效） */
    private volatile List<KnowledgeArticleResponseDTO> cache;

    /** 文章 embedding 缓存：articleId -> 向量 */
    private final Map<Long, float[]> embeddingCache = new ConcurrentHashMap<>();

    /** 查询 embedding 缓存：keyword -> 向量（限制条数，避免重复调用接口） */
    private final Map<String, float[]> queryEmbeddingCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
            return size() > QUERY_EMBEDDING_CACHE_SIZE;
        }
    };

    private final Object cacheLock = new Object();

    @Tool(description = "按关键词和分类搜索知识库文章，返回文章摘要列表")
    public KnowledgePageResponseDTO searchKnowledgeArticles(
            @ToolParam(description = "搜索关键词，支持标题、摘要、正文和标签") String keyword,
            @ToolParam(description = "分类名称，可为空") String category,
            @ToolParam(description = "返回数量上限") Integer limit) {
        int size = normalizeLimit(limit, DEFAULT_RETRIEVAL_LIMIT);
        List<KnowledgeArticleResponseDTO> matches = findArticles(keyword, category)
                .stream()
                .limit(size)
                .collect(Collectors.toList());

        KnowledgePageResponseDTO response = new KnowledgePageResponseDTO();
        response.setList(matches);
        response.setTotal(matches.size());
        response.setPage(1);
        response.setPageSize(size);
        response.setCategories(listKnowledgeCategories());
        return response;
    }

    @Tool(description = "根据知识库文章ID获取文章详情")
    public KnowledgeArticleResponseDTO getKnowledgeArticleById(
            @ToolParam(description = "文章ID") String articleId) {
        if (!StringUtils.hasText(articleId)) {
            throw new BusinessException("文章ID不能为空");
        }

        return getArticles().stream()
                .filter(item -> articleId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("知识内容不存在"));
    }

    @Tool(description = "列出知识库分类")
    public List<String> listKnowledgeCategories() {
        Set<String> categories = getArticles().stream()
                .map(KnowledgeArticleResponseDTO::getCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(categories);
    }

    public KnowledgePageResponseDTO searchKnowledgeArticles(String keyword, String category, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        List<KnowledgeArticleResponseDTO> matched = findArticles(keyword, category);
        int fromIndex = Math.min((safePage - 1) * safePageSize, matched.size());
        int toIndex = Math.min(fromIndex + safePageSize, matched.size());

        KnowledgePageResponseDTO response = new KnowledgePageResponseDTO();
        response.setList(matched.subList(fromIndex, toIndex));
        response.setTotal(matched.size());
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setCategories(listKnowledgeCategories());
        return response;
    }

    public String buildKnowledgeContext(String keyword, int limit) {
        List<KnowledgeArticleResponseDTO> matches = findArticles(keyword, null)
                .stream()
                .limit(normalizeLimit(limit, DEFAULT_RETRIEVAL_LIMIT))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return "知识库检索结果：未找到直接相关的文章。请基于已有心理健康常识回答，并明确说明知识库未命中。";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("知识库参考内容:\n");
        for (int i = 0; i < matches.size(); i++) {
            KnowledgeArticleResponseDTO article = matches.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(article.getTitle())
                    .append(" [")
                    .append(article.getCategory())
                    .append("]\n")
                    .append("摘要: ")
                    .append(article.getSummary())
                    .append("\n")
                    .append("要点: ")
                    .append(excerpt(article.getContent(), 260))
                    .append("\n")
                    .append("来源: ")
                    .append(StringUtils.hasText(article.getSource()) ? article.getSource() : article.getAuthor())
                    .append("\n\n");
        }
        builder.append("回答要求：优先结合以上知识库内容；如果内容不足，请明确说明并给出温和的通用建议，不要编造文章来源。\n");
        return builder.toString();
    }

    public List<KnowledgeArticleResponseDTO> getPublishedArticles() {
        return findArticles(null, null);
    }

    /**
     * 使知识库缓存失效。文章新增 / 修改 / 发布 / 下线 / 删除后调用，
     * 下次检索会从数据库重新加载，保证 AI 与前台读取最新知识。
     */
    public void invalidateCache() {
        synchronized (cacheLock) {
            cache = null;
        }
        embeddingCache.clear();
    }

    private List<KnowledgeArticleResponseDTO> findArticles(String keyword, String category) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);

        // 有关键词时启用语义检索：加载文章向量 + 生成查询向量
        float[] queryVec = null;
        if (StringUtils.hasText(keyword) && embeddingService.isEnabled()) {
            ensureEmbeddingsLoaded();
            queryVec = queryEmbedding(keyword.trim());
        }

        final float[] qv = queryVec;
        return getArticles().stream()
                .filter(item -> !StringUtils.hasText(item.getStatus()) || "published".equalsIgnoreCase(item.getStatus()))
                .filter(item -> !StringUtils.hasText(normalizedCategory) || normalizedCategory.equals(normalize(item.getCategory())))
                .sorted(Comparator
                        .comparing((KnowledgeArticleResponseDTO item) -> totalScore(item, normalizedKeyword, normalizedCategory, qv))
                        .reversed()
                        .thenComparing(this::updatedAtValue, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * 总得分 = 关键词相关度 + 语义相似度加权。
     */
    private double totalScore(KnowledgeArticleResponseDTO article, String keyword, String category, float[] queryVec) {
        int keywordScore = relevanceScore(article, keyword, category);
        if (queryVec == null) {
            return keywordScore;
        }
        Long id = parseId(article.getId());
        float[] articleVec = id == null ? null : embeddingCache.get(id);
        if (articleVec == null) {
            return keywordScore;
        }
        double similarity = embeddingService.cosineSimilarity(queryVec, articleVec);
        if (similarity < SEMANTIC_THRESHOLD) {
            return keywordScore;
        }
        return keywordScore + similarity * SEMANTIC_WEIGHT;
    }

    /**
     * 确保所有已发布文章都有 embedding（批量生成并缓存）；失败静默跳过，下次检索重试。
     */
    private void ensureEmbeddingsLoaded() {
        List<KnowledgeArticleResponseDTO> articles = getArticles();
        List<Long> missingIds = new ArrayList<>();
        List<String> missingTexts = new ArrayList<>();
        for (KnowledgeArticleResponseDTO article : articles) {
            Long id = parseId(article.getId());
            if (id == null || embeddingCache.containsKey(id)) {
                continue;
            }
            missingIds.add(id);
            missingTexts.add(buildEmbeddingText(article));
        }
        if (missingIds.isEmpty()) {
            return;
        }
        List<float[]> vectors = embeddingService.embedBatch(missingTexts);
        if (vectors == null || vectors.size() != missingIds.size()) {
            return;
        }
        for (int i = 0; i < missingIds.size(); i++) {
            embeddingCache.put(missingIds.get(i), vectors.get(i));
        }
    }

    /**
     * 生成查询向量（带缓存，避免同一关键词重复调用接口）。
     */
    private float[] queryEmbedding(String keyword) {
        synchronized (queryEmbeddingCache) {
            float[] cached = queryEmbeddingCache.get(keyword);
            if (cached != null) {
                return cached;
            }
        }
        float[] vector = embeddingService.embed(keyword);
        if (vector != null) {
            synchronized (queryEmbeddingCache) {
                queryEmbeddingCache.put(keyword, vector);
            }
        }
        return vector;
    }

    private String buildEmbeddingText(KnowledgeArticleResponseDTO article) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(article.getTitle())) {
            sb.append(article.getTitle()).append("。");
        }
        if (StringUtils.hasText(article.getSummary())) {
            sb.append(article.getSummary()).append("。");
        }
        String content = excerpt(article.getContent(), 600);
        if (StringUtils.hasText(content)) {
            sb.append(content);
        }
        String text = sb.toString();
        return text.length() > 1500 ? text.substring(0, 1500) : text;
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 懒加载已发布文章列表；缓存为 null 时从 MySQL 全量加载。
     */
    private List<KnowledgeArticleResponseDTO> getArticles() {
        List<KnowledgeArticleResponseDTO> local = cache;
        if (local == null) {
            synchronized (cacheLock) {
                if (cache == null) {
                    cache = loadPublishedArticles();
                }
                local = cache;
            }
        }
        return local;
    }

    private List<KnowledgeArticleResponseDTO> loadPublishedArticles() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, 1)
                        .orderByDesc(Article::getPublishedAt));
        Map<Long, String> categoryNames = articleCategoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(ArticleCategory::getId, ArticleCategory::getName, (a, b) -> a));
        return articles.stream()
                .map(article -> toDTO(article, categoryNames.get(article.getCategoryId())))
                .collect(Collectors.toList());
    }

    private KnowledgeArticleResponseDTO toDTO(Article article, String categoryName) {
        KnowledgeArticleResponseDTO dto = new KnowledgeArticleResponseDTO();
        dto.setId(String.valueOf(article.getId()));
        dto.setTitle(article.getTitle());
        dto.setCategory(categoryName);
        dto.setSummary(article.getSummary());
        dto.setContent(article.getContent());
        dto.setCover(article.getCoverImage());
        dto.setAuthor(article.getAuthor());
        dto.setTags(article.getTags() == null ? new ArrayList<>()
                : Arrays.stream(article.getTags().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList()));
        dto.setStatus(article.getStatus() != null && article.getStatus() == 1 ? "published" : "draft");
        dto.setSource("心理健康知识库");
        dto.setCreatedAt(toIsoString(article.getCreatedAt()));
        dto.setUpdatedAt(toIsoString(article.getUpdatedAt()));
        dto.setViews(article.getReadCount() != null ? article.getReadCount() : 0);
        dto.setLikes(0);
        return dto;
    }

    private String toIsoString(LocalDateTime time) {
        return time == null ? null : time.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private int relevanceScore(KnowledgeArticleResponseDTO article, String keyword, String category) {
        if (!StringUtils.hasText(keyword)) {
            return 1;
        }

        int score = 0;
        score += contains(article.getTitle(), keyword) ? 6 : 0;
        score += contains(article.getSummary(), keyword) ? 4 : 0;
        score += contains(article.getContent(), keyword) ? 3 : 0;
        score += contains(article.getAuthor(), keyword) ? 1 : 0;
        score += contains(article.getSource(), keyword) ? 1 : 0;
        score += article.getTags() != null && article.getTags().stream().anyMatch(tag -> contains(tag, keyword)) ? 2 : 0;
        if (StringUtils.hasText(category) && category.equals(normalize(article.getCategory()))) {
            score += 2;
        }
        return score;
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && StringUtils.hasText(keyword)
                && normalize(value).contains(normalize(keyword));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private long updatedAtValue(KnowledgeArticleResponseDTO article) {
        try {
            return StringUtils.hasText(article.getUpdatedAt())
                    ? OffsetDateTime.parse(article.getUpdatedAt()).toInstant().toEpochMilli()
                    : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private int normalizeLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, 10);
    }

    private String excerpt(String html, int maxLength) {
        if (!StringUtils.hasText(html)) {
            return "-";
        }
        String text = org.springframework.web.util.HtmlUtils.htmlUnescape(html)
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
