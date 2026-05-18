# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This directory is the frontend for DreamArchive, a dream recording and analysis application. Built with Vue 3 + Vite + TypeScript.

## Tech Stack

- Vue 3 (Composition API with `<script setup>`)
- Vite as build tool
- TypeScript
- Vue Router for routing
- Pinia for state management
- Axios for HTTP requests

## Common Commands

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint
npm run lint

# Type check
npm run type-check
```

## Project Structure

```
src/
├── api/          # API request functions (axios)
├── assets/       # Static assets (images, fonts)
├── components/   # Reusable Vue components
├── composables/  # Composition API hooks
├── layouts/      # Layout components
├── router/       # Vue Router config
├── stores/       # Pinia stores
├── types/        # TypeScript type definitions
├── utils/        # Utility functions
└── views/        # Page-level components
```

## Backend API

The backend runs at `http://localhost:8080` (Spring Boot). Available endpoints:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/hello` | Health check |
| POST | `/api/register` | User registration |
| POST | `/api/analysisDream` | Store a dream |
| GET | `/api/dream/{id}` | Get dream by ID |

## Design Direction

**Theme: 阳光梦幻 (Sunny & Dreamy)** - 面向年轻用户群体

- **色调**: 暖色系为主，浅紫/浅蓝/淡粉渐变背景，搭配阳光金/橙色点缀
- **风格**: 轻盈、柔和、有呼吸感，避免沉重压抑
- **元素**: 星星、月亮、云朵、光线等梦幻元素可作为装饰
- **字体**: 圆润可爱的字体风格，避免过于严肃
- **卡片**: 圆角 + 毛玻璃效果 (glassmorphism)，增加层次感
- **动效**: 轻微的浮动、渐入渐出动画，营造梦幻氛围

参考色彩方案:
- 主色: `#7C6FE0` (梦幻紫)
- 辅色: `#FFB347` (阳光橙)
- 背景渐变: `#E8D5F5` → `#B8E6FF` (紫蓝渐变)
- 文字: `#2D2B55` (深紫)

## Conventions

- Use `<script setup lang="ts">` for components
- Component filenames: PascalCase (e.g., `DreamCard.vue`)
- API functions go in `src/api/` and return typed responses
- Store files in `src/stores/` use Pinia's `defineStore`
- CSS: use scoped styles or Tailwind if configured

## Responsive Design

### Breakpoint Strategy

- **Mobile** (default): base font-size 16px, all sizes in `rem`
- **Tablet** (`max-width: 768px`): reduce padding, collapse grids to single column
- **Desktop** (`min-width: 1024px`): `html { font-size: 18px }` to scale all rem-based sizes by 12.5%, plus wider max-widths and larger padding

### PC Adaptation Pattern

Each view adds a `@media (min-width: 1024px)` block for desktop-specific overrides:
- Content wrappers get wider `max-width` (e.g., 700px → 900px for stats, 420px → 500px for forms)
- Navigation padding increases to `1.5rem 3rem`
- Cards get more padding and larger font sizes
- Form inputs get `padding: 0.85rem 1.1rem; font-size: 1rem`

### Common Pitfalls

- No shared layout component — each view independently defines its own container width, padding, and responsive behavior
- `.glass` class is duplicated in every view file (not shared globally)
- Background decorations (stars, clouds, glow) are copy-pasted into each view
- Only two breakpoints used: `768px` (mobile) and `1024px` (desktop)

## Store (Pinia)

`useUserStore` provides: `isLoggedIn`, `userId`, `username`, `email`, `createdAt`, `login()`, `logout()`, `updateProfile()`

- `login()` accepts 4 args: `(name, mail?, joinDate?, id?)`
- `userId` is stored as `string` in localStorage
- API functions in `src/api/user.ts` accept `string | number` for userId

## API Layer

- All API functions are in `src/api/user.ts`
- Returns `axios` promises with typed `ApiResult<T>` responses
- Stats endpoints: `getDreamStats`, `getEmotionDistribution`, `getPlaceDistribution`, `getRecentTrend`
- Auth endpoints: `register`, `login`

