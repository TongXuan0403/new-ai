package org.example.aispingboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.command.ArticleCreateDTO;
import org.example.aispingboot.DTO.response.ArticlePageVO;
import org.example.aispingboot.DTO.response.ArticleVO;
import org.example.aispingboot.entity.Article;
import org.example.aispingboot.entity.ArticleCategory;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.ArticleCategoryMapper;
import org.example.aispingboot.mapper.ArticleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private ArticleCategoryMapper articleCategoryMapper;

    /**
     * 分页查询文章
     *
     * @param categoryId     分类ID（可为空）
     * @param title          标题关键词（可为空）
     * @param status         状态（可为空；非管理员强制只看已发布）
     * @param isAdmin        是否为管理员（管理员可在 status 为空时查看全部）
     * @param currentPage    页码
     * @param size           每页条数
     * @param sortField      排序字段（publishedAt / readCount）
     * @param sortDirection  排序方向（desc / asc）
     */
    public ArticlePageVO page(Long categoryId, String title, Integer status, boolean isAdmin,
                              int currentPage, int size, String sortField, String sortDirection) {
        int safePage = Math.max(currentPage, 1);
        int safeSize = Math.max(size, 1);
        // 非管理员只能看已发布
        Integer effectiveStatus = status;
        if (!isAdmin) {
            effectiveStatus = 1;
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(categoryId != null, Article::getCategoryId, categoryId)
                .like(StringUtils.hasText(title), Article::getTitle, title)
                .eq(effectiveStatus != null, Article::getStatus, effectiveStatus);

        boolean desc = !"asc".equalsIgnoreCase(sortDirection);
        if ("readCount".equals(sortField)) {
            wrapper.orderByDesc(Article::getReadCount);
        } else if ("publishedAt".equals(sortField)) {
            wrapper.orderByDesc(Article::getPublishedAt);
        } else {
            wrapper.orderByDesc(Article::getCreatedAt);
        }

        Page<Article> page = articleMapper.selectPage(new Page<>(safePage, safeSize), wrapper);

        // 批量查分类名
        Map<Long, String> categoryNames = listCategoryNames();

        List<ArticleVO> records = page.getRecords().stream()
                .map(a -> toVO(a, categoryNames.get(a.getCategoryId())))
                .collect(Collectors.toList());

        return ArticlePageVO.builder()
                .records(records)
                .total(page.getTotal())
                .build();
    }

    /**
     * 文章详情
     *
     * @param increaseRead 是否增加阅读量（前台浏览时 true）
     */
    public ArticleVO getDetail(Long id, boolean increaseRead) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        if (increaseRead) {
            Article update = Article.builder()
                    .id(id)
                    .readCount((article.getReadCount() == null ? 0 : article.getReadCount()) + 1)
                    .build();
            articleMapper.updateById(update);
        }
        Map<Long, String> categoryNames = listCategoryNames();
        return toVO(article, categoryNames.get(article.getCategoryId()));
    }

    public ArticleVO create(ArticleCreateDTO dto) {
        Article article = Article.builder()
                .title(dto.getTitle())
                .categoryId(dto.getCategoryId() == null ? 0L : dto.getCategoryId())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .coverImage(dto.getCoverImage())
                .tags(dto.getTags())
                .status(dto.getStatus() == null ? 0 : dto.getStatus())
                .readCount(0)
                .author("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        if (article.getStatus() == 1) {
            article.setPublishedAt(LocalDateTime.now());
        }
        articleMapper.insert(article);
        Map<Long, String> categoryNames = listCategoryNames();
        return toVO(article, categoryNames.get(article.getCategoryId()));
    }

    public ArticleVO update(Long id, ArticleCreateDTO dto) {
        Article exist = articleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("文章不存在");
        }
        Article.ArticleBuilder ub = Article.builder()
                .id(id)
                .title(dto.getTitle())
                .categoryId(dto.getCategoryId() == null ? 0L : dto.getCategoryId())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .coverImage(dto.getCoverImage())
                .tags(dto.getTags());
        if (dto.getStatus() != null) {
            ub.status(dto.getStatus());
            // 从草稿变为已发布时记录发布时间
            if (dto.getStatus() == 1 && exist.getStatus() != 1) {
                ub.publishedAt(LocalDateTime.now());
            }
        }
        ub.updatedAt(LocalDateTime.now());
        articleMapper.updateById(ub.build());
        return getDetail(id, false);
    }

    public void updateStatus(Long id, Integer status) {
        Article exist = articleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("文章不存在");
        }
        Article.ArticleBuilder ub = Article.builder().id(id).status(status);
        if (status != null && status == 1 && exist.getStatus() != 1) {
            ub.publishedAt(LocalDateTime.now());
        }
        articleMapper.updateById(ub.build());
    }

    public void delete(Long id) {
        if (articleMapper.selectById(id) == null) {
            throw new BusinessException("文章不存在");
        }
        articleMapper.deleteById(id);
    }

    public Map<Long, String> listCategoryNames() {
        List<ArticleCategory> categories = articleCategoryMapper.selectList(null);
        return categories.stream()
                .collect(Collectors.toMap(ArticleCategory::getId, ArticleCategory::getName, (a, b) -> a));
    }

    private ArticleVO toVO(Article article, String categoryName) {
        return ArticleVO.builder()
                .id(article.getId())
                .categoryId(article.getCategoryId())
                .categoryName(categoryName)
                .title(article.getTitle())
                .summary(article.getSummary())
                .content(article.getContent())
                .coverImage(article.getCoverImage())
                .tags(article.getTags())
                .status(article.getStatus())
                .readCount(article.getReadCount())
                .author(article.getAuthor())
                .authorName(article.getAuthor())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
