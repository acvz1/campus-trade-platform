# 校园二手物品交易平台 — 技术设计文档

> 版本：v1.0  
> 日期：2026-07-04  

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈选型](#2-技术栈选型)
3. [系统总体架构](#3-系统总体架构)
4. [项目目录结构](#4-项目目录结构)
5. [模块划分说明](#5-模块划分说明)
6. [数据库设计概要](#6-数据库设计概要)
7. [接口规范](#7-接口规范)
8. [部署方案](#8-部署方案)

---

## 1. 项目概述

### 1.1 项目背景

面向本校师生的二手物品交易平台，聚焦教材、数码、宿舍用品、运动器材等闲置物品的流转。核心差异化竞争力在于**校园专属核验机制**，通过手机号+学号双重实名认证，从源头杜绝校外商贩入驻，打造纯净的校内交易生态。

### 1.2 核心目标

| 目标 | 说明 |
|------|------|
| 安全可信 | 双重实名认证，确保交易双方均为本校师生 |
| 简单易用 | 发布快、搜索准、沟通顺畅 |
| 轻量高效 | 聚焦核心交易链路，不做大而全 |
| 可运营 | 后台审核+数据看板，支撑平台治理 |

### 1.3 用户角色

```
┌──────────────────────────────────────────────────────┐
│                    用户角色体系                         │
├───────────────┬──────────────┬─────────────────────────┤
│   普通用户     │   管理员      │   超级管理员             │
├───────────────┼──────────────┼─────────────────────────┤
│ 浏览/搜索商品  │ 商品审核      │ 管理员账号管理           │
│ 发布/编辑商品  │ 违规下架      │ 系统配置                 │
│ 收藏/下单     │ 用户黑名单    │ 数据全量查看             │
│ 私信聊天      │ 类目管理      │                         │
│ 发布求购      │ 数据看板      │                         │
└───────────────┴──────────────┴─────────────────────────┘
```

---

## 2. 技术栈选型

### 2.1 总体选型原则

- **团队熟悉度优先**：选择Java生态，降低学习成本
- **轻量级**：避免过度设计，Spring Boot单体起步，后续可按需拆分
- **校园场景适配**：面向校内网络环境，不依赖高成本云服务
- **前后端分离**：便于并行开发与独立部署

### 2.2 技术栈明细

#### 后端技术栈

| 层次 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| 基础框架 | Spring Boot | 2.7.x / 3.2.x | Java生态标准，自动配置，开箱即用 |
| Web层 | Spring MVC | — | RESTful API，与Spring Boot深度集成 |
| 安全框架 | Spring Security + JWT | — | 成熟的认证授权体系，JWT实现无状态会话 |
| ORM | MyBatis-Plus | 3.5.x | 简化CRUD，条件构造器灵活，学习成本低 |
| 数据库 | MySQL | 8.0+ | 关系型数据，事务支持，校园场景首选 |
| 缓存 | Redis | 7.0+ | 会话存储、热点数据缓存、分布式锁 |
| 搜索引擎 | Elasticsearch | 8.x | 商品全文检索（前期可降级为MySQL LIKE） |
| 消息队列 | RabbitMQ / RocketMQ | — | 订单状态异步流转、消息通知（前期可降级） |
| 实时通信 | WebSocket (Spring) | — | 私信聊天即时推送 |
| 文件存储 | MinIO / 本地磁盘 | — | 商品图片存储，MinIO兼容S3协议 |
| 接口文档 | Knife4j (Swagger) | 4.x | 自动生成API文档，便于前后端联调 |
| 数据校验 | Hibernate Validator | — | 注解式参数校验，减少手动if-else |
| 工具库 | Hutool | 5.8.x | Java工具类集合，减少重复代码 |
| 对象拷贝 | MapStruct | 1.5.x | 编译期生成转换代码，性能优于BeanUtils |
| 构建工具 | Maven | 3.8+ | 依赖管理，多模块构建 |

#### 前端技术栈（推荐）

| 层次 | 技术 | 选型理由 |
|------|------|----------|
| 用户端框架 | Vue 3 + Vant UI | 移动端优先，Vant组件库适配H5 |
| 管理端框架 | Vue 3 + Element Plus | 成熟的中后台组件库 |
| 状态管理 | Pinia | Vue 3官方推荐 |
| HTTP客户端 | Axios | 拦截器、请求封装 |
| 构建工具 | Vite | 快速冷启动，HMR |

#### 开发与运维

| 工具 | 用途 |
|------|------|
| Git + Gitee/GitLab | 版本控制 |
| Jenkins / GitHub Actions | CI/CD |
| Docker + Docker Compose | 容器化部署 |
| Nginx | 反向代理、静态资源 |

---

## 3. 系统总体架构

### 3.1 架构全景图

```
┌────────────────────────────────────────────────────────────────────────────┐
│                              客户端层 (Client)                               │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐  │
│  │   用户端 H5/小程序    │  │   管理端 Web (PC)     │  │   外部系统接口     │  │
│  │   (Vue3 + Vant)      │  │   (Vue3 + Element+)   │  │   (教务系统对接)   │  │
│  └──────────┬───────────┘  └──────────┬───────────┘  └────────┬─────────┘  │
└─────────────┼─────────────────────────┼───────────────────────┼────────────┘
              │                         │                       │
              ▼                         ▼                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                              接入层 (Gateway)                                │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                        Nginx (反向代理 + 静态资源)                      │  │
│  │                   SSL终结 / 限流 / 负载均衡 / Gzip压缩                   │  │
│  └────────────────────────────────┬─────────────────────────────────────┘  │
└───────────────────────────────────┼────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                            应用层 (Application)                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                     Spring Boot 单体应用 (可拆微服务)                    │  │
│  │                                                                        │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │  │
│  │  │ 用户模块  │ │ 商品模块  │ │ 交易模块  │ │ 订单模块  │ │  管理模块   │  │  │
│  │  │ user     │ │ product  │ │ chat     │ │ order    │ │  admin     │  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────┘  │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐                              │  │
│  │  │ 搜索模块  │ │ 求购模块  │ │ 文件模块  │                              │  │
│  │  │ search   │ │ wanted   │ │ file      │                              │  │
│  │  └──────────┘ └──────────┘ └──────────┘                              │  │
│  │                                                                        │  │
│  │  横向切面：Spring Security (认证/鉴权) | AOP (日志/异常) | Validator     │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                              数据层 (Data)                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐  │
│  │  MySQL   │ │  Redis   │ │  MinIO   │ │  Elastic │ │  RabbitMQ      │  │
│  │  主数据库 │ │  缓存     │ │  图片存储 │ │  search  │ │  消息队列       │  │
│  │          │ │  会话     │ │          │ │  搜索引擎 │ │  (前期可选)     │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 核心业务流程

#### 商品交易主流程

```
用户A(卖家)                                    用户B(买家)
    │                                              │
    │  ① 发布商品(图文+价格+交易方式)                  │
    │─────────────────────────────────────────────►│
    │                                              │
    │  ② 管理员审核通过，商品上架                      │
    │                                              │
    │                                    ③ 搜索/浏览发现商品
    │                                              │
    │                                    ④ 收藏或发起私信
    │◄─────────────────────────────────────────────│
    │                                              │
    │  ⑤ 私信沟通(预约自提时间/地点)                   │
    │◄────────────────────────────────────────────►│
    │                                              │
    │                                    ⑥ 下单(确认交易)
    │◄─────────────────────────────────────────────│
    │                                              │
    │                   订单状态：待沟通 → 待自提 → 已完成
    │                                              │
```

#### 订单状态流转

```
                    ┌──────────┐
                    │  待沟通   │  ← 买家下单后的初始状态
                    └────┬─────┘
                         │ 双方确认交易细节
                         ▼
                    ┌──────────┐
                    │  待自提   │  ← 约定好时间地点
                    └────┬─────┘
                         │ 当面交易完成
                         ▼
                    ┌──────────┐
                    │  已完成   │  ← 双方确认
                    └──────────┘

        异常分支：
        待沟通 ──取消──► 已取消
        待自提 ──取消──► 已取消
```

### 3.3 校园核验机制设计

```
用户注册流程：
┌─────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────┐
│ 手机号   │───►│ 短信验证码   │───►│ 输入学号+姓名  │───►│ 学号校验  │
│ 注册     │    │ 验证        │    │              │    │          │
└─────────┘    └─────────────┘    └──────────────┘    └────┬─────┘
                                                          │
                                          ┌───────────────┴───────────────┐
                                          ▼                               ▼
                                   ┌──────────┐                   ┌──────────────┐
                                   │ 校验通过  │                   │  校验失败     │
                                   │ 注册成功  │                   │  拒绝注册     │
                                   └──────────┘                   │  提示联系管理  │
                                                                  └──────────────┘

学号校验策略（按优先级降级）：
  方案A：对接学校教务系统API实时校验（最优，需学校配合）
  方案B：导入学生信息表做离线比对（学期更新）
  方案C：学号格式正则校验 + 校园邮箱验证（轻量启动方案）
  推荐：前期方案C + 方案B结合，后期迁移到方案A
```

---

## 4. 项目目录结构

### 4.1 后端 (Spring Boot 多模块)

```
campus-trade/
├── pom.xml                              # 父POM，依赖管理
├── campus-trade-common/                 # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/campus/common/
│       ├── constant/                    # 常量定义
│       │   ├── RedisConstant.java       #   Redis Key前缀
│       │   ├── OrderStatusEnum.java     #   订单状态枚举
│       │   └── ProductStatusEnum.java   #   商品状态枚举
│       ├── exception/                   # 全局异常
│       │   ├── BusinessException.java   #   业务异常
│       │   └── GlobalExceptionHandler.java # 全局异常处理
│       ├── result/                      # 统一响应
│       │   ├── Result.java              #   统一返回体
│       │   └── ResultCode.java          #   状态码枚举
│       ├── utils/                       # 工具类
│       │   ├── JwtUtils.java            #   JWT工具
│       │   └── RegexUtils.java          #   正则校验工具
│       └── annotation/                  # 自定义注解
│           └── AdminOnly.java           #   管理员权限注解
│
├── campus-trade-framework/              # 框架模块
│   ├── pom.xml
│   └── src/main/java/com/campus/framework/
│       ├── config/                      # 配置类
│       │   ├── SecurityConfig.java      #   Spring Security配置
│       │   ├── RedisConfig.java         #   Redis配置
│       │   ├── WebSocketConfig.java     #   WebSocket配置
│       │   ├── MyBatisPlusConfig.java   #   MyBatis-Plus配置
│       │   ├── CorsConfig.java          #   跨域配置
│       │   └── SwaggerConfig.java       #   Knife4j配置
│       ├── filter/                      # 过滤器
│       │   └── JwtAuthenticationFilter.java # JWT认证过滤器
│       ├── handler/                     # 处理器
│       │   └── MetaObjectHandler.java   #   MyBatis自动填充
│       └── interceptor/                 # 拦截器
│           └── RateLimitInterceptor.java #  限流拦截器
│
├── campus-trade-modules/                # 业务模块（可按需拆分为独立模块）
│   ├── pom.xml
│   └── src/main/java/com/campus/modules/
│       ├── user/                        # 用户模块
│       │   ├── controller/
│       │   │   └── UserController.java
│       │   ├── service/
│       │   │   ├── UserService.java
│       │   │   └── impl/
│       │   │       └── UserServiceImpl.java
│       │   ├── mapper/
│       │   │   └── UserMapper.java
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── UserAddress.java
│       │   │   └── UserAuth.java
│       │   └── dto/
│       │       ├── UserLoginDTO.java
│       │       ├── UserRegisterDTO.java
│       │       └── UserVO.java
│       │
│       ├── product/                     # 商品模块
│       │   ├── controller/
│       │   │   └── ProductController.java
│       │   ├── service/
│       │   │   ├── ProductService.java
│       │   │   └── impl/
│       │   │       └── ProductServiceImpl.java
│       │   ├── mapper/
│       │   │   └── ProductMapper.java
│       │   ├── entity/
│       │   │   ├── Product.java
│       │   │   ├── ProductImage.java
│       │   │   └── Category.java
│       │   └── dto/
│       │       ├── ProductPublishDTO.java
│       │       ├── ProductSearchDTO.java
│       │       └── ProductVO.java
│       │
│       ├── order/                       # 订单模块
│       │   ├── controller/
│       │   │   └── OrderController.java
│       │   ├── service/
│       │   │   ├── OrderService.java
│       │   │   └── impl/
│       │   │       └── OrderServiceImpl.java
│       │   ├── mapper/
│       │   │   └── OrderMapper.java
│       │   ├── entity/
│       │   │   └── Order.java
│       │   └── dto/
│       │       ├── OrderCreateDTO.java
│       │       └── OrderVO.java
│       │
│       ├── chat/                        # 私信模块
│       │   ├── controller/
│       │   │   └── ChatController.java
│       │   ├── service/
│       │   │   ├── ChatService.java
│       │   │   └── impl/
│       │   │       └── ChatServiceImpl.java
│       │   ├── mapper/
│       │   │   ├── MessageMapper.java
│       │   │   └── ConversationMapper.java
│       │   ├── entity/
│       │   │   ├── Message.java
│       │   │   └── Conversation.java
│       │   └── websocket/
│       │       └── ChatWebSocketHandler.java
│       │
│       ├── favorite/                    # 收藏模块
│       │   ├── controller/
│       │   │   └── FavoriteController.java
│       │   ├── service/
│       │   │   └── FavoriteService.java
│       │   ├── mapper/
│       │   │   └── FavoriteMapper.java
│       │   └── entity/
│       │       └── Favorite.java
│       │
│       ├── wanted/                      # 求购模块
│       │   ├── controller/
│       │   │   └── WantedController.java
│       │   ├── service/
│       │   │   ├── WantedService.java
│       │   │   └── impl/
│       │   │       └── WantedServiceImpl.java
│       │   ├── mapper/
│       │   │   └── WantedMapper.java
│       │   ├── entity/
│       │   │   └── WantedPost.java
│       │   └── dto/
│       │       ├── WantedPublishDTO.java
│       │       └── WantedVO.java
│       │
│       └── admin/                       # 管理模块
│           ├── controller/
│           │   └── AdminController.java
│           ├── service/
│           │   ├── AdminService.java
│           │   └── impl/
│           │       └── AdminServiceImpl.java
│           └── dto/
│               ├── AuditDTO.java
│               └── DashboardVO.java
│
├── campus-trade-api/                    # 启动模块
│   ├── pom.xml
│   └── src/main/java/com/campus/
│       ├── CampusTradeApplication.java  # Spring Boot 启动类
│       └── resources/
│           ├── application.yml          # 主配置
│           ├── application-dev.yml      # 开发环境
│           ├── application-prod.yml     # 生产环境
│           ├── db/
│           │   └── campus_trade.sql     # 数据库初始化脚本
│           └── logback-spring.xml       # 日志配置
│
└── doc/                                 # 文档
    ├── 技术设计文档.md
    ├── API文档.md
    └── 部署文档.md
```

### 4.2 前端目录结构

```
campus-trade-frontend/
├── user-app/                            # 用户端 H5
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/                      # 路由
│       │   └── index.ts
│       ├── stores/                      # Pinia状态管理
│       │   ├── user.ts
│       │   └── chat.ts
│       ├── api/                         # 接口封装
│       │   ├── request.ts               #   Axios实例+拦截器
│       │   ├── user.ts
│       │   ├── product.ts
│       │   ├── order.ts
│       │   └── chat.ts
│       ├── views/                       # 页面
│       │   ├── login/
│       │   ├── register/
│       │   ├── home/
│       │   ├── product/
│       │   │   ├── detail.vue
│       │   │   └── publish.vue
│       │   ├── order/
│       │   ├── chat/
│       │   ├── wanted/
│       │   └── user/
│       │       ├── profile.vue
│       │       └── address.vue
│       ├── components/                  # 公共组件
│       │   ├── ProductCard.vue
│       │   ├── SearchBar.vue
│       │   └── NavBar.vue
│       └── assets/                      # 静态资源
│
└── admin-app/                           # 管理端 PC
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── main.ts
        ├── App.vue
        ├── router/
        ├── stores/
        ├── api/
        ├── views/
        │   ├── login/
        │   ├── dashboard/               # 数据看板
        │   ├── audit/                   # 商品审核
        │   ├── product-manage/          # 商品管理
        │   ├── user-manage/             # 用户管理（黑名单）
        │   └── category/                # 类目管理
        └── components/
```

---

## 5. 模块划分说明

### 5.1 模块总览

| 序号 | 模块名称 | 英文标识 | 功能概述 | 优先级 |
|------|---------|---------|---------|--------|
| 1 | 用户模块 | user | 注册登录、双重认证、角色管理、个人信息 | P0 |
| 2 | 商品模块 | product | 商品CRUD、分类、图片、上下架 | P0 |
| 3 | 订单模块 | order | 下单、状态流转、订单记录 | P0 |
| 4 | 搜索模块 | search | 关键词检索、分类筛选、排序 | P1 |
| 5 | 收藏模块 | favorite | 收藏/取消、收藏列表 | P1 |
| 6 | 私信模块 | chat | 实时对话、消息列表 | P1 |
| 7 | 求购模块 | wanted | 发布求购、系统匹配 | P2 |
| 8 | 管理模块 | admin | 审核、黑名单、类目、数据看板 | P0 |
| 9 | 文件模块 | file | 图片上传、存储、缩略图 | P0 |

### 5.2 模块详细设计

---

#### 模块1：用户模块 (user)

**职责**：用户身份认证、个人信息管理、收货地址管理

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/register | 手机号+学号注册 |
| POST | /api/user/login | 手机号+密码登录 |
| POST | /api/user/send-code | 发送短信验证码 |
| GET | /api/user/info | 获取当前用户信息 |
| PUT | /api/user/info | 修改个人信息 |
| POST | /api/user/address | 新增收货地址 |
| GET | /api/user/address | 地址列表 |
| PUT | /api/user/address/{id} | 修改地址 |
| DELETE | /api/user/address/{id} | 删除地址 |

**关键设计**：
```
注册认证流程：
1. 手机号 + 短信验证码 -> 初步验证
2. 学号 + 姓名 -> 校园身份核验
3. 核验通过 -> 创建用户 + 分配角色(默认普通用户)
4. JWT签发 -> 返回token

认证策略：
- 学号规则：正则匹配本校学号格式 (如: 20XX10XXXX)
- 黑名单校验：注册时检查是否在黑名单中
- 一个手机号只能注册一个账号
- 一个学号只能注册一个账号
```

**数据表**：
- `user` — 用户主表
- `user_auth` — 认证记录表
- `user_address` — 收货地址表

---

#### 模块2：商品模块 (product)

**职责**：商品发布、编辑、上下架、分类浏览

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/product | 发布商品 |
| PUT | /api/product/{id} | 编辑商品 |
| GET | /api/product/{id} | 商品详情 |
| DELETE | /api/product/{id} | 删除商品 |
| PUT | /api/product/{id}/status | 上架/下架 |
| GET | /api/product/my | 我的发布列表 |
| GET | /api/product/category | 类目列表 |

**商品状态机**：
```
发布 ──► 待审核 ──审核通过──► 已上架
                └──审核拒绝──► 审核失败(用户可修改重提)
         已上架 ──用户下架──► 已下架
         已上架 ──管理员下架──► 违规下架
         已下架 ──重新上架──► 已上架(需重新审核)
```

**关键设计**：
```
商品发布字段：
- 标题 (必填, 2-50字)
- 描述 (必填, 10-1000字)
- 分类 (必填, 从类目表选择)
- 价格 (必填, 0.01-99999.99)
- 原价 (选填)
- 交易方式: 自提 / 送货到校内
- 图片 (1-9张, 第一张为主图)
- 成色: 全新 / 几乎全新 / 有使用痕迹

防刷策略：
- 同一用户每日发布上限5件
- 标题相似度检测(防重复发布)
- 敏感词过滤
```

**数据表**：
- `category` — 商品类目表
- `product` — 商品主表
- `product_image` — 商品图片表

---

#### 模块3：订单模块 (order)

**职责**：创建订单、状态流转、交易记录

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/order | 创建订单 |
| PUT | /api/order/{id}/status | 更新订单状态 |
| GET | /api/order/{id} | 订单详情 |
| GET | /api/order/buy | 我买到的 |
| GET | /api/order/sell | 我卖出的 |

**订单状态流转**：
```
待沟通 ──► 待自提 ──► 已完成
  │          │
  └── 已取消  └── 已取消
```

**关键设计**：
```
订单创建规则：
- 买家必须已登录且完成实名认证
- 同一商品不可重复下单
- 卖家不能购买自己的商品
- 已下架商品不可下单

状态变更约束：
- 待沟通 → 待自提：买卖双方均同意
- 待自提 → 已完成：买卖双方确认交易完成
- 任一状态 → 已取消：任一方发起取消
- 不允许跨状态跳跃
```

**数据表**：
- `order` — 订单主表

---

#### 模块4：搜索模块 (search)

**职责**：商品搜索、分类筛选、排序

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/search | 关键词搜索 |
| GET | /api/search/hot | 热门搜索词 |

**关键设计**：
```
搜索策略（渐进式）：
阶段1（MVP）：MySQL LIKE + 分类筛选，数据量<10000时足够
阶段2（优化）：Elasticsearch全文检索，分词搜索，搜索建议
阶段3（智能）：基于用户行为的推荐排序

筛选维度：
- 分类 (多选)
- 价格区间
- 成色
- 交易方式
- 排序: 最新发布 / 价格升序 / 价格降序
```

---

#### 模块5：收藏模块 (favorite)

**职责**：商品收藏与取消

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/favorite/{productId} | 收藏商品 |
| DELETE | /api/favorite/{productId} | 取消收藏 |
| GET | /api/favorite | 收藏列表 |

**数据表**：
- `favorite` — 收藏记录表(用户ID+商品ID唯一索引)

---

#### 模块6：私信模块 (chat)

**职责**：买卖双方实时文字沟通

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/chat/conversations | 会话列表 |
| GET | /api/chat/messages/{conversationId} | 历史消息 |
| POST | /api/chat/send | 发送消息 |
| WS | /ws/chat | WebSocket连接 |

**关键设计**：
```
WebSocket连接流程：
1. 客户端携带JWT token建立WS连接
2. 服务端验证token，建立用户-会话映射
3. 消息通过WS实时推送到接收方
4. 消息落库MySQL，离线消息上线后拉取

会话创建规则：
- 买家在商品详情页点击"联系卖家"自动创建会话
- 会话唯一 = 商品ID + 买家ID + 卖家ID
- 每个会话关联一个商品
```

**数据表**：
- `conversation` — 会话表
- `message` — 消息表

---

#### 模块7：求购模块 (wanted)

**职责**：用户发布求购需求，系统匹配商品

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/wanted | 发布求购 |
| GET | /api/wanted | 求购列表 |
| GET | /api/wanted/{id} | 求购详情 |
| GET | /api/wanted/{id}/matches | 查看匹配商品 |

**匹配策略**：
```
匹配维度（权重递减）：
1. 分类匹配 (必须)
2. 关键词匹配 (标题+描述)
3. 价格区间重叠
4. 发布时间新鲜度

匹配结果排序：综合得分 = 分类(50%) + 关键词(30%) + 价格(15%) + 时间(5%)
```

**数据表**：
- `wanted_post` — 求购帖子表

---

#### 模块8：管理模块 (admin)

**职责**：商品审核、违规下架、用户黑名单、类目管理、数据看板

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/product/pending | 待审核商品列表 |
| POST | /api/admin/product/audit | 审核商品(通过/拒绝) |
| PUT | /api/admin/product/{id}/violation | 违规下架 |
| POST | /api/admin/user/blacklist | 加入/移除黑名单 |
| GET | /api/admin/user/blacklist | 黑名单列表 |
| POST | /api/admin/category | 新增类目 |
| PUT | /api/admin/category/{id} | 编辑类目 |
| DELETE | /api/admin/category/{id} | 删除类目 |
| GET | /api/admin/dashboard | 数据看板 |

**数据看板指标**：
```
今日数据：
- 新增用户数
- 新增商品数
- 成交订单数
- 成交金额(GMV)

累计数据：
- 总用户数
- 总商品数
- 总成交量
- 类目发布量分布(饼图)
- 近7日发布量趋势(折线图)
```

**权限控制**：
```
管理员角色：
- 商品审核员：审核权限
- 用户管理员：黑名单权限
- 超级管理员：全部权限（含类目管理、配置）

权限注解：
@AdminOnly("AUDIT")  — 审核权限
@AdminOnly("USER")   — 用户管理权限
@AdminOnly("SUPER")  — 超级管理员
```

---

#### 模块9：文件模块 (file)

**职责**：图片上传、存储、访问

**核心接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/file/upload | 上传图片(支持多文件) |
| GET | /api/file/{filename} | 访问图片 |

**关键设计**：
```
上传限制：
- 格式：jpg, jpeg, png, webp
- 单张最大：5MB
- 每个商品最多9张图

存储路径：
/{env}/{yyyyMM}/{userId}/{uuid}.{ext}
例：/dev/202607/1001/a1b2c3d4.jpg

缩略图策略：
- 列表页：200x200 (缩略图)
- 详情页：原图（前端懒加载）
```

---

### 5.3 模块依赖关系

```
                        ┌─────────────┐
                        │  管理模块    │
                        │   admin     │
                        └──────┬──────┘
                               │ 管理所有模块
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌──────────────┐     ┌──────────────┐      ┌──────────────┐
│   用户模块    │     │   商品模块    │      │   订单模块    │
│    user      │◄────│   product    │─────►│    order     │
└──────┬───────┘     └──────┬───────┘      └──────┬───────┘
       │                    │                     │
       │            ┌───────┼───────┐             │
       │            ▼       ▼       ▼             │
       │     ┌──────────┐ ┌──────────┐ ┌──────────┐
       │     │ 搜索模块  │ │ 收藏模块  │ │ 私信模块  │
       │     │ search   │ │ favorite │ │  chat    │
       │     └──────────┘ └──────────┘ └──────────┘
       │                                          │
       └──────────────┬───────────────────────────┘
                      │
                      ▼
              ┌──────────────┐
              │   文件模块    │
              │    file      │
              └──────────────┘

        ┌──────────────┐
        │   求购模块    │ ──► 依赖商品模块（匹配）
        │   wanted     │
        └──────────────┘
```

### 5.4 开发排期建议

| 阶段 | 周期 | 内容 |
|------|------|------|
| 第1周 | 基础搭建 | 项目脚手架、数据库设计、用户模块(注册登录认证) |
| 第2周 | 核心功能 | 商品模块(发布/编辑/列表/详情)、文件模块(图片上传) |
| 第3周 | 交易闭环 | 订单模块(下单/状态流转)、收藏模块 |
| 第4周 | 互动体验 | 私信模块(WebSocket聊天)、搜索模块 |
| 第5周 | 管理后台 | 审核、黑名单、类目管理、数据看板 |
| 第6周 | 增值+联调 | 求购模块、前后端联调、Bug修复 |
| 第7周 | 测试上线 | 功能测试、性能测试、部署上线 |

---

## 6. 数据库设计概要

### 6.1 E-R 核心关系

```
  ┌──────────┐         ┌──────────┐         ┌──────────┐
  │   user   │ 1─────N │ product  │ 1─────N │ product  │
  │          │         │          │         │ _image   │
  └──────────┘         └──────────┘         └──────────┘
       │                    │
       │ 1                  │ 1
       │                    │
       ├────── N ───────────┤
       │                    │
  ┌──────────┐         ┌──────────┐         ┌──────────┐
  │ favorite │         │  order   │ N─────1 │  order   │
  │          │         │          │         │ (买家)   │
  └──────────┘         └──────────┘         └──────────┘
       │                                       │
       │ N                                     │ N
       │                                       │
  ┌──────────┐                            ┌──────────┐
  │  order   │                            │  user    │
  │ (卖家)   │                            │          │
  └──────────┘                            └──────────┘
```

### 6.2 核心表清单

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| user | 用户表 | id, phone, student_id, real_name, role, status, created_at |
| user_auth | 认证记录 | id, user_id, auth_type, auth_status |
| user_address | 收货地址 | id, user_id, contact, phone, address_detail |
| category | 商品类目 | id, name, parent_id, sort, icon |
| product | 商品表 | id, user_id, category_id, title, description, price, original_price, condition, trade_type, status, created_at |
| product_image | 商品图片 | id, product_id, url, sort, is_main |
| favorite | 收藏 | id, user_id, product_id, created_at |
| `order` | 订单 | id, buyer_id, seller_id, product_id, status, created_at |
| conversation | 会话 | id, product_id, buyer_id, seller_id, last_message_at |
| message | 消息 | id, conversation_id, sender_id, content, created_at |
| wanted_post | 求购 | id, user_id, category_id, title, description, min_price, max_price, status |
| blacklist | 黑名单 | id, user_id, reason, operator_id, created_at |
| audit_log | 审核记录 | id, product_id, operator_id, result, reason, created_at |

---

## 7. 接口规范

### 7.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1717507200000
}
```

### 7.2 状态码规范

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 业务冲突（如重复下单） |
| 500 | 服务器错误 |

### 7.3 分页请求/响应

```json
// 请求
GET /api/product?page=1&size=20&categoryId=1&sort=latest

// 响应
{
  "code": 200,
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

---

## 8. 部署方案

### 8.1 环境规划

| 环境 | 用途 | 配置 |
|------|------|------|
| 开发环境 (dev) | 本地开发 | 单机，H2/本地MySQL |
| 测试环境 (test) | 集成测试 | 单机，Docker Compose |
| 生产环境 (prod) | 正式上线 | 云服务器或校内服务器 |

### 8.2 Docker Compose 部署

```yaml
# 简化部署方案
services:
  mysql:
    image: mysql:8.0
  redis:
    image: redis:7-alpine
  minio:
    image: minio/minio
  app:
    image: campus-trade:latest
    ports:
      - "8080:8080"
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
```

### 8.3 Nginx 路由规则

```
/                   → 前端静态资源 (user-app)
/admin              → 管理端静态资源 (admin-app)
/api/               → Spring Boot 后端 (8080)
/ws/                → WebSocket (8080)
/file/              → MinIO 图片服务 (9000)
```

---

> **文档维护说明**：本文档为 v1.0 初始版本，后续随项目迭代持续更新。各模块的详细接口定义见 `doc/API文档.md`。
