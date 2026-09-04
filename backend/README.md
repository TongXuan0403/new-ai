# backend · 心理健康助手后端服务

> Spring Boot 3.5 + **Spring AI** + **MCP** 的 AI 心理支持后端服务，提供认证、AI 流式对话、知识库检索、情绪日记、知识管理与数据统计等 REST 与 SSE 接口。

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-6DB33F?logo=spring&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.8-6DB33F)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.7-1B6AC6)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-000000)

---

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 用户认证 | 注册 / 登录 / 登出 / 当前用户；JWT 无状态鉴权；`user_type` 区分普通用户（1）与管理员（2） |
| AI 心理对话 | Spring AI `ChatClient` + 会话窗口记忆（最近 30 条），DeepSeek 兼容接口，**SSE 流式**返回；无 Key 或异常时本地兜底回复 |
| 知识库 RAG | 知识库数据源为 MySQL `knowledge_article` **已发布文章**（与后台「知识文章管理」打通：增删改 / 发布 / 下线后缓存自动失效、AI 立即生效）；通过 `@Tool` 暴露为 **MCP 工具**（`searchKnowledgeArticles` / `getKnowledgeArticleById` / `listKnowledgeCategories`），对话中自主检索增强回答 |
| 情绪日记 | 用户记录情绪评分 / 主导情绪 / 诱因 / 压力等级；管理端分页查看与删除 |
| 知识文章 | 分类树 + 文章分页（前台仅已发布、管理员全量）、富文本内容、封面上传、状态流转 |
| 数据统计 | `/api/data-analytics/overview` 运营聚合概览 |
| 文件上传 | 10MB 限制，`uploads/` 本地存储，返回可访问 URL |
| 心理中心预约/转介（P2） | 可配置资源库（校内/热线/本地）、预约申请与处理流转（`/api/counseling/**`） |
| 匿名校园报告（P2） | `/api/data-analytics/campus-report` 纯聚合报告，无个人数据 |
| 主题化成长计划（P2） | 计划 CRUD/审核发布、用户进度跟踪（`/api/growth-plan/**`） |

## 技术栈

- **基础框架**：Spring Boot 3.5.15（Java 17）、Spring Web、Spring Validation
- **AI**：Spring AI 1.1.8（`spring-ai-starter-model-openai` 接入 DeepSeek）、`spring-ai-starter-mcp-server-webmvc`（MCP Server，STREAMABLE 协议）、`MessageWindowChatMemory`（30 条窗口记忆）
- **持久化**：MyBatis-Plus 3.5.7（`mybatis-plus-spring-boot3-starter`）、MySQL 8.0、spring-boot-starter-data-jdbc
- **安全**：Spring Security + `com.auth0:java-jwt`（4.4.0）
- **工具**：Lombok、Hutool 5.8.25

## 环境要求

- JDK 17、Maven 3.9+
- MySQL 8.0
- （可选）DeepSeek / OpenAI 兼容 API Key；未配置时 AI 对话使用本地兜底回复，流程不中断

## 快速开始

### 1. 初始化数据库

```bash
mysql -uroot -p < sql/schema.sql
```

> 脚本创建库 `mental_health_assistant` 与 10 张表（`user` / `consultation_session` / `consultation_message` / `knowledge_category` / `knowledge_article` / `emotion_diary` / `counseling_resource` / `appointment_request` / `growth_plan` / `growth_plan_progress`）。种子数据（账号、知识、心理资源、成长计划）由后端启动时通过 `DataInitializer` 自动写入。

### 2. 配置

默认配置见 `src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/mental_health_assistant` | 数据库连接 |
| `server.port` | `1236` | 服务端口 |
| `spring.ai.openai.api-key` | `${AI_API_KEY:sk-no-key-configured}` | 从环境变量 `AI_API_KEY` 读取；未配置时 AI 对话走本地兜底回复 |
| `spring.ai.openai.base-url` | `https://api.deepseek.com` | OpenAI 兼容接口地址 |
| `spring.ai.openai.chat.options.model` | `deepseek-v4-pro` | 对话模型 |
| `jwt.secret` | 本地默认值 | **生产环境务必更换强密钥** |
| `app.upload-dir` | `uploads` | 文件上传目录 |

**推荐**：通过环境变量注入密钥，避免密钥进入版本库：

```bash
# PowerShell
$env:AI_API_KEY='你的Key'          # 设置后即接入 DeepSeek 智能对话
$env:JWT_SECRET='生产环境强密钥'
mvn spring-boot:run
```

> **密钥保护**：本地调试时也可直接在 `application.yml` 填写 API Key，但该文件已用 `git update-index --skip-worktree` 标记，**本地 Key 不会进入版本库 / 推送到远程**。

### 3. 启动

```bash
mvn spring-boot:run
```

启动成功后：

- 服务地址：<http://localhost:1236>
- 种子账号：`admin` / `demo`，密码均为 `123456`（`admin` 为管理员，`demo` 为普通用户）
- 知识库来自 MySQL `knowledge_article` 表（`status=1` 已发布文章）；旧静态知识库 `knowledge-base/articles.json` 会在启动时自动迁移入库（按标题幂等，分类自动创建）

