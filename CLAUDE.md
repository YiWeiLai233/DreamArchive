# DreamArchive

Dream Archive - Spring Boot 3.5.7 + MyBatis + MySQL，Java 17。Vue 3 + TypeScript 前端。

## 项目结构

```
src/main/java/com/yiweilai/DreamArchive/
├── controller/    # Controller (LoginController, RegisterController, ChangePasswordController,
│                  #   ResetPasswordController, AdminController, DreamController, StatsController,
│                  #   UserController, AiPoolController)
├── service/       # Service (LoginService, RegisterService, AdminService, AiService,
│                  #   AiProviderPool, DreamService, DreamAiTaskService, StatsService, TokenService,
│                  #   ResetPasswordService, VerificationCodeService)
├── mapper/        # Mapper (LoginMapper, RegisterMapper, AdminMapper, DreamContentMapper,
│                  #   DreamStatsMapper, StatsMapper, ResetPasswordMapper, DreamMapper)
├── DTO/           # DTO (User, LoginResponse, DreamContent, DreamStats, AdminOverview, AiProvider 等)
├── config/        # Config (SecurityConfig, TokenAuthenticationFilter, SecurityPaths,
│                  #   WebConfig, AsyncConfig, GlobalExceptionHandler)
└── util/          # Util (Result, PasswordEncrypt, CaptchaGenerator)
src/main/resources/
├── mapper/        # MyBatis XML
├── db/            # admin_role_patch.sql, admin_user_status_patch.sql
└── application.properties
frontend/             # Vue 3 + TS + Vite 前端，12 个 View 组件
```

## 核心表结构

| 表 | 用途 | 主键 |
|---|---|---|
| `user` | 用户表 (id, username, password, email, role, status, deleted, avatar_url, created_at) | INT 自增 |
| `dream_content` | 梦境主表 (id, user_id, title, content, emotion, place, time, interpretation, created_at) | UUID varchar(36) |
| `dream_stats` | 每日情绪统计 (user_id, stat_date, 各情绪计数) | INT 自增，uk_user_date 唯一 |
| `dream_place_stats` | 地点统计 (user_id, place, dream_count) | INT 自增，uk_user_place 唯一 |

**关键规则**: 所有新梦境数据写入 `dream_content`，统计 SQL 查 `dream_content` 或预计算表。

## 认证与安全

- **Spring Security**: `SecurityConfig` 过滤链，CSRF 关闭、无状态 session、全局 CORS
- **Token**: `TokenService` HMAC-SHA256 签名，前端存 localStorage，axios 拦截器自动附带
- **TokenAuthenticationFilter**: 验 token + 查数据库确认用户未删除/未封禁
- **SecurityPaths**: 公开接口白名单 (login, register, reset-password, hello, swagger)
- **密码**: `PasswordEncrypt` SHA-256+盐值
- **管理员**: `/api/admin/**` 要求 `ROLE_ADMIN` 或 `ROLE_SUPER_ADMIN`
- **前端守卫**: guestOnly(/login,/register,/reset-password)、requiresAuth、requiresAdmin(/admin)
- **Session**: 前端 15 分钟超时
- **软删除**: user.deleted=1，封禁: user.status='BANNED'

## 验证码+邮件系统

- **VerificationCodeService**: 支持多场景 (register/login/change-password/reset-password)
- Redis 存储验证码 (5分钟 TTL) + 频率限制 (60秒)
- QQ邮箱 SMTP: `spring.mail.host=smtp.qq.com`，端口 587
- 邮件发送失败返回用户友好提示，不泄露 SMTP 技术错误

## 接口清单

