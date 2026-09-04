# AI 心理健康助手 · 2.0

> 基于 **Spring AI + MCP** 的 AI 心理支持与自助管理平台（前后端分离架构）。
> 面向 18 岁及以上大学生的情绪倾诉、AI 心理对话、情绪日记与心理健康知识库。

> ⚠️ **重要声明**：本产品提供 AI 心理支持与自助管理工具，**不提供诊断、治疗、用药建议或紧急救援**。如处于立即危险，请拨打 120 / 110 / 12356 或联系可信任的人。

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-6DB33F?logo=spring&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.8-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?logo=vite&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)

---

## 版本说明

| 版本 | 分支 | 说明 |
| --- | --- | --- |
| **V2.0（当前主干）** | `main` | **前后端独立工程**（`backend` + `frontend`），引入 **Spring AI ChatClient + MCP（模型上下文协议）**，AI 对话可实时检索心理健康知识库，并新增后台数据分析 / 情绪日志管理等能力 |
| V1.x（MVP，已归档） | `v1` | 单体结构（`server` / `web` 两个目录），含 P0 安全 MVP 与 P1 连续使用增强；代码与历史已完整保留在 `v1` 分支 |

## 仓库结构

```
├── backend/        # 后端服务（Spring Boot 3.5 + Spring AI + MyBatis-Plus）
│   ├── src/main/java/   # Java 源码（controller / service / mapper / config / AiService）
│   ├── src/main/resources/  # 配置与知识库资源
│   └── sql/            # 建表脚本（schema.sql）与演示种子数据（seed_demo_data.sql）
├── frontend/              # 前端应用（Vue 3 + Vite + Element Plus + Pinia）
│   └── src/             # 页面 / 组件 / 路由 / 状态 / API 封装
└── docs/                # 产品设计与技术文档（含 V1 历史归档）
```

## 功能特性

- **用户认证**：注册 / 登录 / 登出 / 当前用户，JWT 无状态鉴权，普通用户与管理员双角色。
- **AI 心理对话**：基于 Spring AI ChatClient（DeepSeek 兼容接口）+ 会话窗口记忆（30 条），支持 **SSE 流式输出**；知识库缺失或接口异常时自动降级为本地兜底回复。
- **知识库检索增强（RAG）**：知识库通过 `@Tool` 暴露为 **MCP 工具**（搜索 / 详情 / 分类），AI 在对话中自主调用检索，回答优先引用已发布文章。
- **情绪日记**：记录情绪评分、主导情绪、诱因与压力等级，支持管理端分页查看与删除。
- **知识文章管理**：分类 + 文章分页检索、富文本编辑（wangeditor）、封面图上传、发布 / 下线状态流转。
- **数据分析**：运营概览聚合（用户 / 会话 / 消息 / 情绪分布），前端 ECharts 可视化。
- **会话管理**：会话列表（用户看自己、管理员看全部）、消息回看、删除、会话级情绪分析。

### P2 扩展（v2.0 已实现）

- **学校心理中心预约 / 转介**：可配置的心理资源库（校内 / 热线 / 本地），学生可提交预约申请，管理端处理确认 / 完成 / 取消流转。
- **匿名聚合校园心理报告**：基于全站匿名聚合数据生成校园心理健康报告（情绪分布、评分区间、近 7 天趋势、低情绪占比），不含任何个人身份信息。
- **主题化成长计划**：专业人员审核的阶段性自助计划（情绪 / 压力 / 睡眠 / 人际），学生可浏览、记录并跟踪完成进度。
- **语音输入**：AI 对话页支持浏览器原生语音输入（Web Speech API，Chrome / Edge）。
- **无障碍与多端适配**：跳转链接、ARIA 导航标注、键盘可达，导航与关键页面响应式适配手机端。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.5.15 · Java 17 · Spring Security + JWT（auth0 java-jwt） |
| AI | Spring AI 1.1.8（OpenAI 兼容接入 DeepSeek）· ChatClient · MCP Server（STREAMABLE） |
| ORM | MyBatis-Plus 3.5.7 · MySQL 8.0（`mental_health_assistant`） |
| 流式 | Reactor `Flux<ServerSentEvent>`（SSE：`message` → `done` / `error`） |
| 前端 | Vue 3.5 · Vite 7 · Element Plus 2.13 · Pinia · Vue Router 4 · ECharts 6 · wangeditor 5 |
| 工具 | Lombok · Hutool · spring-boot-starter-validation |

