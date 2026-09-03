package org.example.aispingboot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.aispingboot.DTO.response.KnowledgeArticleResponseDTO;
import org.example.aispingboot.DTO.response.KnowledgePageResponseDTO;
import org.example.aispingboot.exception.BusinessException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_RETRIEVAL_LIMIT = 3;

    private final ObjectMapper objectMapper;
    private final List<KnowledgeArticleResponseDTO> articles = new ArrayList<>();

    public KnowledgeBaseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadKnowledgeBase() {
        Resource resource = new ClassPathResource("knowledge-base/articles.json");
        if (!resource.exists()) {
            throw new BusinessException("知识库资源文件不存在");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            List<KnowledgeArticleResponseDTO> loaded = objectMapper.readValue(
                    inputStream,
                    new TypeReference<>() {
                    }
            );
            articles.clear();
            articles.addAll(loaded);
        } catch (IOException e) {
            throw new BusinessException("加载知识库失败: " + e.getMessage());
        }
    }

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

        return articles.stream()
                .filter(item -> articleId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("知识内容不存在"));
    }

    @Tool(description = "列出知识库分类")
    public List<String> listKnowledgeCategories() {
        Set<String> categories = articles.stream()
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

    private List<KnowledgeArticleResponseDTO> findArticles(String keyword, String category) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);

        return articles.stream()
                .filter(item -> !StringUtils.hasText(item.getStatus()) || "published".equalsIgnoreCase(item.getStatus()))
                .filter(item -> !StringUtils.hasText(normalizedCategory) || normalizedCategory.equals(normalize(item.getCategory())))
                .sorted(Comparator
                        .comparing((KnowledgeArticleResponseDTO item) -> relevanceScore(item, normalizedKeyword, normalizedCategory))
                        .reversed()
                        .thenComparing(this::updatedAtValue, Comparator.reverseOrder()))
                .collect(Collectors.toList());
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