### 公开接口 (无需认证)

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/login` | 密码登录 |
| POST | `/api/login/send-code` | 登录验证码 |
| POST | `/api/login/code` | 验证码登录（不存在则自动注册） |
| POST | `/api/register` | 注册（需验证码） |
| POST | `/api/register/send-code` | 注册验证码 |
| POST | `/api/reset-password/send-code` | 重置密码验证码 |
| POST | `/api/reset-password` | 重置密码 |
| GET | `/api/hello` | 健康检查 |

### 需认证接口

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/account/setup` | 新用户设置用户名密码 |
| POST | `/api/change-password/send-code` | 修改密码验证码 |
| POST | `/api/change-password` | 修改密码（需验证码） |
| GET | `/api/dream/{id}` | 查单个梦境 |
| GET | `/api/dreams/user/{userId}` | 查用户所有梦境 |
| POST | `/api/analysisDream` | 保存梦境 |
| POST | `/api/dreams/save-and-analyze` | 保存+异步AI解析 |
| POST | `/api/dream/{id}/delete` | 删除梦境 |
| POST | `/api/upload/image` | 上传图片到 MinIO |
| GET | `/api/stats/{userId}/*` | 统计接口 |
| GET | `/api/user/by-email` | 邮箱查用户 |

### 管理员接口 (需 ROLE_ADMIN 或 ROLE_SUPER_ADMIN)

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/admin/overview` | 管理总览 |
| POST | `/api/admin/user-action` | 用户操作 |
| POST | `/api/admin/delete-user` | 软删除 |
| POST | `/api/admin/dream-detail` | 梦境详情 |
| GET | `/api/admin/ai-pool/providers` | 查看所有 AI provider 状态 |
| POST | `/api/admin/ai-pool/providers` | 添加 provider |
| POST | `/api/admin/ai-pool/providers/{name}/update` | 更新 provider |
| POST | `/api/admin/ai-pool/providers/{name}/delete` | 删除 provider |
| POST | `/api/admin/ai-pool/providers/{name}/reset` | 重置熔断 |

## 前端 API 模块

| 文件 | 用途 |
|---|---|
| `api/axios.ts` | 共享 axios 实例，带 `attachAuthHeader` 拦截器 |
| `api/user.ts` | 注册、登录(密码/验证码)、统计、验证码发送、setupAccount |
| `api/dream.ts` | 梦境 CRUD、AI 解析 |
| `api/admin.ts` | 管理员后台 |

## 前端页面

| 页面 | 路径 | 说明 |
|------|------|------|
| HomeView | `/` | 首页/仪表板 |
| LoginView | `/login` | 密码登录/验证码登录切换，新用户自动注册+设置弹窗 |
| RegisterView | `/register` | 注册（验证码强制校验，注册后自动登录） |
| ResetPasswordView | `/reset-password` | 多步密码重置 |
| RecordDreamView | `/record-dream` | 记录梦境+AI解析，PC左右分栏/手机向导式 |
| DreamListView | `/dreams` | 梦境列表+搜索+详情 |
| ProfileView | `/profile` | 个人资料+头像 |
| ChangePasswordView | `/change-password` | 修改密码（需验证码） |
| DreamStatsView | `/dream-stats` | 统计图表 |
| AdminView | `/admin` | 管理员控制台 |
| ErrorView | `/error/:code` | 错误页 |
| LearnMoreView | `/learn-more` | 产品介绍 |

## 前端设计风格

项目有两套视觉风格，按页面类型选用：

### 主功能页面：梦幻玻璃拟态 (Dreamy Glassmorphism)

适用于：HomeView、LoginView、RegisterView、RecordDreamView、DreamListView、ProfileView、ChangePasswordView、ResetPasswordView、DreamStatsView、LearnMoreView、ErrorView。

**色彩**: 主色 `#7C6FE0` (紫) + 辅色 `#FFB347` (橙)，渐变背景 `#E8D5F5` → `#B8E6FF`，文字深紫 `#2D2B55` / 浅紫 `#6B6899`。CSS 变量在 `assets/main.css` `:root` 中定义。

**视觉**: 毛玻璃卡片 (`rgba(255,255,255,0.25)` + `backdrop-filter: blur(20-24px)`)、梦幻背景层 (星星/云朵/光晕)、浮动 emoji 装饰 (🌙✨⭐💫🔮)、渐变文字 (`-webkit-background-clip: text`)。