## 快速开始（概览）

```bash
# 1. 初始化数据库
mysql -uroot -p < backend/sql/schema.sql

# 2. 启动后端（端口 1236）
cd backend
mvn spring-boot:run

# 3. 启动前端（端口 5173，已代理 /api → localhost:1236）
cd frontend
npm install
npm run dev
```

> 种子数据（`admin` / `demo` 账号，密码 `123456`，知识分类与文章）由后端启动时自动写入。

### 演示数据（可选）

导入演示数据可立即获得完整的业务展示效果（成长计划 / 预约管理 / 校园报告均有数据可看）：

```bash
mysql -uroot -p123456 --default-character-set=utf8mb4 < backend/sql/seed_demo_data.sql
```

| 数据类别 | 数量 | 说明 |
| --- | --- | --- |
| 成长计划 | 10 条 | 覆盖情绪 / 正念 / 睡眠 / 自我成长 / 学习效能 / 人际 / 作息 / 自尊自信 / 压力 / 积极心理，已发布 |
| 预约管理 | 10 条 | 面向学校心理中心与 12356 热线，覆盖待处理 / 已确认 / 已取消 / 已完成四种状态 |
| 校园报告数据 | 5 用户 + 10 日记 | 新增 5 个学生账号（密码 `123456`）与近 7 天情绪日记，驱动匿名校园报告聚合展示 |

> 演示数据幂等性说明：重复执行会因 `user` 表唯一键冲突而中止，删除相应记录后即可重新导入；`growth_plan` 无唯一约束，重复执行前请先清空对应记录。

### 已知问题与修复记录

| 日期 | 问题 | 根因 | 修复 |
| --- | --- | --- | --- |
| 2026-09-04 | P2 成长计划 / 预约管理 / 校园报告 / 心理资源页面添加数据后列表不刷新、始终显示 No Data | axios 响应拦截器已返回业务数据（`data.data`），页面代码误用 `res.data?.records` 取数导致始终取到 `undefined` | 前端 5 个页面统一移除多余的 `.data` 层（`growthPlansAdmin.vue` / `growthPlans.vue` / `appointments.vue` / `counseling.vue` / `report.vue`） |
| 2026-09-04 | 成长计划详情正文 Markdown 标题/列表显示为原始 `##`、`\n` 文本 | 种子数据生成脚本将换行符转义成字面 `\\n` 入库，前端依赖真实换行识别标题与列表 | ① 已入库数据将字面 `\n` 替换为真实换行符；② 修正 `gen_seed_data.py` 转义并重新生成 `seed_demo_data.sql`；③ 优化 `growthPlans.vue` 的 `renderContent`（列表合并为 `<ul>`、去除标题后多余换行） |

> 取数约定：`@/utils/request.js` 响应拦截器对 `code === 200` 直接返回业务数据 `data.data`，页面调用 `service.get/post` 后应直接消费返回对象，**不要**再访问 `.data`。

## 文档

- [后端服务文档（backend）](backend/README.md)
- [前端应用文档（frontend）](frontend/README.md)
- [产品设计文档](docs/产品设计文档.md)
- [技术方案](docs/技术方案.md)
- [**项目进度报告（V2.0 现状核查：功能 / 45 个接口 / 风险清单）**](docs/项目进度报告.html)
- [实施进度看板](docs/实施进度看板.html)

## License

本项目为内部学习与验证用途的演示项目，代码仅供学习参考。心理支持功能不构成医疗建议。