## Views

| View | Route | Purpose |
|------|-------|---------|
| `HomeView.vue` | `/` | Landing page with hero, features, nav |
| `LoginView.vue` | `/login` | Login form |
| `RegisterView.vue` | `/register` | Registration form |
| `ResetPasswordView.vue` | `/reset-password` | Password reset (3-step flow) |
| `ChangePasswordView.vue` | `/change-password` | Change password (logged in) |
| `DreamListView.vue` | `/dreams` | Dream list with search/filter/detail modal |
| `DreamStatsView.vue` | `/dream-stats` | Statistics charts and trends |
| `ProfileView.vue` | `/profile` | User profile view/edit |

## Build Notes

- `npm run build` runs `vue-tsc && vite build` — TypeScript errors will block the build
- If adding new store properties, update the return statement in the store definition
- API function parameter types must match how the store stores values (e.g., `string | number` for IDs)

---

## Session Learnings (2025-05-18)

### Store 数据同步到页面

**错误做法**：用 `reactive` 复制 store 值（只复制一次，之后不同步）
```typescript
// ❌ 不要这样做
const profile = reactive({
  username: userStore.username,  // 只在初始化时读取一次
})
```

**正确做法**：用 `computed` 实时响应 store 变化
```typescript
// ✅ 正确
const profileUsername = computed(() => userStore.username || '默认值')
```

### 后端密码安全

User DTO 的 password 字段使用 `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`：
- 接收前端传入的密码（注册/登录请求）✅
- 响应 JSON 中不包含密码 ✅
- 不能用 `@JsonIgnore`，因为它会同时忽略反序列化（导致注册时收不到密码）

### 后端返回用户信息

登录接口 `POST /api/login` 返回 `Result<User>`，前端通过以下路径获取：
```
data.code              → 状态码 200
data.data.username     → 用户名
data.data.email        → 邮箱
data.data.createdAt    → 注册时间（LocalDateTime 格式）
data.data.id           → 用户ID
```

loginMapper.xml 中需要 `created_at as createdAt` 做列别名映射。

### 浮动装饰元素规范

每个页面的浮动装饰元素主题不同，但统一使用 `float` 动画：
- 首页：✨ 🌙 ⭐ 💫 🌟
- 我的梦境：💭 🌙 🦋 ☁️ 💤
- 个人资料：💎 🌸 🪄 🫧
- 梦境统计：🔮 🌀 🪐 ✨ 🎆
- 登录页/注册页/修改密码：🔑 ✨ 🌙

CSS 模板：
```css
.decor { position: fixed; inset: 0; pointer-events: none; z-index: 1; }
.float-icon { position: absolute; font-size: 1.5rem; animation: float 6s ease-in-out infinite; opacity: 0.6; }
.fi-1 { top: 10%; left: 8%; animation-delay: 0s; }
/* 每个页面的 fi-N 位置和大小略有差异 */
```

### 首页布局结构

首页从上到下的结构：
1. **英雄区** — 标题 + 居中"开始记录"按钮（只有一个按钮，无"了解更多"）
2. **统计概览**（已登录时显示）— 三张 glass 卡片：梦境总数、最常情绪、最常地点，点击跳转 `/dream-stats`
3. **特性介绍** — 三张 glass 卡片：记录梦境 / AI 解梦 / 了解更多

统计数据通过 `onMounted` 调用 `getDreamStats`、`getEmotionDistribution`、`getPlaceDistribution` 并行加载，静默失败不展示错误。

### 首页功能卡片点击逻辑

未登录点击功能卡片 → 跳转登录页；已登录 → 跳转对应功能页：
```typescript
function goStats() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  router.push('/dream-stats')
}
```

### 后端 user 表结构

```sql
user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)
```

### API 接口清单（前端已对接）

