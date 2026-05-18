# DreamArchive 项目分析与完善计划

## 一、项目概述

DreamArchive 是一个"梦境档案"后端项目，基于 Spring Boot 3.5.7 + MyBatis + MySQL，使用 Java 17 构建。核心功能包括：

- 用户注册/登录
- 梦境记录的存储与查询
- AI 梦境解析（调用外部 OpenAI 兼容 API）
- 验证码与邮件验证（规划中）

**当前状态：项目处于半成品阶段，仅用户注册和梦境存储/查询基本可用，其余功能存在严重 Bug 或尚未实现。**

---

## 二、现有功能逐模块分析

### 2.1 用户注册（基本可用，有缺陷）

**涉及文件：**
- `controller/registerController.java` — `POST /api/register`
- `service/registerService.java`
- `mapper/registerMapper.java` + `registerMapper.xml`
- `DTO/User.java`
- `util/passwordEncrypt.java`

**现状：**
注册流程基本走通：接收 User JSON → BCrypt 加密密码 → 检查用户名/邮箱是否已存在 → 插入数据库。

**存在的问题：**

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| registerMapper.xml 中 INSERT 包含 `#{id}` 但 Java 代码未设置 id | 高 | 如果数据库 id 不是自增的，插入时 id 为 null 会失败 |
| 返回值是裸字符串 "200"/"500"/"用户已经存在" | 中 | 没有统一响应格式，前端难以解析 |
| 没有输入校验 | 中 | username/password/email 为空或格式非法时不会报错 |
| 类名不符合命名规范 | 低 | `registerController`、`registerService`、`registerMapper` 均为小写开头 |
| 注入了 `CaptchaGenerator` 但未使用 | 低 | registerService 中有无用的注入 |

---

### 2.2 梦境存储与查询（基本可用，有缺陷）

**涉及文件：**
- `controller/analyzeDream.java` — `POST /api/analysisDream`、`GET /api/dream/{id}`
- `service/DreamService.java`
- `mapper/DreamMapper.java` + `dreamMapper.xml`
- `DTO/Dream.java`

**现状：**
可以存储梦境（插入 dream 表）和按 ID 查询梦境。

**存在的问题：**

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| 接口名叫 `analysisDream`（解析梦境）但实际只是存储，没有调用 AI | 高 | 名不副实，interpretation 字段由前端传入而非 AI 生成 |
| `Dream.java` 中 `Interpretation` 字段首字母大写 | 中 | Jackson 序列化后 JSON key 为 `"Interpretation"`（大写 I），与其他字段风格不一致 |
| 缺少用户维度的数据隔离 | 高 | 任何人都可以查询任何梦境，没有与用户关联 |
| 缺少修改/删除接口 | 中 | 只有插入和查询，没有完整的 CRUD |
| `@CrossOrigin` 冗余重复 | 低 | 类级别和方法级别都加了 |

---

### 2.3 用户登录（完全不可用）

**涉及文件：**
- `mapper/loginMapper.java` + `loginMapper..xml`（双点文件名）

**现状：**
只有一个空壳 Mapper 接口，没有 Controller、没有 Service。对应的 XML 文件存在严重错误。

**存在的问题：**

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| loginMapper..xml 中 `SELECT FROM dream` 缺少列名 | 致命 | SQL 语法错误，执行必报错 |
| 查错了表 | 致命 | 登录应该查 `user` 表，SQL 写的是 `dream` 表 |
| WHERE 条件用 `id` 查询 | 致命 | 登录应该用 `username` 或 `email` 查询 |
| 没有 Controller 和 Service | 致命 | 整个登录功能没有对外暴露 |
| 缺少 `@Mapper` 注解 | 高 | Spring Boot 可能无法扫描到此 Mapper |
| 文件名双点 `loginMapper..xml` | 中 | 维护混乱，建议修正 |

---

### 2.4 密码重置（基本可用，待完善）