**交互**: 胶囊按钮 (radius 50px)、hover 上浮 + 紫色投影、入场动画 `card-enter` (下方淡入缩放)、submit 按钮光泽扫过效果。

**字体**: Noto Sans SC，PC 端 `font-size: 18px`。

**响应式**: ≤768px / ≥1024px / ≥1440px 三档，侧边装饰仅 ≥1024px 显示。

**规范**: 新主功能页面必须复用此风格，参考 `HomeView.vue` 和 `LoginView.vue`。

### 管理员后台：简洁 Dashboard 风格

适用于：AdminView。

**色彩**: 浅灰白背景 (`#f5f7fb`)、indigo 主色 (`#4f46e5` → `#7c3aed`)、白色卡片 + 细边框。

**视觉**: 无毛玻璃、无浮动装饰、无梦幻背景。白色实底卡片 + 微投影、指标卡左侧色条、表格布局。

**交互**: 圆角按钮 (radius 10-12px)、hover 微上浮、modal 弹窗带 hero 渐变头部。

**规范**: 管理类页面用此风格，参考 `AdminView.vue`。

## 前端响应式布局策略

RecordDreamView 采用 JS 切换模板的方式实现响应式：

- **PC (>= 1024px)**：左右分栏，左侧边栏（情绪/时间/地点）+ 右侧主内容区
- **手机 (< 1024px)**：向导式两步卡片，步骤指示器 + 上一步/下一步导航
- 实现：`isMobile = ref(window.innerWidth < 1024)` + resize 监听 + `v-if`/`v-else` 切换
- CSS 断点：`@media (max-width: 1023px)` 覆盖手机端样式

## MinIO 对象存储

- `service/MinioService` — 上传、预签名 URL（7天有效）、删除
- `controller/FileController` — `POST /api/upload/image` 图片上传接口
- 配置: `minio.endpoint/access-key/secret-key/bucket`
- Bucket: `dream-archive`
- 数据库存储 MinIO 对象路径（objectName），读取时动态生成预签名 URL
- `dream_content.image_url` — 梦境图片对象路径
- `user.avatar_url` — 用户头像对象路径
- 头像在登录时通过 `LoginResponse.avatarUrl` 返回预签名 URL
- 梦境图片在查询时通过 `DreamController.enrichImageUrl()` 动态生成

## HTTP 方法规范

项目只使用 GET 和 POST，不使用 PUT/DELETE/PATCH。操作类接口用 POST，路径加动词后缀（如 `/dream/{id}/delete`、`/providers/{name}/update`）。

## AI 资源池化

详见 `docs/AI-POOL.md`。

- `DTO/AiProvider` — provider 模型
- `service/AiProviderPool` — 加权轮询 + 延迟感知 + 熔断降级
- `controller/AiPoolController` — 管理员 CRUD 接口
- 配置格式: `ai.pool.providers[0].name/url/apiKey/model/weight/enabled`
- 多模态配置: `ai.pool.vision-provider` 指定图片分析专用 provider

## 开发命令

```bash
# 前端
cd frontend && npm run build

# 后端（需 JDK 17）
mvn -DskipTests compile
```

## 服务器

- 数据库: MySQL 192.168.199.136:3306
- Redis: 192.168.199.136:6379
- 邮件: QQ邮箱 859399899@qq.com

---

## 本地协作约定

- 本机 Java 17 路径：`C:\Program Files\Java\jdk-17.0.4.1`
- 上传 GitHub 时不要加入所有 `.md` 文档文件。
- 上传 GitHub 时不要加入配置文件信息、密钥、账号、服务器连接等敏感信息。
- 以后写入本地 `AGENTS.md` 的协作说明，也同步追加到 `CLAUDE.md`。
- 同步到 `CLAUDE.md` 时只添加新内容，不删除已有内容。
- git commit 不要加 `Co-Authored-By: Claude` 签名。
- 没有叫你推送不允许 git push。
- 用户说"写入记忆"时，同步写入 memory、CLAUDE.md、笔记、AGENTS.md 四处。
- 没有用户允许不准删除任何文件。