## 项目结构

```
backend/
├── sql/schema.sql                     # 建表脚本
├── src/main/java/org/example/aispingboot/
│   ├── AiService/                     # AI 相关服务与提示词管理
│   │   ├── PsychologicalSupportService.java  # 心理对话核心（SSE 流式 + 知识库上下文）
│   │   └── PromptManage.java          # 系统提示词
│   ├── config/                        # ChatClient / MCP / Security / JWT / MyBatis-Plus 等配置
│   ├── controller/                    # REST 接口（user / psychological-chat / knowledge / emotion-diary / article / data-analytics / file）
│   ├── service/                       # 业务逻辑（含 KnowledgeBaseService：RAG 检索 + MCP 工具）
│   ├── mapper/                        # MyBatis-Plus Mapper
│   ├── entity/                        # 数据实体
│   ├── DTO/                           # 入参（command）与出参（response）
│   ├── common/                        # 统一返回 Result / 全局异常
│   └── util/                          # JWT 工具 / 用户上下文
└── src/main/resources/
    ├── application.yml                # 配置
    └── knowledge-base/articles.json   # 旧静态知识（启动时自动迁移到 MySQL，仅作迁移源）
```

## API 概览

统一前缀 `/api`，统一响应结构 `Result{code, msg, data}`；需登录的接口在请求头携带 `token: <JWT>`。

### 用户认证 `/api/user`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/user/login` | 登录，返回 token 与用户信息 |
| POST | `/user/add` | 注册 |
| POST | `/user/logout` | 登出（JWT 无状态，前端清除即可） |
| GET | `/user/current` | 获取当前登录用户 |

### AI 心理对话 `/api/psychological-chat`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/psychological-chat/session/start` | 创建会话并保存首条消息 |
| POST | `/psychological-chat/stream` | **SSE 流式对话**（`text/event-stream`） |
| GET | `/psychological-chat/sessions` | 会话分页（管理员全部 / 用户本人） |
| GET | `/psychological-chat/sessions/{id}/messages` | 会话消息列表 |
| DELETE | `/psychological-chat/sessions/{id}` | 删除会话 |
| GET | `/psychological-chat/session/{id}/emotion` | 会话情绪分析 |

### 知识库 / 文章 `/api/knowledge`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/knowledge` | 知识库分页检索（keyword / category） |
| GET | `/knowledge/{id}` | 知识详情 |
| GET | `/knowledge/categories` | 分类列表 |
| GET | `/knowledge/article/page` | 文章分页（前台仅已发布） |
| GET | `/knowledge/article/{id}` | 文章详情 |
| POST / PUT / DELETE | `/knowledge/article[/{id}]` | 文章增改删（**管理员**） |
| PUT | `/knowledge/article/{id}/status` | 发布 / 下线（管理员） |

### 情绪日记 `/api/emotion-diary`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/emotion-diary` | 新增日记 |
| GET | `/emotion-diary/admin/page` | 管理端分页（关键词 / 日期筛选） |
| DELETE | `/emotion-diary/admin/{id}` | 管理端删除 |

### 数据统计 / 文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/data-analytics/overview` | 运营概览（聚合统计） |
| POST | `/file/upload` | 文件上传（multipart，10MB） |

## SSE 流式协议

`POST /api/psychological-chat/stream` 返回 `text/event-stream`，事件序列：

```
event: message   data: {"code":200,"data":{"content":"片段","type":"normal"}}
event: done      data: {}
```

- AI 回复以多个 `message` 事件增量推送，结束以 `done` 收尾；
- 未登录或会话校验失败时返回 `event: error`；
- AI 接口不可用时自动降级为本地兜底回复（`ai_model = local-fallback`），保证前端流程完整。

## AI / MCP 说明

- **对话**：`ChatClient`（bean `open-ai`）使用 OpenAI 兼容接口接入 DeepSeek，启用 tool-calling，并挂载 `MessageChatMemoryAdvisor`（30 条窗口记忆）。
- **MCP**：`KnowledgeMcpConfig` 通过 `MethodToolCallbackProvider` 将 `KnowledgeBaseService` 的 `@Tool` 方法注册为工具，模型可在对话中自主检索知识库。
- **RAG 上下文**：发送给模型前，`PsychologicalSupportService` 会基于用户消息调用 `buildKnowledgeContext` 注入最相关的知识摘要作为系统上下文，并要求模型优先引用、不编造来源。
- **数据实时性**：`KnowledgeBaseService` 以 MySQL `knowledge_article` 为唯一数据源并维护内存缓存；`ArticleService` 每次增删改 / 状态流转后调用 `invalidateCache()`，保证管理后台改动无需重启即可被 AI 检索到。

## 测试

```bash
mvn test
```

单元 / 集成测试使用 H2 内存库，不影响本地 MySQL 数据。

另提供人工回归脚本（需后端运行在 `localhost:1236`）：

```bash
python test-rag.py               # 真实 AI 对话，验证知识库参与回答
python test-rag-upload-loop.py   # 后台新增文章 -> AI 知识库立即检索到（缓存失效）
```

## License

本项目为内部学习与验证用途的演示项目，代码仅供学习参考。心理支持功能不构成医疗建议。