**涉及文件：**
- `controller/ResetPasswordController.java` — `POST /api/reset-password/send-code`、`POST /api/reset-password`
- `service/ResetPasswordService.java`
- `mapper/ResetPasswordMapper.java` + `resetPasswordMapper.xml`
- `mapper/LoginMapper.java` + `loginMapper.xml`（用于检查用户是否存在）

**现状：**
已完成基本的密码重置流程：验证用户存在 → 加密新密码 → 更新数据库。前端页面完整，后端接口可用。

**已实现：**
- ✅ `POST /api/reset-password/send-code` — 发送验证码（暂时跳过实际发送）
- ✅ `POST /api/reset-password` — 重置密码（跳过验证码验证）
- ✅ 用户存在性检查
- ✅ 密码加密存储
- ✅ 统一响应格式 `Result<T>`

**待完善功能：**

| 序号 | 任务 | 说明 | 优先级 |
|------|------|------|--------|
| 1 | 验证码生成与存储 | 使用 Redis 存储验证码，设置 5 分钟过期 | 高 |
| 2 | 邮件发送服务 | 使用 JavaMailSender（SMTP）发送验证码邮件 | 高 |
| 3 | 验证码验证逻辑 | 在 `send-code` 接口生成并存储验证码，在 `reset-password` 接口验证 | 高 |
| 4 | 前端 API 函数 | 在 `user.ts` 中添加 `sendResetCode()` 和 `resetPassword()` | 中 |
| 5 | 重试限制 | 同一用户 1 分钟内只能发送一次验证码 | 低 |
| 6 | 密码强度校验 | 增加密码复杂度要求（大小写+数字+特殊字符） | 低 |

---

### 2.5 AI 梦境解析（完全不可用）

**涉及文件：**
- `service/AiService.java`
- `config/Aiconfig.java`（与 AiService 高度重复）
- `DTO/AI.java`、`DTO/messages.java`
- `util/JsonUtil.java`

**现状：**
有两个类都在做同一件事（调用外部 AI API），但都没有接入业务流程，且都有致命 Bug。

**存在的问题：**

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| `@Value` 注入 static 字段 | 致命 | Spring 无法注入 static 字段，url/apiKey/model 运行时永远为 null，必抛 NPE |
| 完全未被调用 | 致命 | 没有任何 Controller 或 Service 调用 AiService |
| AiService 和 Aiconfig 代码重复 | 高 | 两个类做了几乎一样的事情 |
| 每次调用创建新线程池但从不 shutdown | 高 | 线程泄漏 |
| 异常时返回字符串 "200" | 中 | 无法与成功结果区分 |
| AI.java 的 temperature 是 String 类型 | 低 | 部分 API 可能拒绝 |

---

### 2.6 验证码与邮件验证（空壳）

**涉及文件：**
- `service/captcha.java` — 无 `@Service` 注解，方法无返回值
- `service/emailCode.java` — 方法体只 `return "access"`
- `util/CaptchaGenerator.java` — 只生成 4 位数字字符串，非图片验证码

**现状：**
三个类都是空壳或半成品，完全没有接入业务流程。注释中提到要用 Redis 存储验证码、用 SMTP 发送邮件，但均未实现。

---

## 三、全局性问题

### 3.1 安全隐患

| 问题 | 说明 |
|------|------|
| API Key 明文提交到 Git | `application.properties` 中的 `ai.api.key` 已暴露在代码仓库 |
| 数据库密码明文提交 | `spring.datasource.password` 同样暴露 |
| CORS 完全开放 | 所有 Controller 使用 `@CrossOrigin(origins = "*")`，允许任何来源访问 |
| 没有 Spring Security 配置 | 虽然引入了 `spring-security-crypto`，但没有完整的安全过滤链 |
| 没有 JWT/Session 机制 | 无法实现用户认证和授权 |

### 3.2 代码规范问题

| 问题 | 涉及文件数 |
|------|-----------|
| 类名小写开头（不符合 Java 大驼峰规范） | 11 个类 |
| 导入了 Lombok 但未使用 `@Data` 等注解 | Dream.java、DreamRequest.java |
| 大量未使用的 import | 多个文件 |
| 返回值全是裸 String，没有统一响应格式 | 所有 Controller |
| 字段命名不一致（如 `Interpretation` 大写开头） | Dream.java |