## 变更记录

### 2026-05-23 部署修复 + 前端优化

| 文件 | 改动 |
|------|------|
| `SecurityConfig.java` | CORS 白名单加 `http://175.178.84.239:*`，修复新服务器部署 403 |
| `WebConfig.java` | CORS 白名单加 `http://175.178.84.239:*` |
| `RecordDreamView.vue` | 时间选择器从原生 select 改为标签式，显示具体时间段（00-02、08-10 等）；手机端情绪按钮 flex:1 加宽 |
| `DreamListView.vue` | AI 解析序号显示改为自动递增 |
| `dreamInterpretation.ts` | AI 解析序号逻辑：忽略 AI 原始序号，改为 itemCounter 自动递增 1,2,3,4,5 |

### 2026-05-26 死代码清理 + MinIO 图片上传

**死代码清理**（已删除 11 个文件）:

| 文件 | 原因 |
|------|------|
| `DTO/Dream.java` + `mapper/DreamMapper.java` + `mapper/dreamMapper.xml` | 旧 dream 表，已被 DreamContent 替代 |
| `DTO/AI.java` + `DTO/DreamRequest.java` | 无引用 |
| `controller/TestDataController.java` + `controller/InitController.java` | 无认证端点，安全隐患 |
| `config/Ai.java` | 空类 |
| `service/EmailService.java` + `service/CaptchaService.java` + `util/CaptchaGenerator.java` | 桩代码 |
| `AiService.aiSerice()` 方法 | 拼写错误的死方法 |

**MinIO 图片上传**:

| 文件 | 改动 |
|------|------|
| `pom.xml` | 添加 `io.minio:minio:8.5.12` 依赖 |
| `application.properties` | 添加 MinIO 配置 (endpoint/access-key/secret-key/bucket) |
| `service/MinioService.java` | 新建：上传、预签名 URL、删除 |
| `controller/FileController.java` | 新建：`POST /api/upload/image` |
| `DTO/DreamContent.java` | 新增 `imageUrl` 字段 |
| `dreamContentMapper.xml` | INSERT/SELECT 加 `image_url` 列 |
| `DreamController.java` | saveDreamInternal 提取 imageUrl |
| `DreamService.java` | saveDream 加 imageUrl 参数 |
| `frontend/src/api/dream.ts` | 新增 `uploadImage()`、DreamForm/DreamContent 加 imageUrl |
| `frontend/src/views/RecordDreamView.vue` | PC 侧边栏 + 手机步骤 1 加图片上传区域 |
| `frontend/src/views/DreamListView.vue` | 卡片缩略图 + 详情弹窗完整图片 |

**头像迁移到 MinIO**:

| 文件 | 改动 |
|------|------|
| `DTO/User.java` | 新增 `avatarUrl` 字段 |
| `DTO/LoginResponse.java` | 新增 `avatarUrl` 字段，登录时返回 |
| `loginMapper.xml` | SELECT 加 `avatar_url`，新增 `updateAvatarUrl` |
| `LoginMapper.java` | 新增 `updateAvatarUrl` 方法 |
| `LoginService.java` | 登录时生成头像预签名 URL |
| `UserController.java` | 新增 `POST /api/user/avatar`，查询用户时返回头像 URL |
| `frontend/src/api/user.ts` | 新增 `updateAvatarUrl()`，UserInfo 加 `avatarUrl` |
| `frontend/src/views/ProfileView.vue` | 头像上传改为 MinIO + 保存 URL 到数据库 |
| `frontend/src/views/LoginView.vue` | 登录后存储 avatarUrl |
| `MinioService.java` | 新增 `extractObjectName()` 兼容旧数据（完整 URL → objectName） |
| 数据库清理 | `SUBSTRING_INDEX` 提取 objectName，去掉预签名 URL 前缀和参数 |
| `DTO/Message.java` | `content` 从 `String` 改为 `Object`，支持多模态（文字+图片） |
| `AiService.java` | 新增 `analyzeDream(content, imageUrl)`，有图时用 vision API 格式 |
| `DreamAiTaskService.java` | `completeDreamAiFields` 加 `imageUrl` 参数 |
| `DreamController.java` | 保存分析和重新解析都传递 imageUrl |
| `FileController.java` | 文件类型三层校验：Spring 限制 + 白名单 + 魔数校验 |

