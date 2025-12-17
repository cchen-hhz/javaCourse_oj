# 前端开发计划 (Frontend Development Plan)

## 1. 项目概述
本项目旨在为现有的 Online Judge 后端构建一个现代化的前端应用。
- **核心框架**: Astro + Vue 3
- **设计目标**: 结构清晰、高可维护性、良好的用户体验。

## 2. 技术栈选型
- **构建工具/框架**: Astro (作为静态/SSR 框架), Vue 3 (作为交互组件框架)
- **样式库**: Tailwind CSS (原子化 CSS), Element Plus (Vue 3 UI 组件库, 用于快速构建后台及表单)
- **状态管理**: Pinia (Vue 3 推荐的状态管理库)
- **网络请求**: Axios (处理 HTTP 请求)
- **Markdown 渲染**: markdown-it 或类似库 (用于题目描述渲染)
- **代码编辑器**: Monaco Editor (用于代码提交)

## 3. 项目结构设计
```
frontend/
├── public/              # 静态资源
├── src/
│   ├── components/      # Vue 组件 (功能性组件)
│   │   ├── common/      # 通用组件 (Header, Footer)
│   │   ├── problem/     # 题目相关组件
│   │   ├── submission/  # 提交记录相关组件
│   │   └── user/        # 用户相关组件
│   ├── layouts/         # Astro 布局文件
│   ├── pages/           # Astro 页面 (路由)
│   │   ├── index.astro          # 首页
│   │   ├── login.astro          # 登录页
│   │   ├── register.astro       # 注册页
│   │   ├── problems/
│   │   │   ├── index.astro      # 题目列表
│   │   │   └── [id].astro       # 题目详情
│   │   ├── submissions/
│   │   │   ├── index.astro      # 提交列表
│   │   │   └── [id].astro       # 提交详情
│   │   └── user/
│   │       └── [id].astro       # 用户主页
│   ├── stores/          # Pinia 状态管理 (UserStore, etc.)
│   ├── utils/           # 工具函数 (Axios 封装, 格式化等)
│   └── styles/          # 全局样式
├── astro.config.mjs     # Astro 配置
├── tailwind.config.mjs  # Tailwind 配置
└── package.json
```

## 4. API 集成与异常处理
后端 API 位于 `/api` 下。我们将创建一个 Axios 实例进行统一管理。

### 异常处理策略
针对 requirement.md 中提到的问题：
1.  **500 错误处理**:
    -   拦截器将捕获 500 错误。
    -   尝试解析错误响应体，如果包含具体错误信息则展示给用户。
    -   如果是未知服务器错误，展示通用的 "服务器内部错误" 提示。
2.  **401 未授权处理**:
    -   拦截器捕获 401 错误。
    -   自动重定向到 `/login` 页面。
    -   (可选) 保存当前尝试访问的 URL，登录后跳回。

## 5. 安全与登录管理 (SecurityConfig 修改计划)
根据要求，需要修改 `backend/security/src/main/java/com/edu/oj/config/SecurityConfig.java`。

**当前问题**: `SecurityConfig` 可能未正确配置未登录时的跳转行为，或者使用了默认的 403/401 响应但未引导用户。
**修改方案**:
-   确保 `AuthenticationEntryPoint` 在检测到未登录用户访问受保护资源 (`/api/users/me`, `/api/submissions` POST 等) 时，返回明确的 401 状态码。
-   前端 Axios 拦截器负责监听 401 并执行 `window.location.href = '/login'` 跳转。
-   *注*: 如果后端需要强制 HTTP 重定向 (302)，我们可以配置 `LoginUrlAuthenticationEntryPoint`，但对于前后端分离架构，前端控制跳转更为标准。

## 6. 页面功能规划

### 6.1 公共部分
-   **导航栏**: Logo, 题目列表, 提交状态, (登录/注册 或 用户头像/下拉菜单).
-   **Footer**: 版权信息.

### 6.2 首页 (Home)
-   展示欢迎信息。
-   可能的公告或热门题目。

### 6.3 用户认证 (Auth)
-   **登录**: 表单 (用户名/邮箱, 密码), 调用 `/api/log/login`.
-   **注册**: 表单 (用户名, 密码, 邮箱), 调用 `/api/log/register`.

### 6.4 题目模块 (Problems)
-   **列表页**: 表格展示题目 ID, 标题, 通过率等。支持分页 (`pageSize`, `pageNum`).
-   **详情页**:
    -   展示题目描述 (Markdown).
    -   文件下载链接 (`/api/problems/{id}/file/{name}`).
    -   **代码提交区域**: Monaco Editor, 语言选择, 提交按钮 (调用 `/api/submissions`).

### 6.5 提交记录模块 (Submissions)
-   **列表页**: 展示提交 ID, 用户, 题目, 结果, 时间。支持筛选.
-   **详情页**:
    -   展示评测结果 (AC, WA, TLE 等).
    -   展示代码 (仅本人或 Admin 可见).
    -   展示详细评测信息 (`SubmissionConfig`).

### 6.6 用户模块 (User)
-   **个人主页**: 展示用户信息, 已解决题目等。
-   **设置**: 修改资料 (如果 API 支持).

### 6.7 管理员功能 (Admin) - *视权限而定*
-   题目管理 (增删改, 上传测试数据).
-   用户管理 (封禁/解封).

## 7. 下一步行动
1.  初始化 Astro 项目。
2.  安装 Vue, Tailwind, Axios, Pinia, Element Plus。
3.  配置 Axios 拦截器。
4.  开始编写 Layout 和 基础页面。