### 3.3 架构问题

| 问题 | 说明 |
|------|------|
| 没有统一的异常处理 | 没有 `@ControllerAdvice`，异常直接抛出或吞掉 |
| 没有统一的响应格式 | 各 Controller 返回类型不一致（String / Dream / void） |
| 没有分层清晰的架构 | config 包中放了业务服务类（Aiconfig），DTO 命名不规范 |
| mapper XML 文件名双点 | `loginMapper..xml`、`resetByUsername..xml` |
| 没有数据库迁移工具 | 没有 Flyway/Liquibase，表结构靠手动建 |

---

## 四、完善计划

按照优先级分为三个阶段：

### 第一阶段：修复致命 Bug + 基础架构（让项目能跑起来并可扩展）

#### 4.1 基础架构搭建

| 序号 | 任务 | 说明 |
|------|------|------|
| 1.1 | 创建统一响应类 `Result<T>` | 包含 code/message/data 三个字段，所有 Controller 统一使用 |
| 1.2 | 创建全局异常处理 `GlobalExceptionHandler` | 使用 `@RestControllerAdvice` 统一捕获和处理异常 |
| 1.3 | 敏感配置外部化 | 将 API Key、数据库密码改为环境变量 `${DB_PASSWORD}` 方式读取 |
| 1.4 | 添加 `.gitignore` 规则 | 确保 `.env` 等敏感文件不被提交 |
| 1.5 | 全局 CORS 配置 | 创建 `WebConfig` 类统一配置 CORS，移除各 Controller 上的 `@CrossOrigin` |
| 1.6 | 修复 mapper XML 文件名 | `loginMapper..xml` → `loginMapper.xml`，`resetByUsername..xml` → `resetPasswordMapper.xml` |

#### 4.2 修复现有致命 Bug

| 序号 | 任务 | 说明 |
|------|------|------|
| 2.1 | 修复 AiService 的 `@Value` 注入 | 去掉 static 修饰符，或改用 setter 注入方式 |
| 2.2 | 修复 resetPasswordSerivce 正则表达式 | `matches("[*@.*]")` → `contains("@")`，`matches("[\\d+]")` → `matches("\\d+")` |
| 2.3 | 修复 resetPasswordMapper 参数绑定 | 方法签名改为 `resetByUsername(@Param("username") String username, @Param("newPassword") String newPassword)` |
| 2.4 | 修复 resetPasswordMapper 返回类型 | `String` → `int` |
| 2.5 | 修复 loginMapper..xml 的 SQL | 改为 `SELECT * FROM user WHERE username = #{username} OR email = #{email}` |
| 2.6 | 修复 registerMapper.xml 的 INSERT | 去掉 `#{id}`（假设数据库 id 自增），或在 Java 代码中生成 id |
| 2.7 | 修复 Dream.java 字段命名 | `Interpretation` → `interpretation`（小写开头） |

#### 4.3 类名规范化（重命名）

将以下类名改为大驼峰：

| 原名 | 新名 |
|------|------|
| `analyzeDream` | `DreamController` |
| `registerController` | `RegisterController` |
| `registerMapper` | `RegisterMapper` |
| `loginMapper` | `LoginMapper` |
| `resetPasswordMapper` | `ResetPasswordMapper` |
| `registerService` | `RegisterService` |
| `resetPasswordSerivce` | `ResetPasswordService` |
| `captcha` | `CaptchaService` |
| `emailCode` | `EmailService` |
| `passwordEncrypt` | `PasswordEncrypt` |
| `messages` | `Message` |
| `AI` | `AiRequest` |

---

### 第二阶段：实现核心功能（登录 + AI 解梦 + 完善梦境 CRUD）

#### 4.4 用户登录功能