**超级管理员**:

| 文件 | 改动 |
|------|------|
| `RegisterMapper.java` | 新增 `countUsers()` |
| `RegisterService.java` | 第一个注册用户自动设为 `SUPER_ADMIN` |
| `LoginService.java` | 验证码自动注册也检查第一个用户 |
| `AdminService.java` | `requireAdmin()` 接受 SUPER_ADMIN；`isSuperAdmin()` 保护超级管理员不被编辑/删除/封禁 |
| `SecurityConfig.java` | `hasAnyRole("ADMIN", "SUPER_ADMIN")` |
| `frontend/src/api/admin.ts` | 角色类型加 `'SUPER_ADMIN'` |
| `frontend/src/router/index.ts` | 路由守卫允许 SUPER_ADMIN |
| `frontend/src/stores/index.ts` | `isAdmin` 包含 SUPER_ADMIN |
| `frontend/src/views/AdminView.vue` | 金色徽章、按钮禁用、角色锁定、ghost-btn CSS |

### 2026-05-27 AI 多模态优化

| 文件 | 改动 |
|------|------|
| `application.properties` | 新增 `ai.pool.vision-provider=mimo2`，指定图片分析专用 provider |
| `AiProviderPool.java` | 新增 `visionProvider` 字段 + `acquire(String providerName)` 按名字获取 provider |
| `MinioService.java` | 新增 `getImageAsBase64(objectName)`，下载图片转 base64 data URL |
| `AiService.java` | 图片分析改用 base64 替代预签名 URL；新增 emotion/place/time 参数拼入用户消息；`max_tokens` 从 600 提到 2000 |
| `DreamAiTaskService.java` | 新增 8 参数重载，透传 emotion/place/time |
| `DreamController.java` | 保存分析和重新解析都传入 emotion/place/time |

### 2026-05-28 深夜模式适配

| 文件 | 改动 |
|------|------|
| `frontend/src/composables/useTheme.ts` | 新建：主题 composable，light ↔ dark 两态切换，首次跟随系统，localStorage 持久化 |
| `frontend/src/assets/main.css` | 新增 `:root.dark` CSS 变量覆盖（背景/文字/毛玻璃/滚动条/选区） |
| `frontend/src/App.vue` | 调用 `useTheme()` 初始化主题 |
| `frontend/src/views/HomeView.vue` | 导航栏加主题切换按钮（44px + 紫色光环）+ 深色覆盖 |
| `frontend/src/views/LoginView.vue` | 深色覆盖（星星/云朵/毛玻璃/输入框/消息提示） |
| `frontend/src/views/RegisterView.vue` | 深色覆盖 |
| `frontend/src/views/RecordDreamView.vue` | 深色覆盖（表单/情绪按钮/时间标签/AI 解析结果） |
| `frontend/src/views/DreamListView.vue` | 深色覆盖（卡片/筛选/详情弹窗/确认弹窗） |
| `frontend/src/views/ProfileView.vue` | 深色覆盖（资料卡/统计卡/表单/只读字段） |
| `frontend/src/views/ChangePasswordView.vue` | 深色覆盖 |
| `frontend/src/views/ResetPasswordView.vue` | 深色覆盖（步骤指示器/表单） |
| `frontend/src/views/DreamStatsView.vue` | 深色覆盖（概览卡/情绪分布/周统计/地点统计） |
| `frontend/src/views/LearnMoreView.vue` | 深色覆盖（特性卡/步骤/技术标签） |
| `frontend/src/views/ErrorView.vue` | 深色覆盖 |
| `frontend/src/views/AdminView.vue` | 深色覆盖（顶栏/侧边栏/指标卡/表格/badge/弹窗），修正 20+ 选择器名不匹配 |

