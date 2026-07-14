# 后端开发规范

本文档是 `member1` 基础架构的使用说明，后续成员开发后端模块时应遵循本规范。

## 1. 环境与启动

- JDK 17+
- Maven 3.9+
- PostgreSQL 14+
- 数据库名默认 `campus_trade`

常用环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/campus_trade` | 数据库连接 |
| `DB_USERNAME` | `postgres` | 数据库用户名 |
| `DB_PASSWORD` | `postgres` | 数据库密码 |
| `DB_INIT_MODE` | `always` | 是否执行幂等建表和初始化数据 |
| `JWT_SECRET` | 内置开发密钥 | Base64 编码且解码后至少 32 字节；生产必须覆盖 |
| `UPLOAD_DIR` | `./uploads` | 图片存储根目录 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 允许的前端来源，多个值用逗号分隔 |

启动命令：

```bash
cd backend
mvn spring-boot:run
```

启动时会执行 `db/schema.sql` 和 `db/data.sql`。脚本可重复运行，默认初始化教材、数码、宿舍用品、运动器材和其他五个一级类目。

## 2. 包结构

业务代码放在 `com.campus.trade` 下：

```text
controller/              REST 接口
service/                 Service 接口
service/impl/            Service 实现
mapper/                  MyBatis-Plus Mapper
entity/                  与数据库表对齐的实体
dto/                     请求对象
vo/                      响应视图对象
common/                  公共响应、异常、工具、注解
config/                  框架配置
```

后续 Mapper 统一放在 `com.campus.trade.mapper`，已由 `MyBatisPlusConfig` 扫描。

## 3. Controller 规范

- Controller 只负责收参、校验、调用 Service 和封装 `Result<T>`，不写 SQL 或业务事务。
- JSON 请求 DTO 使用 Jakarta Validation 注解，并在参数前加 `@Valid`。
- 所有接口返回统一结构：`code`、`message`、`data`、`timestamp`。
- 公开接口与需登录接口以 `design_docs/APIDesign.md` 为准。

```java
@PostMapping
public Result<ProductVO> create(@Valid @RequestBody ProductCreateDTO request,
                                @AuthenticationPrincipal AuthenticatedUser user) {
    return Result.success(productService.create(request, user.userId()));
}
```

## 4. Service 与异常规范

- 跨多次写操作的方法使用 `@Transactional(rollbackFor = Exception.class)`。
- 业务规则不满足时抛 `BusinessException`，不要返回 `null` 或在 Controller 中手写错误响应。
- 常用错误：400 参数错误、403 权限不足、404 资源不存在、409 状态冲突。

```java
if (product == null) {
    throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
}
```

未处理异常由 `GlobalExceptionHandler` 记录日志，并统一返回 500；禁止把堆栈或数据库信息返回给前端。

## 5. JWT 与权限

请求头格式固定为：

```text
Authorization: Bearer <token>
```

Token 包含 `userId` 和 `role` 两个 claim，角色值为 `USER`、`ADMIN` 或 `SUPER_ADMIN`。Controller 通过 `@AuthenticationPrincipal AuthenticatedUser user` 获取当前用户，不接受前端传入的 userId 作为操作人身份。

管理员接口在 Controller 类或方法上添加 `@AdminOnly`。普通用户访问返回 403。

用户注册和登录模块可直接注入：

- `JwtUtils`：`generateToken(userId, role)`
- `PasswordEncoder`：BCrypt 密码哈希与校验
- `RegexUtils`：手机号、学号格式校验

## 6. MyBatis-Plus 规范

- 实体字段使用驼峰，数据库字段使用蛇形，已开启自动映射。
- 主键由 PostgreSQL 自增生成。
- 分页统一使用 MyBatis-Plus `Page<T>`，单页最大 100 条。
- 时间字段建议使用 `OffsetDateTime`；`createdAt`、`updatedAt` 可由 `MyBatisMetaObjectHandler` 自动填充。
- 软删除实体使用 `deletedAt` 字段，并与数据库的 `deleted_at` 对齐。

## 7. 文件上传

接口：`POST /api/file/upload`，使用 `multipart/form-data`：

- `file`：图片文件
- `type`：`product` 或 `avatar`
- 单文件不超过 5MB
- 支持 JPEG、PNG、WebP

服务端按文件头识别真实格式，文件保存到 `UPLOAD_DIR/yyyyMM/userId/uuid.ext`，返回 `/uploads/...` 访问地址。不得绕过 `FileService` 直接按原始文件名写磁盘。

## 8. 验证命令

```bash
cd backend
mvn clean test
```

测试使用 H2 的 PostgreSQL 兼容模式，不依赖本机数据库。提交前至少保证编译和测试全部通过。

OpenAPI 页面：`http://localhost:8080/doc.html`。