| 序号 | 任务 | 说明 |
|------|------|------|
| 3.1 | 创建 `LoginService` | 接收 username/email + password，查询用户，用 BCrypt 验证密码 |
| 3.2 | 创建 `LoginController` | `POST /api/login`，返回登录结果（后续可加 JWT token） |
| 3.3 | 修复 `loginMapper.xml` | 正确的 SQL：按 username 或 email 查询 user 表 |
| 3.4 | 实现 JWT 认证（可选） | 引入 `jjwt` 依赖，登录成功后返回 token，后续请求携带 token 验证身份 |

#### 4.5 AI 梦境解析功能

| 序号 | 任务 | 说明 |
|------|------|------|
| 4.1 | 删除 `Aiconfig.java` 和 `config/Ai.java` | 消除重复代码和空壳类 |
| 4.2 | 重写 `AiService` | 去掉 static 字段，使用 `@Value` 正确注入配置；去掉线程池，改用同步调用或 Spring 异步 |
| 4.3 | 将 AI 解析接入梦境存储流程 | 修改 `DreamController`：存储梦境后自动调用 AiService 解析，将 interpretation 写回数据库 |
| 4.4 | 清理 `JsonUtil` | 使其成为通用工具或直接在 AiService 中使用 ObjectMapper |

#### 4.6 完善梦境 CRUD

| 序号 | 任务 | 说明 |
|------|------|------|
| 5.1 | 新增 `updateDream` | `PUT /api/dream/{id}`，修改梦境内容 |
| 5.2 | 新增 `deleteDream` | `DELETE /api/dream/{id}`，删除梦境 |
| 5.3 | 新增 `listDreams` | `GET /api/dreams`，分页查询当前用户的所有梦境 |
| 5.4 | 梦境与用户关联 | dream 表增加 `user_id` 字段，查询时按用户过滤 |

#### 4.7 密码重置功能

| 序号 | 任务 | 说明 |
|------|------|------|
| 6.1 | 创建 `ResetPasswordController` | `POST /api/reset-password`，接收 identifier + newPassword |
| 6.2 | 修复 `ResetPasswordService` 逻辑 | 修正正则、修正 Mapper 调用参数 |
| 6.3 | 修复 `resetPasswordMapper.xml` | SQL 参数名与 Java 方法参数名一致 |

---

### 第三阶段：安全加固 + 增强功能

#### 4.8 安全加固

| 序号 | 任务 | 说明 |
|------|------|------|
| 7.1 | 引入 Spring Security | 配置 `SecurityFilterChain`，保护需要登录的接口 |
| 7.2 | JWT Token 机制 | 登录返回 token，接口通过 `@PreAuthorize` 或过滤器验证身份 |
| 7.3 | 输入校验 | 使用 `@Valid` + `@NotBlank`/`@Email` 等注解校验请求参数 |
| 7.4 | CORS 收紧 | 限制允许的 origin，不再使用 `*` |

#### 4.9 验证码与邮件

| 序号 | 任务 | 说明 |
|------|------|------|
| 8.1 | 实现邮件发送服务 | 使用 JavaMailSender（SMTP）发送验证码邮件 |
| 8.2 | 实现验证码存储 | 使用 Redis 存储验证码，设置 5 分钟过期 |
| 8.3 | 完善 `CaptchaService` | 生成验证码 → 存储到 Redis → 发送邮件 → 提供验证接口 |
| 8.4 | 注册流程增加验证码校验 | 注册时要求提供邮箱验证码 |

#### 4.10 其他增强

| 序号 | 任务 | 说明 |
|------|------|------|
| 9.1 | 清理未使用的依赖 | 移除 pom.xml 中的 unirest-java、okhttp、qianfan |
| 9.2 | 清理未使用的 import | 各文件中多余的 import 语句 |
| 9.3 | 添加 Swagger/OpenAPI 文档 | 引入 springdoc-openapi，自动生成 API 文档 |
| 9.4 | 添加数据库迁移 | 引入 Flyway，版本化管理表结构 |
| 9.5 | 补充单元测试 | 使用 JUnit 5 + Mockito 编写 Service 层测试 |
| 9.6 | Docker 化 | 完善 `compose.yaml`，添加 MySQL + Redis + 应用的编排 |