**深夜模式设计**：
- 触发方式：首次跟随系统 `prefers-color-scheme`，手动切换后固定（light ↔ dark）
- 切换按钮：HomeView 导航栏 🌙/☀️ 图标，44px 紫色光环
- 色彩体系：深紫蓝夜空背景（`#0F0C29` → `#1A1145`），暗色毛玻璃（`rgba(20,17,38,0.7)`），淡紫边框（`rgba(155,143,255,0.28)`）
- 管理后台：独立深色主题（`#0E0E14` 背景 + `#1A1A26` 卡片 + `#32324A` 边框）

### 2026-05-28 本地协作记忆

- dark-mode 对比度、全局主题切换、记录页移动端和管理员标签修复，目标推送分支为 `codex-dark-mode-contrast`。
- 上传 GitHub 时仍不要提交 `.md` 文档、配置文件、密钥、账号或服务器连接信息。
- 以后提交信息使用规范 commit 格式，标题说明写中文，例如 `fix(frontend): 修复深色模式卡片对比度`、`feat(theme): 新增全局主题切换按钮`。
- 以后用户说“写入记忆”时，除同步到 `AGENTS.md` 和 `CLAUDE.md` 外，也要同步追加到 `D:\desktop\dreamAc` 的当日开发笔记一份；如果当前权限不允许写入 D 盘笔记，要明确说明并给出需要追加的内容。
- 以后 commit 不要带任何 Codex/Claude/AI 签名或协作者尾注，例如不要添加 `Co-authored-by: Codex <noreply@openai.com>`、`Co-Authored-By: Claude`、`Generated by Codex`。

### 2026-06-02 部署文档 + 配置模板

| 文件 | 改动 |
|------|------|
| `docs/DEPLOYMENT.md` | 新建：完整部署文档（环境要求、MySQL/Redis/MinIO 安装、后端构建、前端构建、Nginx 反代、HTTPS、运维监控、常见问题） |
| `src/main/resources/application.properties.example` | 新建：配置模板，敏感信息替换为占位符，供上传 GitHub 使用 |

### 2026-06-02 安全响应头 + HttpOnly Cookie 分支

| 文件 | 改动 |
|------|------|
| `SecurityConfig.java` | 添加 CSP（script-src 'self'）、X-Frame-Options、X-Content-Type-Options、Referrer-Policy 安全响应头 |
| `feat/httponly-cookie` 分支 | 已创建并推送，待开发 Token 从 localStorage 迁移到 HttpOnly Cookie |
| `RecordDreamView.vue` | AI 解析免责声明升级（两行详细版），保存并解析按钮上方加提醒 |
| `DreamListView.vue` | AI 解析免责声明同步升级 |

### 2026-06-02 笔记同步约定

- 用户以后说“写入笔记”时，必须同步写入三个位置：本项目 `AGENTS.md`、本项目 `CLAUDE.md`、桌面 `DreamAc笔记.md`。
- 桌面 DreamAc 笔记路径：`C:\Users\85939\Desktop\DreamAc笔记.md`。

### 2026-06-02 CORS 配置集中化

- CORS 只保留在 `SecurityConfig.java` 的 `CorsConfigurationSource` 中，由 Spring Security `.cors(Customizer.withDefaults())` 读取；删除重复的 `WebConfig.java`，避免两处白名单改漏。
- 新增 `config/CorsProperties.java`，所有 CORS 值从 `application.properties` 的 `app.cors.*` 读取；Java 代码不要硬编码 `localhost`、`127.0.0.1` 或服务器 IP。
- `application.properties` 被 `.gitignore` 排除，部署服务器时必须同步补 `app.cors.allowed-origin-patterns`、`app.cors.allowed-methods`、`app.cors.allowed-headers`、`app.cors.exposed-headers`、`app.cors.allow-credentials`、`app.cors.max-age`。
- 当前提交：`7a4213b refactor: load CORS settings from application config`，已推送到 `origin/master`。

