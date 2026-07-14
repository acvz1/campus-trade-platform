# 校园闲置物品智慧流转平台

> 校园二手闲置物品发布、浏览、交换、求购、私信及个人中心于一体的综合交易平台。

## 项目结构

```
campus-trade-platform/
├── backend/                # 后端 - Spring Boot + MyBatis-Plus + PostgreSQL
├── frontend/               # 前端 - React + Vite + TypeScript + Ant Design
├── admin-app/              # 独立管理端 - React + Vite + Ant Design
├── docs/                   # 项目文档（开发规范、Git 协作规范）
├── .gitignore
└── README.md
```

## 技术栈

### 后端

| 技术 | 说明 |
|------|------|
| Spring Boot 4.1 | 基础框架 |
| MyBatis-Plus 3.5 | ORM 框架 |
| PostgreSQL | 关系型数据库 |
| JWT (jjwt) | 登录鉴权 |
| springdoc-openapi 3 | API 接口文档 |
| Lombok | 简化实体类代码 |

### 前端

| 技术 | 说明 |
|------|------|
| React 19 | UI 框架 |
| Vite 8 | 构建工具 |
| TypeScript 6 | 类型安全 |
| Ant Design 6 | UI 组件库 |
| React Router 7 | 路由管理 |
| Zustand 5 | 全局状态管理 |
| Axios | HTTP 请求 |

## 快速开始

### 后端

**环境要求：** JDK 17+、Maven 3.9+、PostgreSQL 14+

```bash
cd backend

# 1. 创建数据库
createdb campus_trade

# 2. 配置数据库连接（也可直接修改 application.yml）
# Windows PowerShell: $env:DB_PASSWORD='你的数据库密码'
# macOS/Linux: export DB_PASSWORD='你的数据库密码'

# 3. 运行
./mvnw spring-boot:run
```

启动成功后访问 API 文档：http://localhost:8080/doc.html

### 前端

**环境要求：** Node.js 18+

```bash
cd frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
```

启动成功后访问：http://localhost:5173/

### 管理端

```bash
cd admin-app
npm install
npm run dev
```

启动成功后访问：http://localhost:5174/admin/login

本地 MVP 初始化管理员：`13800000000` / `Admin@123`。生产部署前必须修改默认密码。

## 平台功能

- 校园二手闲置物品发布
- 商品浏览与搜索
- 物品交换
- 求购信息发布
- 用户私信
- 个人中心
- 商品审核与运营数据看板

## 项目文档

- [产品需求](design_docs/PRD.md)
- [技术设计](design_docs/TechnicalDesign.md)
- [API 设计](design_docs/APIDesign.md)
- [数据库设计](design_docs/Database.md)
- [后端开发规范](design_docs/BackendDevelopmentGuide.md)
- [前端开发规范](design_docs/DevRule.md)
- [Git 团队协作规范](design_docs/GitGuide.md)

## 分支策略

| 分支 | 用途 |
|------|------|
| `main` | 线上稳定分支，禁止直接提交 |
| `dev` | 开发主分支，所有功能合并到此 |
| `feature/xxx` | 新功能分支 |

## License

MIT