---

## 五、数据库表结构建议

### 现有表

**user 表：**
```sql
CREATE TABLE user (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE
);
```

**dream 表：**
```sql
CREATE TABLE dream (
    id              VARCHAR(36) PRIMARY KEY,
    time            VARCHAR(50),
    place           VARCHAR(100),
    emotion         VARCHAR(50),
    content         TEXT,
    interpretation  TEXT
);
```

### 需要修改

1. **dream 表增加 `user_id` 字段**：关联用户，实现数据隔离
2. **考虑增加 `created_at`、`updated_at` 字段**：记录创建和更新时间
3. **如果使用 Redis 存验证码，不需要新建数据库表**

---

## 六、依赖变更建议

### 需要新增的依赖

| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-security` | 完整的 Spring Security 支持 |
| `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` | JWT Token 生成和验证 |
| `spring-boot-starter-data-redis` | Redis 连接（存储验证码） |
| `spring-boot-starter-mail` | 发送邮件 |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | Swagger API 文档 |
| `org.flywaydb:flyway-core` + `flyway-mysql` | 数据库迁移 |

### 需要移除的依赖

| 依赖 | 原因 |
|------|------|
| `com.baidubce:qianfan` | 未使用 |
| `com.mashape.unirest:unirest-java` | 未使用 |
| `com.squareup.okhttp3:okhttp` | 未使用 |

---

## 七、建议的开发顺序

```
第一阶段（基础 + 修复）
├── 1. 统一响应类 Result<T>
├── 2. 全局异常处理
├── 3. 敏感配置外部化
├── 4. 修复所有致命 Bug
├── 5. 类名规范化
└── 6. 修复 mapper XML 文件名

第二阶段（核心功能）
├── 7. 用户登录（LoginController + LoginService + JWT）
├── 8. AI 解梦接入（重写 AiService，接入梦境存储流程）
├── 9. 完善梦境 CRUD（修改/删除/列表/用户关联）
└── 10. 密码重置（修复并接通 Controller）

第三阶段（安全 + 增强）
├── 11. Spring Security 集成
├── 12. 验证码 + 邮件服务
├── 13. 输入校验
├── 14. 清理代码（依赖/import/重复类）
├── 15. API 文档（Swagger）
└── 16. 单元测试 + Docker 化
```

---

## 八、当前可工作的接口

以下接口基本可用（需确认数据库连接正常）：

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/api/hello` | 健康检查，返回 "Hello Dream Archive" | ✅ 可用 |
| POST | `/api/register` | 用户注册（密码 BCrypt 加密） | ✅ 可用 |
| POST | `/api/analysisDream` | 存储梦境（不含 AI 解析） | ✅ 可用 |
| GET | `/api/dream/{id}` | 按 ID 查询梦境 | ✅ 可用 |
| POST | `/api/reset-password/send-code` | 发送重置密码验证码 | ✅ 可用（暂无实际邮件发送） |
| POST | `/api/reset-password` | 重置密码 | ✅ 可用（暂跳过验证码验证） |
| GET | `/api/stats/{userId}` | 获取用户梦境统计（实时查询） | ✅ 可用 |
| GET | `/api/stats/{userId}/cached` | 获取用户梦境统计（从统计表） | ✅ 可用 |
| GET | `/api/stats/{userId}/total` | 获取用户梦境总数 | ✅ 可用 |
| GET | `/api/stats/{userId}/emotion` | 获取情绪分布统计 | ✅ 可用 |
| GET | `/api/stats/{userId}/place` | 获取地点分布统计 | ✅ 可用 |
| GET | `/api/stats/{userId}/trend?days=7` | 获取最近N天趋势 | ✅ 可用 |

### 忘记密码功能待完善清单