### 2026-06-02 错误信息隐藏 + analysis_status 分支

| 文件 | 改动 |
|------|------|
| `GlobalExceptionHandler.java` | catch-all 异常不再返回 `e.getMessage()`，改为固定友好提示 + `log.error()` |
| `DreamController.java` / `FileController.java` / `GuestController.java` / `UserController.java` | 错误响应隐藏异常详情，加 Logger |
| `StatsService.java` / `ResetPasswordService.java` / `MinioService.java` / `AiService.java` | 同上 |
| `SecurityConfig.java` | 添加 CSP、X-Frame-Options、X-Content-Type-Options、Referrer-Policy |
| `feat/analysis-status` 分支 | `dream_content` 新增 `analysis_status`（NONE/PENDING/SUCCESS/FAILED）+ `analysis_error`，替代哨兵字符串。部署需先执行 `db/analysis_status_patch.sql` |

### 2026-06-03 桌面路径修正

- 用户本机桌面路径统一按 `D:\desktop\` 处理，不再使用 `C:\Users\85939\Desktop\` 作为桌面路径。
- `dreamAC` 桌面文件夹路径为 `D:\desktop\dreamAC\`。
- 桌面 DreamAc 笔记路径改按 `D:\desktop\DreamAc笔记.md`。

### 2026-06-03 HttpOnly Cookie + CSRF 测试记录

| 文件 | 改动 |
|------|------|
| `CsrfProtectionFilter.java` | 给三参构造函数加 `@Autowired`，修复 Spring 启动时 `No default constructor found` |
| `SecurityConfig.java` | `csrfProtectionFilter` 改挂到 `UsernamePasswordAuthenticationFilter` 前，避免用自定义 `TokenAuthenticationFilter` 作为排序锚导致 `does not have a registered order` |
| `application.properties.example` / 本地 `application.properties` | `app.cors.allowed-headers` 补 `X-XSRF-TOKEN`，跨域部署时允许 CSRF header 预检 |

验证结果：
- `mvn test`：20 tests，0 failures，0 errors。
- `npm run build`：通过。
- `npm run test:cookie-auth`：通过。
- `java -jar target/DreamArchive-0.0.1-SNAPSHOT.jar`：Tomcat 成功启动到 8080。
- `GET /api/hello`：200，返回 `Hello Dream Archive`。
- CORS 预检带 `Access-Control-Request-Headers: X-XSRF-TOKEN, Content-Type`：200，并返回 `Access-Control-Allow-Headers: X-XSRF-TOKEN, Content-Type`。
- CSRF smoke test：带认证 Cookie 但不带 CSRF header 的 POST 返回 403 `CSRF token invalid`；带匹配 `XSRF-TOKEN` 后进入认证阶段，假 token 返回 401，符合预期。

注意：
- 部署服务器的 `application.properties` 也必须同步补 `X-XSRF-TOKEN`。
- 生产 HTTPS 部署时 `app.auth.cookie-secure=true`。
- 本机验证时出现 MinIO 和 AI provider heartbeat 的 `Permission denied`，属于外部服务/网络权限问题，不影响 HttpOnly Cookie + CSRF 验证。

### 2026-06-03 AI 资源池 Dashboard

| 文件 | 改动 |
|------|------|
| `frontend/src/api/admin.ts` | 新增 `AiProviderInfo`/`AiProviderForm` 接口 + 5 个 API 函数（getAiProviders/addAiProvider/updateAiProvider/deleteAiProvider/resetAiProviderCircuit） |
| `frontend/src/views/AdminView.vue` | 侧边栏新增"AI 资源池"tab，onMounted 自动加载 provider 列表；内容面板表格（名称/模型/URL/权重/状态/熔断/失败次数/平均延迟/操作）；新增/编辑 Provider 弹窗；启禁用/重置熔断/删除操作+确认框；深色模式适配 |
| `frontend/src/views/AdminView.vue` | 修复梦境面板 `v-else` 导致与 AI 面板混显，改为 `v-if="activePanel === 'dreams'"` |

### 2026-06-03 梦境总数统计修复

根因：
- 首页、梦境统计页使用 `/api/stats/{userId}`，原实现把最近 7 天 `dream_stats` 汇总误当成总梦境数。
- 个人资料页使用 `/api/stats/{userId}/total`，原实现只汇总 365 天 `dream_stats`，不是 `dream_content` 全量。
- “我的梦境”列表从 `dream_content` 查询全量，因此页面之间总数不一致。

| 文件 | 改动 |
|------|------|
| `StatsService.java` | `getDreamStats` 和 `getTotalDreams` 的总数统一改为 `statsMapper.countByUserId(userId)`，直接以 `dream_content` 为准 |
| `StatsServiceTest.java` | 新增回归测试，覆盖统计总览和个人资料总数必须使用 `dream_content` 全量计数 |

验证结果：
- `mvn test`：22 tests，0 failures，0 errors。
- `npm run build`：通过。

当前提交：`3b9c169 fix: count total dreams from dream content`，已推送到 `origin/master`。

### 2026-06-06 Docker 一键部署

分支：[`docker-install`](https://github.com/YiWeiLai233/DreamArchive/tree/docker-install)

| 文件 | 改动 |
|------|------|
| `docker/Dockerfile` | 后端多阶段构建（Maven 编译 → JRE 17 运行） |
| `docker/frontend.Dockerfile` | 前端多阶段构建（Node 18 → Nginx） |
| `docker/docker-compose.yml` | 5 服务编排：MySQL + Redis + MinIO + Backend + Frontend |
| `docker/nginx.conf` | Nginx 反向代理 + 登录限流 5r/m + 通用限流 10r/s + Gzip + 安全头 |
| `docker/.env.example` | 环境变量模板（MySQL/MinIO/SMTP/Auth/AI） |
| `docker/deploy.sh` | Linux 一键部署脚本 |
| `docker/deploy.ps1` | Windows 一键部署脚本 |
| `docker/README.md` | Docker 部署说明 |
| `application-docker.properties` | Spring Boot Docker 环境配置（服务名直连 + 环境变量注入） |
| `.dockerignore` | 构建排除规则 |

使用方法：
```bash
cd docker
cp .env.example .env   # 编辑填写真实配置
./deploy.sh            # Linux 一键部署
# 或 .\deploy.ps1      # Windows
# 或 docker compose up -d
```

### 2026-06-06 Docker 安全加固 + 问题清单修复

根据 `docker-install` 分支问题清单修复全部 P0/P1/P2 问题：

| 问题 | 修复 |
|------|------|
| `.dockerignore` 排除 `docker/` 导致前端构建失败 | 移除 `docker/` 排除行 |
| `app.auth.token-ttl-seconds` 配置名错误 | 改为 `app.auth.ttl-seconds`（与 `TokenService` 一致） |
| MySQL/Redis/Backend 默认暴露公网端口 | 改为 `expose`，仅前端 80 对外，MinIO 控制台绑定 `127.0.0.1:9001` |
| Redis 无密码 | 新增 `REDIS_PASSWORD` 环境变量，compose 加 `--requirepass` |
| 邮箱账号硬编码 `859399899@qq.com` | 改为 `${MAIL_USERNAME}` 环境变量 |
| MinIO bucket 无自动初始化 | 新增 `minio-init` 服务（mc 创建 bucket + 设置公开下载） |
| Swagger 默认公开 | nginx.conf 中注释掉 swagger location |
| 限流配置前缀不一致 | 改为 `app.security.rate-limit.*` 对齐 security-rate-limits 分支 |
| 上传接口无专用限流 | nginx 新增 `dream_upload` 限流区 30r/m |
| AUTH_SECRET 无长度校验 | deploy 脚本增加 32 位最小长度检查 |
| Docker 文档未区分本地/生产 | README 新增本地开发 vs 生产部署对比 + 故障排查 |

新增文件：`.github/workflows/ci.yml`（后端构建+测试）、`.github/workflows/docker-build.yml`（Docker 镜像构建验证）
