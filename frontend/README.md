# frontend · 心理健康助手前端应用

> Vue 3 + Vite 构建的前后端分离前端，面向用户的**前台**（AI 咨询 / 情绪日记 / 知识库）与面向运营的**后台**（数据分析 / 知识管理 / 咨询记录 / 情绪日志）双端一体。

![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?logo=vite&logoColor=white)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.13-409EFF)
![Pinia](https://img.shields.io/badge/Pinia-3-FFD859)
![ECharts](https://img.shields.io/badge/ECharts-6-AA344D)

---

## 功能特性

**前台（用户端）**

- 首页 / 登录注册
- **AI 心理咨询**：发起会话、SSE 流式对话（`@microsoft/fetch-event-source`）、历史会话回看与情绪分析
- **情绪日记**：记录当日情绪评分、主导情绪、诱因、压力等级
- **心理健康知识库**：分类检索、文章详情（Markdown 渲染、安全转义）
- **心理资源与预约（P2）**：校内 / 热线 / 本地资源浏览，在线提交预约申请、查看处理状态
- **主题化成长计划（P2）**：专业人员审核的阶段性计划，浏览、记录并跟踪完成进度
- **语音输入（P2）**：对话页支持浏览器原生语音输入（Chrome / Edge）

**后台（管理端 `/back`）**

- 数据分析看板：运营概览，ECharts 可视化
- 知识文章管理：分类树、分页检索、富文本编辑（wangeditor）、封面图上传、发布 / 下线
- 咨询记录：全量会话与消息查看
- 情绪日志：全量日记查看与删除
- 校园报告（P2）：匿名聚合报告，ECharts 可视化
- 预约管理（P2）：预约处理 + 心理资源管理
- 成长计划管理（P2）：计划发布 / 下线 / 编辑 / 删除

## 技术栈

| 类别 | 依赖 |
| --- | --- |
| 框架 | Vue 3.5 · Vue Router 4 · Pinia 3 |
| 构建 | Vite 7 · `@vitejs/plugin-vue` |
| UI | Element Plus 2.13 · `@element-plus/icons-vue` |
| 数据请求 | Axios（统一拦截器：token 注入、业务码处理、401/403 跳登录） |
| 流式 | `@microsoft/fetch-event-source`（SSE 对话） |
| 可视化 / 编辑 | ECharts 6 · `@wangeditor/editor` 5 |
| 样式 | Sass |

## 环境要求

- Node.js 18+（推荐 20/24）
- 后端服务已启动（默认 `http://localhost:1236`）

## 快速开始

```bash
# 1. 安装依赖
npm install

# 2. 启动开发服务（端口 5173）
npm run dev

# 3. 生产构建
npm run build
npm run preview
```

打开 <http://localhost:5173>。

## 代理与配置

`vite.config.js` 中开发服务器将 `/api` 代理到后端，可通过环境变量覆盖：

```bash
# PowerShell
$env:VITE_API_TARGET='http://localhost:1236'
npm run dev
```

- 后端地址常量：`src/config/index.js`（`fileBaseUrl`，用于拼接上传文件 URL）
- 请求封装：`src/utils/request.js`（baseURL `/api`，超时 15s，token 从 localStorage 读取并放入 `token` 请求头）

## 项目结构

```
frontend/
├── index.html
├── vite.config.js              # 开发代理 / 构建分包配置
├── src/
│   ├── main.js                 # 应用入口
│   ├── App.vue
│   ├── router/index.js         # 路由（后台 /auth / 前台 /）与登录守卫
│   ├── stores/admin.js         # Pinia 状态
│   ├── api/                    # 接口封装
│   │   ├── frontend.js         # 前台接口（会话 / 日记 / 知识库）
│   │   └── admin.js            # 后台接口（登录 / 文章 / 会话 / 日记 / 统计）
│   ├── components/             # 布局与公共组件（AuthLayout / BackendLayout / FrontendLayout / 富文本 / Markdown 渲染 等）
│   ├── views/                  # 页面
│   │   ├── 前台：home / consultation / emotionDiary / frontendKnowledge / articleDetail / counseling / growthPlans / login / register
│   │   └── 后台：dashboard / knowledge / consultations / emotional / report / appointments / growthPlansAdmin
│   ├── utils/                  # axios 实例与工具
│   └── assets/                 # 静态资源
```

## 路由

| 路径 | 说明 | 权限 |
| --- | --- | --- |
| `/` | 首页 | 公开 |
| `/consultation` | AI 心理咨询 | 登录 |
| `/emotion-diary` | 情绪日记 | 登录 |
| `/knowledge` | 心理健康知识库 | 公开 |
| `/knowledge/article/:id` | 文章详情 | 公开 |
| `/auth/login` · `/auth/register` | 登录 / 注册 | 公开 |
| `/back/dashboard` | 数据分析 | 管理员 |
| `/back/knowledge` | 知识文章管理 | 管理员 |
| `/back/consultations` | 咨询记录 | 管理员 |
| `/back/emotional` | 情绪日志 | 管理员 |
| `/counseling` | 心理资源与预约 | 公开 / 提交需登录 |
| `/growth-plans` | 成长计划 | 公开 / 记进度需登录 |
| `/back/report` | 校园报告 | 管理员 |
| `/back/appointments` | 预约管理 | 管理员 |
| `/back/growth-plans` | 成长计划管理 | 管理员 |

> 路由守卫根据 `localStorage` 中的 token 与 `userType` 控制访问：`userType=2` 管理员进入后台，`userType=1` 普通用户仅前台，未登录访问后台跳转登录页。

## 构建优化

`vite.config.js` 通过 `manualChunks` 将 `vue-vendor` / `element-vendor` / `echarts-vendor` / `editor-vendor` 分包，降低首屏加载体积、利于浏览器缓存。

## License

本项目为内部学习与验证用途的演示项目，代码仅供学习参考。