| 功能 | 当前状态 | 后续计划 |
|------|---------|---------|
| 验证码生成 | 跳过 | 引入 Redis，生成 6 位随机数字验证码 |
| 验证码存储 | 跳过 | 使用 Redis 存储，key 为 `captcha:{identifier}`，5 分钟过期 |
| 邮件发送 | 跳过 | 引入 `spring-boot-starter-mail`，使用 SMTP 发送 |
| 验证码验证 | 跳过 | 比对用户输入与 Redis 中存储的验证码 |
| 重试限制 | 无 | 同一用户 1 分钟内只能请求一次 |
| 前端 API 封装 | 未封装 | 在 `user.ts` 中添加 `sendResetCode()` 和 `resetPassword()` |

### 梦境统计功能

**涉及文件：**
- `controller/StatsController.java` — 统计接口
- `service/StatsService.java` — 统计服务
- `mapper/StatsMapper.java` — 实时统计查询
- `mapper/DreamStatsMapper.java` — 统计表操作
- `DTO/DreamStats.java` — 统计数据模型
- `DTO/DreamStatsEntity.java` — 每日统计实体
- `DTO/DreamPlaceStats.java` — 地点统计实体
- `mapper/statsMapper.xml` — 实时统计 SQL
- `mapper/dreamStatsMapper.xml` — 统计表 SQL
- `db/dream_stats_table.sql` — 建表脚本

**数据库表：**
- `dream_stats` — 每日统计汇总表（按用户+日期聚合）
- `dream_place_stats` — 地点统计表（按用户+地点聚合）

**已实现接口：**

| 接口 | 说明 |
|------|------|
| `GET /api/stats/{userId}` | 实时查询 dream 表统计数据 |
| `GET /api/stats/{userId}/cached` | 从统计表查询（性能更好） |
| `GET /api/stats/{userId}/total` | 获取梦境总数 |
| `GET /api/stats/{userId}/emotion` | 情绪分布（开心/难过/恐惧/愤怒/平静/其他） |
| `GET /api/stats/{userId}/place` | 地点分布（Top 10） |
| `GET /api/stats/{userId}/trend?days=7` | 最近 N 天趋势 |

**统计数据自动更新：**
- 调用 `StatsService.updateStatsOnNewDream()` 可在用户添加梦境时自动更新统计表
- 情绪自动分类：开心、难过、恐惧、愤怒、平静、其他

**待完善：**

| 功能 | 说明 |
|------|------|
| 自动触发 | 在 DreamService 存储梦境后自动调用 `updateStatsOnNewDream()` |
| 历史数据同步 | 提供批量同步接口，将历史 dream 数据导入统计表 |
| 更多维度 | 按周/月统计、按时间段分析 |
| 前端页面 | 创建统计图表页面（饼图、柱状图、折线图） |

---

## Session Learnings (2025-05-18)

### 登录接口返回格式

`POST /api/login` 返回 `Result<User>`，前端通过以下路径获取：
```
data.code              → 状态码 200
data.data.id           → 用户ID (int)
data.data.username     → 用户名
data.data.email        → 邮箱
data.data.createdAt    → 注册时间（LocalDateTime 格式，如 "2024-01-01T10:30:00"）
```

loginMapper.xml 中需要 `created_at as createdAt` 做列别名映射，否则前端收不到注册时间。

### User DTO 密码安全

使用 `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`：
- 接收前端传入的密码（注册/登录请求）✅
- 响应 JSON 中自动排除密码字段 ✅
- 不能用 `@JsonIgnore`，因为它会同时忽略反序列化（导致注册时收不到密码）

### user 表实际结构

```sql
user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)
```

### 前后端 userId 类型对齐

- 数据库 `user.id` 是 `INT`
- 后端 Java 用 `int` / `Integer`
- 前端 store 用 `string`（localStorage 存储）
- 前端 API 函数参数类型声明为 `string | number` 以兼容

### 统一响应格式 Result<T>

所有接口使用 `Result<T>` 包装返回值：
```java
Result.success(data)    → { code: 200, message: "success", data: T }
Result.error(msg)       → { code: 500, message: msg, data: null }
```

### 前端已对接的完整 API 清单

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
| GET | `/api/stats/{userId}/trend?days=7` | DreamStatsView |