| 方法 | 路径 | 前端调用位置 |
|------|------|-------------|
| POST | `/api/login` | LoginView |
| POST | `/api/register` | RegisterView |
| POST | `/api/reset-password/send-code` | ResetPasswordView |
| POST | `/api/reset-password` | ResetPasswordView |
| POST | `/api/change-password` | ChangePasswordView |
| GET | `/api/stats/{userId}` | DreamStatsView |
| GET | `/api/stats/{userId}/emotion` | DreamStatsView |
| GET | `/api/stats/{userId}/place` | DreamStatsView |
| GET | `/api/stats/{userId}/trend` | DreamStatsView |

---

## Session Learnings (2026-05-19)

### 梦境统计地点分布数据源

**问题**：修改数据库 `dream_place_stats` 表的 `dream_count` 后，前端 `/api/stats/{userId}/place` 接口返回的数据不变。

**原因**：`getPlaceDistribution` 接口原本从 `dream` 表实时统计（`statsMapper.countByPlace`），而不是从 `dream_place_stats` 表读取。

**解决方案**：修改 `StatsService.java` 的 `getPlaceDistribution` 方法，改为从 `dream_place_stats` 表读取：

```java
// 修改前：从 dream 表实时统计
List<Map<String, Object>> distribution = statsMapper.countByPlace(userId);

// 修改后：从 dream_place_stats 表读取
List<DreamPlaceStats> placeStats = dreamStatsMapper.selectPlaceStatsByUser(userId);
List<Map<String, Object>> distribution = placeStats.stream()
        .map(ps -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("label", ps.getPlace());
            map.put("value", ps.getDreamCount());
            return map;
        })
        .collect(java.util.stream.Collectors.toList());
```

**注意**：修改后端代码后需要重启 Spring Boot 服务才能生效。

### 统计接口数据源对照表

| 接口 | 数据源 | 说明 |
|------|--------|------|
| `GET /api/stats/{userId}` | `dream` 表 | 实时统计 |
| `GET /api/stats/{userId}/emotion` | `dream` 表 | 实时统计 |
| `GET /api/stats/{userId}/place` | `dream_place_stats` 表 | 从统计表读取（已修改） |
| `GET /api/stats/{userId}/trend` | `dream` 表 | 实时统计 |

### 梦境保存逻辑重构

**问题**：原来保存梦境时只是简单插入 `dream` 表，没有更新统计数据。

**新逻辑**：保存梦境时分别写入三张表：

```
POST /api/analysisDream
        ↓
    DreamService.saveDream()
        ↓
    ┌─────────────────────────────────────────┐
    │  1. dream_content 表 ← 存储梦境内容     │
    │  2. dream_stats 表   ← 更新情绪统计     │
    │  3. dream_place_stats 表 ← 更新地点统计 │
    └─────────────────────────────────────────┘
```

**新增文件：**
- `DTO/DreamContent.java` - 梦境内容实体
- `mapper/DreamContentMapper.java` - 梦境内容 Mapper
- `mapper/dreamContentMapper.xml` - MyBatis 映射
- `db/dream_content_table.sql` - 建表脚本

**数据库表结构：**

```sql
dream_content (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(100) DEFAULT '',
    content TEXT NOT NULL,
    interpretation TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)
```

**前端 API 类型更新：**

```typescript
export interface DreamContent {
    id: string
    userId: number
    title: string
    content: string
    emotion: string
    place: string
    time: string
    interpretation: string
    createdAt: string
}
```

### 错误页面与错误处理

**新增文件：**
- `views/ErrorView.vue` - 错误页面组件
- `utils/errorHandler.ts` - 错误处理工具

**支持的错误码：**
- 400: 请求错误
- 401: 未登录
- 403: 无权访问
- 404: 页面未找到
- 500: 服务器错误
- 502/503: 服务不可用

**路由配置：**
- `/error/:code` - 显示对应错误页面
- `/:pathMatch(.*)*` - 404 兜底路由

**API 拦截器：**
- `src/api/user.ts` 中配置 axios 响应拦截器
- 非200状态码自动跳转错误页面

---

## 数据库表清单

| 表名 | 用途 |
|------|------|
| `user` | 用户信息 |
| `dream` | 旧梦境表（已废弃） |
| `dream_content` | 梦境内容 |
| `dream_stats` | 情绪统计（按日期） |
| `dream_place_stats` | 地点统计 |
