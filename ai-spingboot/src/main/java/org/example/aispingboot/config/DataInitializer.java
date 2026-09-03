package org.example.aispingboot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispingboot.entity.Article;
import org.example.aispingboot.entity.ArticleCategory;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.mapper.ArticleCategoryMapper;
import org.example.aispingboot.mapper.ArticleMapper;
import org.example.aispingboot.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动时初始化种子数据：
 * - 管理员账号 admin / 演示账号 demo（密码均为 123456）
 * - 知识分类与文章（供前台展示与 AI 检索）
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ArticleCategoryMapper articleCategoryMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Override
    public void run(String... args) {
        initUsers();
        initCategoriesAndArticles();
    }

    private void initUsers() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")) == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(encoder.encode("123456"))
                    .nickname("系统管理员")
                    .userType(2)
                    .status(1)
                    .gender(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userMapper.insert(admin);
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "demo")) == 0) {
            User demo = User.builder()
                    .username("demo")
                    .email("demo@example.com")
                    .password(encoder.encode("123456"))
                    .nickname("演示用户")
                    .userType(1)
                    .status(1)
                    .gender(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userMapper.insert(demo);
        }
    }

    private void initCategoriesAndArticles() {
        if (articleCategoryMapper.selectCount(null) > 0) {
            return;
        }
        Map<String, Long> categoryIds = new LinkedHashMap<>();
        String[] names = {"情绪管理", "压力应对", "人际交往", "睡眠健康"};
        int sort = 1;
        for (String name : names) {
            ArticleCategory category = ArticleCategory.builder()
                    .name(name)
                    .sortNo(sort++)
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            articleCategoryMapper.insert(category);
            categoryIds.put(name, category.getId());
        }

        if (articleMapper.selectCount(null) == 0) {
            insertArticle(categoryIds.get("情绪管理"), "如何识别并调节日常情绪",
                    "学会识别自己的情绪，是情绪管理的第一步。",
                    "<h2>识别情绪</h2><p>情绪没有好坏之分，每一种情绪都在传递信息。试着用具体的词描述当下的感受，比如“烦躁”“委屈”而不是笼统的“不舒服”。</p><h2>调节方法</h2><ul><li>深呼吸练习：吸气 4 秒，屏息 4 秒，呼气 6 秒</li><li>情绪日记：记录触发事件与身体反应</li><li>适度运动：散步或慢跑 20 分钟</li></ul>",
                    "情绪管理,自我调节", 1);
            insertArticle(categoryIds.get("压力应对"), "大学生压力管理实用指南",
                    "学业、社交与未来的多重压力下，学会科学应对。",
                    "<h2>压力的来源</h2><p>考试、作业、人际、就业……适度的压力能激发动力，但长期高压会影响身心健康。</p><h2>应对策略</h2><ul><li>把大目标拆成小步骤</li><li>每天留出 30 分钟完全属于自己的时间</li><li>与信任的人倾诉，不要独自硬扛</li><li>必要时寻求学校心理咨询中心帮助</li></ul>",
                    "压力,考试,焦虑", 1);
            insertArticle(categoryIds.get("人际交往"), "改善人际关系的五个小技巧",
                    "从倾听开始，让关系更温暖。",
                    "<h2>五个小技巧</h2><ol><li>认真倾听，不急于打断</li><li>表达感谢要具体</li><li>学会说“不”，设立边界</li><li>主动联系，关系需要经营</li><li>接纳不同，求同存异</li></ol>",
                    "人际交往,沟通", 1);
            insertArticle(categoryIds.get("睡眠健康"), "改善睡眠质量的科学方法",
                    "从作息到环境，科学改善睡眠。",
                    "<h2>建立规律作息</h2><p>固定起床时间，比固定入睡时间更重要。</p><h2>睡前习惯</h2><ul><li>睡前一小时远离电子屏幕</li><li>卧室保持黑暗与凉爽</li><li>下午之后避免咖啡因</li><li>失眠时起床走动，不要硬躺</li></ul>",
                    "睡眠,失眠,作息", 1);
        }
    }

    private void insertArticle(Long categoryId, String title, String summary, String content, String tags, Integer status) {
        Article article = Article.builder()
                .categoryId(categoryId)
                .title(title)
                .summary(summary)
                .content(content)
                .tags(tags)
                .status(status)
                .readCount(0)
                .author("admin")
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        articleMapper.insert(article);
    }
}