### CORS 配置

前端 dev server 在 `localhost:5173`，后端在 `localhost:8080`。开发阶段使用 Vite proxy 代理 `/api` 请求，避免 CORS 问题。生产环境需要后端配置 `WebConfig` 统一 CORS。

### 数据库表结构与读写关系

系统有两张梦境表，必须注意读写一致性：

| 表名 | 用途 | 写入方 | 读取方 |
|------|------|--------|--------|
| `dream_content` | 梦境主表（当前使用） | `DreamService.saveDream()` | `DreamContentMapper`、`statsMapper.xml` |
| `dream` | 旧表（仅测试用） | `TestDataController`（JDBC 直写） | `DreamMapper`（按 ID 查询） |
| `dream_stats` | 每日统计汇总 | `StatsService.updateStatsOnNewDream()` | `DreamStatsMapper` |
| `dream_place_stats` | 地点统计汇总 | `StatsService.updateStatsOnNewDream()` | `DreamStatsMapper` |

**关键规则：所有新的梦境数据写入 `dream_content` 表，统计 SQL 必须查 `dream_content` 表。**

`dream_content` 表结构：
```sql
dream_content (
  id VARCHAR(36) PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(200),
  content TEXT,
  emotion VARCHAR(50),
  place VARCHAR(100),
  time VARCHAR(50),
  interpretation TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
)
```

### 统计 Bug 修复记录（2025-05-19）

**问题**：梦境统计页面数据全部为 0，情绪统计缺失。

**根因**：`statsMapper.xml` 中的统计 SQL 查询的是 `dream` 表，但 `DreamService.saveDream()` 写入的是 `dream_content` 表，导致实时统计查不到数据。即使改为查 `dream_content`，情绪字段（emotion）在旧记录中为 NULL，仍无法正确统计。

**修复方案**：将 StatsService 中所有统计方法改为从预计算表 `dream_stats` / `dream_place_stats` 读取，不再依赖实时查询。

| 方法 | 改动前（查 `dream_content`） | 改动后（查 `dream_stats`） |
|------|---------------------------|--------------------------|
| `getDreamStats` | `statsMapper` 实时聚合 | `dreamStatsMapper.selectRecentDays` 聚合 |
| `getTotalDreams` | `statsMapper.countByUserId` | `selectRecentDays` 求和 |
| `getEmotionDistribution` | `statsMapper.countByEmotion` | 从 `dream_stats` 的情绪列聚合 |
| `getRecentTrend` | `statsMapper.countRecentDays` | `dreamStatsMapper.selectRecentDays` |
| `getPlaceDistribution` | 已用 `dream_place_stats` | 不变 |

新增辅助方法：`buildEmotionDistribution()`（聚合情绪列）、`buildPlaceDistribution()`（构建地点分布）。

**验证**：`GET /api/stats/3` 返回正确数据（10条梦境，情绪/地点/趋势完整）。

**教训**：
1. 多张功能相似的表必须确保读写一致
2. 预计算表（`dream_stats`）比实时查询更可靠，应优先使用
3. `dream_content` 表的 `emotion`/`place`/`time` 列是后加的，旧记录为 NULL，需要默认值填充

### 个人资料头像功能记录（2026-05-19）

已在前端个人资料页加入自定义头像功能：

- 文件：`fount/src/views/ProfileView.vue`
- 状态管理：`fount/src/stores/index.ts`
- 首页头像展示：`fount/src/views/HomeView.vue`
- 当前实现方式：前端选择图片后转为 Data URL，存入 `localStorage.avatar`
- 上传限制：仅允许图片文件，大小不超过 2MB
- UI 规则：只保留“更换头像”，不要添加“移除头像”按钮

重要注意：

- `ProfileView.vue` 中的背景/浮动装饰图标（如 💎、🌸、🪄、🫧）是用户指定的视觉设计元素，不是乱码。
- 不要把这些装饰图标当作编码错误清理或替换。
- 如需修复真正的乱码文本，只改影响语法、构建或可读性的文本，不要改动背景装饰图标。
