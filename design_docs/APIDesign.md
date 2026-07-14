# 校园闲置物品智慧流转平台 API 设计文档

> 版本：v1.0  
> 日期：2026-07-09  
> 依据文档：`PRD.md`、`TechnicalDesign.md`、`Database.md`

---

## 目录

1. [接口规范总则](#1-接口规范总则)
2. [用户模块](#2-用户模块)
3. [商品类目模块](#3-商品类目模块)
4. [商品模块](#4-商品模块)
5. [收藏模块](#5-收藏模块)
6. [订单模块](#6-订单模块)
7. [私信模块](#7-私信模块)
8. [求购模块](#8-求购模块)
9. [文件上传模块](#9-文件上传模块)
10. [管理端模块](#10-管理端模块)
11. [WebSocket 接口](#11-websocket-接口)

---

## 1. 接口规范总则

### 1.1 基础路径

| 端 | 基础路径 |
|----|----------|
| 用户端 / 管理端 API | `/api` |
| WebSocket | `/ws` |

### 1.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1717507200000
}
```

### 1.3 HTTP 状态码

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 请求正常处理 |
| 400 | 参数错误 | 请求参数校验失败 |
| 401 | 未认证 | 未登录或 Token 过期 |
| 403 | 无权限 | 角色/权限不足 |
| 404 | 资源不存在 | 目标数据不存在 |
| 409 | 业务冲突 | 如重复下单、不可流转的状态转换 |
| 500 | 服务器内部错误 | 系统异常 |

### 1.4 鉴权方式

除「注册」「登录」「发送短信验证码」「公开商品列表/详情」「公开类目列表」外，所有接口需在请求头携带 JWT Token：

```
Authorization: Bearer <token>
```

角色权限约定：

- `USER`：普通用户，可访问用户端全部接口
- `ADMIN` / `SUPER_ADMIN`：管理员，可访问管理端接口；SUPER_ADMIN 额外拥有管理员账号管理权限

### 1.5 分页参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页条数，最大 100 |

分页响应体：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

---

## 2. 用户模块

### 2.1 发送短信验证码

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/user/sms/send` |
| **鉴权** | 否 |
| **描述** | 向指定手机号发送短信验证码，用于注册、登录、换绑手机号 |

**请求体：**

```json
{
  "phone": "13800000000",
  "scene": "REGISTER"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | 是 | 手机号，11位 |
| scene | String | 是 | 场景：`REGISTER` / `LOGIN` / `CHANGE_PHONE` |

**响应 data：** `null`

**业务规则：** 同一手机号同一场景 60 秒内不可重复发送，验证码有效期 5 分钟。

---

### 2.2 手机号注册

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/user/register` |
| **鉴权** | 否 |
| **描述** | 使用手机号+短信验证码注册新账号 |

**请求体：**

```json
{
  "phone": "13800000000",
  "smsCode": "123456",
  "password": "Abc@123456",
  "nickname": "张三"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | 是 | 手机号 |
| smsCode | String | 是 | 短信验证码 |
| password | String | 是 | 密码，6-20位 |
| nickname | String | 否 | 昵称，默认使用脱敏手机号 |

**响应 data：**

```json
{
  "userId": 1,
  "phone": "138****0000",
  "nickname": "张三",
  "role": "USER",
  "status": "ACTIVE",
  "studentVerified": false,
  "token": "eyJhbGciOiJI..."
}
```

**业务规则：** 注册后自动写入 `user_auth` 表 PHONE 类型认证记录，`studentVerified` 为 false 时不可发布商品与交易。

---

### 2.3 手机号+密码登录

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/user/login` |
| **鉴权** | 否 |
| **描述** | 手机号+密码登录 |

**请求体：**

```json
{
  "phone": "13800000000",
  "password": "Abc@123456"
}
```

**响应 data：** 同注册返回的用户信息及 Token。

---

### 2.4 短信验证码登录

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/user/login/sms` |
| **鉴权** | 否 |
| **描述** | 手机号+短信验证码登录，无需密码 |

**请求体：**

```json
{
  "phone": "13800000000",
  "smsCode": "123456"
}
```

**响应 data：** 同注册返回的用户信息及 Token。

---

### 2.5 获取当前用户信息

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/user/profile` |
| **鉴权** | 是 |
| **描述** | 获取当前登录用户的完整信息 |

**响应 data：**

```json
{
  "id": 1,
  "phone": "138****0000",
  "nickname": "张三",
  "avatar": "/file/avatar/1.jpg",
  "gender": 0,
  "school": "XX大学",
  "studentId": "20210001",
  "realName": "张三",
  "role": "USER",
  "status": "ACTIVE",
  "studentVerified": true,
  "createdAt": "2026-07-01T12:00:00Z"
}
```

---

### 2.6 修改个人信息

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/user/profile` |
| **鉴权** | 是 |
| **描述** | 修改昵称、头像等个人资料 |

**请求体：**

```json
{
  "nickname": "张三丰",
  "avatar": "/file/avatar/2.jpg",
  "gender": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | String | 否 | 昵称 |
| avatar | String | 否 | 头像 URL（需先通过文件上传接口获取） |
| gender | Integer | 否 | 0-未知 1-男 2-女 |

**响应 data：** 更新后的用户信息。

---

### 2.7 修改密码

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/user/password` |
| **鉴权** | 是 |
| **描述** | 修改登录密码 |

**请求体：**

```json
{
  "oldPassword": "Abc@123456",
  "newPassword": "Xyz@654321"
}
```

**响应 data：** `null`

---

### 2.8 更换绑定手机号

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/user/phone` |
| **鉴权** | 是 |
| **描述** | 更换已绑定的手机号 |

**请求体：**

```json
{
  "newPhone": "13900000000",
  "smsCode": "654321"
}
```

**响应 data：** 更新后的用户信息。

---

### 2.9 学号实名认证

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/user/auth/student` |
| **鉴权** | 是 |
| **描述** | 提交学号、姓名进行校园身份认证 |

**请求体：**

```json
{
  "studentId": "20210001",
  "realName": "张三"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| studentId | String | 是 | 学号 |
| realName | String | 是 | 真实姓名 |

**响应 data：**

```json
{
  "authStatus": "PENDING"
}
```

**业务规则：**

- 提交后写入 `user_auth` 表，`auth_type=STUDENT_ID`，`auth_status=PENDING`
- MVP 阶段可默认为 `VERIFIED`（对接学校系统后改为异步校验）
- 认证通过后 `app_user.student_id`、`app_user.real_name` 回填

---

### 2.10 收货地址列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/user/address` |
| **鉴权** | 是 |
| **描述** | 获取当前用户所有收货地址 |

**响应 data：**

```json
[
  {
    "id": 1,
    "contact": "张三",
    "phone": "13800000000",
    "province": "",
    "city": "",
    "district": "",
    "detail": "北校区 12号宿舍楼 301室",
    "isDefault": true,
    "createdAt": "2026-07-01T12:00:00Z"
  }
]
```

---

### 2.11 新增收货地址

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/user/address` |
| **鉴权** | 是 |
| **描述** | 新增收货地址，最多 10 个 |

**请求体：**

```json
{
  "contact": "张三",
  "phone": "13800000000",
  "detail": "北校区 12号宿舍楼 301室",
  "isDefault": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contact | String | 是 | 联系人 |
| phone | String | 是 | 联系电话 |
| detail | String | 是 | 详细地址（校区+楼栋+宿舍号） |
| isDefault | Boolean | 否 | 是否默认，默认 false |

**响应 data：** 新增的地址对象。

---

### 2.12 修改收货地址

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/user/address/{id}` |
| **鉴权** | 是 |
| **描述** | 修改指定收货地址 |

**请求体：** 同新增。

**响应 data：** 更新后的地址对象。

---

### 2.13 删除收货地址

| 项目 | 说明 |
|------|------|
| **接口** | `DELETE /api/user/address/{id}` |
| **鉴权** | 是 |
| **描述** | 删除指定收货地址（软删除，设置 deleted_at） |

**响应 data：** `null`

---

### 2.14 设置默认收货地址

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/user/address/{id}/default` |
| **鉴权** | 是 |
| **描述** | 将指定地址设为默认地址（其余地址取消默认） |

**响应 data：** `null`

---

## 3. 商品类目模块

### 3.1 获取类目树

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/category` |
| **鉴权** | 否 |
| **描述** | 获取所有可见的类目树（一级+二级），按 sort_order 排序 |

**响应 data：**

```json
[
  {
    "id": 1,
    "name": "教材",
    "icon": "book",
    "sortOrder": 10,
    "children": [
      {
        "id": 6,
        "name": "考研资料",
        "parentId": 1,
        "icon": null,
        "sortOrder": 10
      }
    ]
  },
  {
    "id": 2,
    "name": "数码",
    "icon": "device",
    "sortOrder": 20,
    "children": []
  }
]
```

---

## 4. 商品模块

### 4.1 商品列表（搜索+筛选）

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/product` |
| **鉴权** | 否 |
| **描述** | 分页查询在售商品，支持关键词搜索与多条件筛选 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词，匹配标题、描述 |
| categoryId | Long | 否 | 类目 ID（二级类目） |
| parentCategoryId | Long | 否 | 一级类目 ID（查询其下所有二级类目商品） |
| minPrice | BigDecimal | 否 | 最低价格 |
| maxPrice | BigDecimal | 否 | 最高价格 |
| conditionLevel | String | 否 | 成色：`NEW` / `LIKE_NEW` / `USED` / `OLD` |
| tradeType | String | 否 | 交易方式：`PICKUP` / `DELIVERY` / `BOTH` |
| sort | String | 否 | 排序：`latest`(默认) / `price_asc` / `price_desc` |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20 |

**响应 data：**

```json
{
  "records": [
    {
      "id": 1,
      "title": "高等数学第七版",
      "mainImage": "/file/product/uuid/1.jpg",
      "price": 15.00,
      "conditionLevel": "USED",
      "tradeType": "PICKUP",
      "seller": {
        "id": 10,
        "nickname": "张三",
        "avatar": "/file/avatar/1.jpg"
      },
      "categoryName": "教材",
      "favoriteCount": 3,
      "createdAt": "2026-07-05T10:00:00Z"
    }
  ],
  "total": 100,
  "page": 1,
  "size": 20,
  "pages": 5
}
```

---

### 4.2 商品详情

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/product/{id}` |
| **鉴权** | 否 |
| **描述** | 获取商品完整详情，含图片列表、卖家信息 |

**响应 data：**

```json
{
  "id": 1,
  "title": "高等数学第七版",
  "description": "几乎全新，只用了一学期，无笔记",
  "price": 15.00,
  "originalPrice": 48.00,
  "conditionLevel": "USED",
  "tradeType": "PICKUP",
  "tradeRemark": "北校区图书馆门口自提",
  "status": "ON_SALE",
  "viewCount": 120,
  "favoriteCount": 3,
  "categoryId": 1,
  "categoryName": "教材",
  "images": [
    { "id": 1, "url": "/file/product/uuid/1.jpg", "isMain": true, "sort": 1 },
    { "id": 2, "url": "/file/product/uuid/2.jpg", "isMain": false, "sort": 2 }
  ],
  "seller": {
    "id": 10,
    "nickname": "张三",
    "avatar": "/file/avatar/1.jpg",
    "soldCount": 5
  },
  "createdAt": "2026-07-05T10:00:00Z",
  "updatedAt": "2026-07-05T12:00:00Z"
}
```

---

### 4.3 发布商品

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/product` |
| **鉴权** | 是 |
| **描述** | 发布闲置商品，需完成学号认证，发布后状态为 `PENDING_REVIEW` |

**请求体：**

```json
{
  "categoryId": 1,
  "title": "高等数学第七版",
  "description": "几乎全新，只用了一学期",
  "price": 15.00,
  "originalPrice": 48.00,
  "conditionLevel": "USED",
  "tradeType": "PICKUP",
  "tradeRemark": "北校区图书馆门口自提",
  "images": [
    { "url": "/file/product/uuid/1.jpg", "isMain": true, "sort": 1 },
    { "url": "/file/product/uuid/2.jpg", "isMain": false, "sort": 2 }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Long | 是 | 类目 ID |
| title | String | 是 | 商品标题，2-100 字符 |
| description | String | 是 | 商品描述，10-2000 字符 |
| price | BigDecimal | 是 | 售价，>0 |
| originalPrice | BigDecimal | 否 | 原价 |
| conditionLevel | String | 是 | 成色 |
| tradeType | String | 是 | 交易方式 |
| tradeRemark | String | 否 | 交易备注 |
| images | Array | 是 | 图片数组，至少 1 张 |

**响应 data：** 创建后的商品详情。

**业务规则：**

- 未通过学号认证返回 403
- 被加入黑名单返回 403
- 同一用户同时最多发布 50 件在售/待审商品

---

### 4.4 修改商品信息

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/product/{id}` |
| **鉴权** | 是 |
| **描述** | 修改已发布但未售出的商品信息，仅本人可操作 |

**请求体：** 同发布商品。

**响应 data：** 更新后的商品详情。

**业务规则：** 状态为 `ON_SALE` / `PENDING_REVIEW` / `REJECTED` / `OFF_SHELF` 的商品可编辑。

---

### 4.5 商品上架/下架

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/product/{id}/shelf` |
| **鉴权** | 是 |
| **描述** | 切换商品的上架/下架状态 |

**请求体：**

```json
{
  "onShelf": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| onShelf | Boolean | 是 | true-上架（OFF_SHELF→ON_SALE），false-下架（ON_SALE→OFF_SHELF） |

**响应 data：** 更新后的商品状态。

---

### 4.6 删除商品

| 项目 | 说明 |
|------|------|
| **接口** | `DELETE /api/product/{id}` |
| **鉴权** | 是 |
| **描述** | 下架或已售出商品可软删除（设置 deleted_at），仅本人可操作 |

**响应 data：** `null`

---

### 4.7 我的发布

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/product/my` |
| **鉴权** | 是 |
| **描述** | 分页查询当前用户发布的所有商品 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 按状态筛选 |

**响应 data：** 分页商品列表（同商品列表格式）。

---

## 5. 收藏模块

### 5.1 添加收藏

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/favorite/{productId}` |
| **鉴权** | 是 |
| **描述** | 收藏指定商品，不可收藏自己的商品 |

**响应 data：** `null`

**错误码：** 重复收藏返回 409。

---

### 5.2 取消收藏

| 项目 | 说明 |
|------|------|
| **接口** | `DELETE /api/favorite/{productId}` |
| **鉴权** | 是 |
| **描述** | 取消对指定商品的收藏 |

**响应 data：** `null`

---

### 5.3 我的收藏列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/favorite` |
| **鉴权** | 是 |
| **描述** | 分页查询当前用户的收藏商品，支持按类目筛选 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Long | 否 | 按类目筛选 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

**响应 data：**

```json
{
  "records": [
    {
      "id": 1,
      "product": {
        "id": 10,
        "title": "高等数学第七版",
        "mainImage": "/file/product/uuid/1.jpg",
        "price": 15.00,
        "status": "ON_SALE",
        "createdAt": "2026-07-05T10:00:00Z"
      },
      "createdAt": "2026-07-06T08:00:00Z"
    }
  ],
  "total": 20,
  "page": 1,
  "size": 20,
  "pages": 1
}
```

---

### 5.4 检查收藏状态

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/favorite/check/{productId}` |
| **鉴权** | 是 |
| **描述** | 检查当前用户是否已收藏某商品（用于商品详情页渲染收藏按钮） |

**响应 data：**

```json
{
  "favorited": true
}
```

---

## 6. 订单模块

### 6.1 创建订单

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/order` |
| **鉴权** | 是 |
| **描述** | 买家对指定商品创建订单，订单状态初始为 `PENDING_COMMUNICATION` |

**请求体：**

```json
{
  "productId": 1,
  "addressId": 3,
  "remark": "可以小刀吗"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | Long | 是 | 商品 ID |
| addressId | Long | 否 | 收货地址 ID（交易方式为 DELIVERY 时必填） |
| remark | String | 否 | 买家备注，200 字符内 |

**响应 data：**

```json
{
  "id": 10,
  "orderNo": "202607091530001",
  "productId": 1,
  "productTitle": "高等数学第七版",
  "buyerId": 5,
  "sellerId": 10,
  "status": "PENDING_COMMUNICATION",
  "price": 15.00,
  "createdAt": "2026-07-09T15:30:00Z"
}
```

**业务规则：**

- 未认证返回 403
- 不可购买自己的商品，返回 409
- 商品状态非 `ON_SALE` 返回 409
- 同一买家同一商品已有未完成订单返回 409

---

### 6.2 我的订单列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/order` |
| **鉴权** | 是 |
| **描述** | 分页查询当前用户相关的订单（作为买家或卖家） |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| role | String | 否 | 角色：`buyer`(默认) / `seller` |
| status | String | 否 | 订单状态筛选 |

**响应 data：**

```json
{
  "records": [
    {
      "id": 10,
      "orderNo": "202607091530001",
      "productId": 1,
      "productTitle": "高等数学第七版",
      "productImage": "/file/product/uuid/1.jpg",
      "price": 15.00,
      "tradeType": "PICKUP",
      "status": "PENDING_COMMUNICATION",
      "buyer": { "id": 5, "nickname": "李四", "avatar": "...", "phone": "139****0000" },
      "seller": { "id": 10, "nickname": "张三", "avatar": "...", "phone": "138****0000" },
      "address": { "contact": "李四", "phone": "13900000000", "detail": "..." },
      "buyerRemark": "可以小刀吗",
      "pickupTime": null,
      "pickupLocation": null,
      "completedAt": null,
      "cancelledAt": null,
      "cancelReason": null,
      "createdAt": "2026-07-09T15:30:00Z"
    }
  ],
  "total": 5,
  "page": 1,
  "size": 20,
  "pages": 1
}
```

---

### 6.3 订单详情

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/order/{id}` |
| **鉴权** | 是 |
| **描述** | 获取指定订单详情，仅买卖双方可查看 |

**响应 data：** 同订单列表中的单条记录结构。

---

### 6.4 更新订单状态

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/order/{id}/status` |
| **鉴权** | 是 |
| **描述** | 买卖双方确认交易时间地点后，将订单状态推进为 `PENDING_PICKUP`；确认收货后推进为 `COMPLETED` |

**请求体：**

```json
{
  "action": "CONFIRM_PICKUP",
  "pickupTime": "2026-07-10 14:00",
  "pickupLocation": "北校区图书馆门口"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| action | String | 是 | `CONFIRM_PICKUP`：确认自提时间地点（PENDING_COMMUNICATION→PENDING_PICKUP）<br>`COMPLETE`：确认收货完成（PENDING_PICKUP→COMPLETED） |
| pickupTime | String | 否 | CONFIRM_PICKUP 时必填，约定时间 |
| pickupLocation | String | 否 | CONFIRM_PICKUP 时必填，约定地点 |

**响应 data：** 更新后的订单详情。

**业务规则：**

- `CONFIRM_PICKUP`：买卖双方均可操作
- `COMPLETE`：仅买家可操作，完成后商品状态变为 `SOLD`
- 非法状态流转返回 409

---

### 6.5 取消订单

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/order/{id}/cancel` |
| **鉴权** | 是 |
| **描述** | 在 PENDING_COMMUNICATION 或 PENDING_PICKUP 状态下买卖双方均可取消 |

**请求体：**

```json
{
  "cancelReason": "临时不需要了"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cancelReason | String | 否 | 取消原因，200 字符 |

**响应 data：** 更新后的订单详情。

**业务规则：** 订单状态为 `COMPLETED` 或 `CANCELLED` 时不可取消，返回 409。

---

## 7. 私信模块

### 7.1 会话列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/conversation` |
| **鉴权** | 是 |
| **描述** | 分页查询当前用户参与的所有会话，按最后消息时间倒序 |

**响应 data：**

```json
{
  "records": [
    {
      "id": 5,
      "productId": 1,
      "productTitle": "高等数学第七版",
      "productImage": "/file/product/uuid/1.jpg",
      "productPrice": 15.00,
      "otherUser": {
        "id": 5,
        "nickname": "李四",
        "avatar": "/file/avatar/1.jpg"
      },
      "lastMessage": "还在吗？",
      "lastMessageTime": "2026-07-09T14:00:00Z",
      "unreadCount": 2,
      "status": "ACTIVE",
      "createdAt": "2026-07-08T10:00:00Z"
    }
  ],
  "total": 3,
  "page": 1,
  "size": 20,
  "pages": 1
}
```

---

### 7.2 创建/获取会话

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/conversation` |
| **鉴权** | 是 |
| **描述** | 买家对某商品发起与卖家的私信会话。同一商品+同一买家+同一卖家仅存在一个会话，若已存在则直接返回 |

**请求体：**

```json
{
  "productId": 1
}
```

**响应 data：** 会话详情（结构同列表中的单条）。

**业务规则：** 不可与自己发起会话，返回 409。

---

### 7.3 会话详情

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/conversation/{id}` |
| **鉴权** | 是 |
| **描述** | 获取会话元信息（商品、对方用户信息），仅参与者可查看 |

**响应 data：** 同列表中的单条结构。

---

### 7.4 获取消息列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/conversation/{id}/messages` |
| **鉴权** | 是 |
| **描述** | 分页查询指定会话的消息记录，按时间升序。调用后自动把对方消息标记为已读 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数，默认 50 |
| beforeId | Long | 否 | 消息游标，查询该 ID 之前的消息（上拉加载更多） |

**响应 data：**

```json
{
  "records": [
    {
      "id": 101,
      "conversationId": 5,
      "senderId": 10,
      "content": "你好，这个还在吗？",
      "messageType": "TEXT",
      "createdAt": "2026-07-09T14:00:00Z"
    }
  ],
  "total": 20,
  "page": 1,
  "size": 50,
  "pages": 1
}
```

---

### 7.5 发送消息

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/conversation/{id}/message` |
| **鉴权** | 是 |
| **描述** | 向指定会话发送文字消息（HTTP 降级方案，同时也支持 WebSocket） |

**请求体：**

```json
{
  "content": "你好，还在吗？"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 是 | 消息内容，1-2000 字符 |

**响应 data：** 发送后的消息对象。

**业务规则：** 会话非参与者返回 403；会话已关闭返回 409。

---

## 8. 求购模块

### 8.1 求购列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/wanted` |
| **鉴权** | 否 |
| **描述** | 分页查询公开的求购信息，支持关键词搜索与类目筛选 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词 |
| categoryId | Long | 否 | 类目筛选 |
| parentCategoryId | Long | 否 | 一级类目筛选 |
| sort | String | 否 | `latest` / `expiring_soon` |
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

**响应 data：**

```json
{
  "records": [
    {
      "id": 1,
      "title": "求购二手考研数学教材",
      "description": "最好是张宇的版本",
      "categoryId": 1,
      "categoryName": "教材",
      "minPrice": 10.00,
      "maxPrice": 30.00,
      "status": "OPEN",
      "validUntil": "2026-07-20T00:00:00Z",
      "user": {
        "id": 5,
        "nickname": "李四",
        "avatar": "/file/avatar/2.jpg"
      },
      "contactPhone": "139****0000",
      "createdAt": "2026-07-09T10:00:00Z"
    }
  ],
  "total": 8,
  "page": 1,
  "size": 20,
  "pages": 1
}
```

---

### 8.2 求购详情

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/wanted/{id}` |
| **鉴权** | 否 |
| **描述** | 获取求购信息完整详情，含匹配到的在售商品推荐 |

**响应 data：** 包含 `matchedProducts` 数组（最多 5 条，按匹配度排序）。

```json
{
  "id": 1,
  "title": "求购二手考研数学教材",
  "description": "最好是张宇的版本",
  "categoryId": 1,
  "categoryName": "教材",
  "minPrice": 10.00,
  "maxPrice": 30.00,
  "status": "OPEN",
  "validUntil": "2026-07-20T00:00:00Z",
  "user": { "id": 5, "nickname": "李四", "avatar": "..." },
  "contactPhone": "139****0000",
  "matchedProducts": [
    {
      "id": 10,
      "title": "张宇考研数学教材",
      "price": 20.00,
      "mainImage": "...",
      "conditionLevel": "USED"
    }
  ],
  "createdAt": "2026-07-09T10:00:00Z"
}
```

---

### 8.3 发布求购

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/wanted` |
| **鉴权** | 是 |
| **描述** | 发布求购需求，需完成学号认证 |

**请求体：**

```json
{
  "categoryId": 1,
  "title": "求购二手考研数学教材",
  "description": "最好是张宇的版本",
  "minPrice": 10.00,
  "maxPrice": 30.00,
  "validUntil": "2026-07-20T00:00:00Z",
  "contactPhone": "13900000000"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Long | 是 | 类目 ID |
| title | String | 是 | 需求标题 |
| description | String | 否 | 详细描述 |
| minPrice | BigDecimal | 否 | 预期最低价 |
| maxPrice | BigDecimal | 否 | 预期最高价 |
| validUntil | String | 是 | 有效期截止时间 |
| contactPhone | String | 否 | 联系方式，默认使用注册手机号 |

**响应 data：** 创建后的求购详情。

---

### 8.4 修改求购

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/wanted/{id}` |
| **鉴权** | 是 |
| **描述** | 修改本人发布的求购信息，仅 OPEN 状态可修改 |

**请求体：** 同发布求购。

**响应 data：** 更新后的求购详情。

---

### 8.5 关闭求购

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/wanted/{id}/close` |
| **鉴权** | 是 |
| **描述** | 手动关闭求购，状态变为 CLOSED |

**响应 data：** 更新后的求购详情。

---

### 8.6 删除求购

| 项目 | 说明 |
|------|------|
| **接口** | `DELETE /api/wanted/{id}` |
| **鉴权** | 是 |
| **描述** | 软删除本人发布的求购信息 |

**响应 data：** `null`

---

### 8.7 我的求购

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/wanted/my` |
| **鉴权** | 是 |
| **描述** | 分页查询当前用户发布的所有求购信息 |

**响应 data：** 分页求购列表。

---

## 9. 文件上传模块

### 9.1 上传文件

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/file/upload` |
| **鉴权** | 是 |
| **描述** | 上传图片文件，返回可访问的 URL |

**请求方式：** `multipart/form-data`

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件 |
| type | String | 是 | 用途类型：`product`(商品图) / `avatar`(头像) |

**响应 data：**

```json
{
  "url": "/file/product/uuid/abc123.jpg"
}
```

**业务规则：**

- 支持格式：jpg / jpeg / png / webp
- 单文件大小 ≤ 10MB
- 用户头像仅保留最新一张

---

## 10. 管理端模块

> 管理端所有接口需 `role` 为 `ADMIN` 或 `SUPER_ADMIN`。

### 10.1 商品审核列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/admin/product/review` |
| **鉴权** | 是（管理员） |
| **描述** | 分页获取待审核/已审核的商品列表 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 商品状态筛选，默认 `PENDING_REVIEW` |
| keyword | String | 否 | 搜索关键词 |
| categoryId | Long | 否 | 类目筛选 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

**响应 data：** 分页商品列表（含卖家信息、图片）。

---

### 10.2 商品审核

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/admin/product/{id}/audit` |
| **鉴权** | 是（管理员） |
| **描述** | 对 PENDING_REVIEW 状态的商品进行审核 |

**请求体：**

```json
{
  "action": "APPROVE",
  "reason": ""
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| action | String | 是 | `APPROVE`：通过（→ON_SALE）；`REJECT`：驳回（→REJECTED） |
| reason | String | 否 | 驳回原因（REJECT 时必填） |

**响应 data：** 更新后的商品状态。

**业务规则：** 写入 `audit_log` 审核记录。

---

### 10.3 违规下架

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/admin/product/{id}/violation` |
| **鉴权** | 是（管理员） |
| **描述** | 将 ON_SALE 状态商品标记为违规并强制下架 |

**请求体：**

```json
{
  "reason": "发布违禁商品"
}
```

**响应 data：** `null`

**业务规则：** 商品状态变为 `VIOLATION_DELISTED`，写入 `audit_log`。

---

### 10.4 解除违规

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/admin/product/{id}/relieve` |
| **鉴权** | 是（管理员） |
| **描述** | 解除商品违规状态，恢复为 ON_SALE |

**请求体：**

```json
{
  "reason": "误判，经核实商品合规"
}
```

**响应 data：** `null`

**业务规则：** 仅 `VIOLATION_DELISTED` 状态可操作。

---

### 10.5 用户列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/admin/user` |
| **鉴权** | 是（管理员） |
| **描述** | 分页查询平台用户，支持按学号、昵称搜索 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 学号或昵称 |
| status | String | 否 | 状态筛选 |
| role | String | 否 | 角色筛选 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

**响应 data：**

```json
{
  "records": [
    {
      "id": 1,
      "phone": "138****0000",
      "nickname": "张三",
      "studentId": "20210001",
      "realName": "张三",
      "role": "USER",
      "status": "ACTIVE",
      "studentVerified": true,
      "productCount": 3,
      "orderCount": 2,
      "createdAt": "2026-07-01T12:00:00Z"
    }
  ],
  "total": 50,
  "page": 1,
  "size": 20,
  "pages": 3
}
```

---

### 10.6 加入黑名单

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/admin/user/{id}/blacklist` |
| **鉴权** | 是（管理员） |
| **描述** | 将指定用户加入黑名单 |

**请求体：**

```json
{
  "reason": "多次发布违规商品"
}
```

**响应 data：** `null`

**业务规则：**

- 不可拉黑管理员，返回 409
- 已在黑名单中（ACTIVE）返回 409
- 用户状态变为 `BLACKLISTED`，写入 `blacklist` 表
- 该用户正在出售的商品统一下架

---

### 10.7 解除黑名单

| 项目 | 说明 |
|------|------|
| **接口** | `DELETE /api/admin/user/{id}/blacklist` |
| **鉴权** | 是（管理员） |
| **描述** | 解除用户黑名单状态 |

**请求体：**

```json
{
  "reason": "用户申诉通过"
}
```

**响应 data：** `null`

**业务规则：** 用户状态恢复为 `ACTIVE`，blacklist 记录状态变更为 `REMOVED`。

---

### 10.8 黑名单列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/admin/blacklist` |
| **鉴权** | 是（管理员） |
| **描述** | 分页查询黑名单记录 |

**请求参数：** 支持 status 筛选。

**响应 data：** 分页黑名单记录（含被拉黑用户信息、操作人信息、原因、时间）。

---

### 10.9 类目管理列表

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/admin/category` |
| **鉴权** | 是（管理员） |
| **描述** | 获取全量类目树（含隐藏类目），用于后台管理 |

**响应 data：** 同公开类目树，额外返回 `isVisible` 字段。

---

### 10.10 新增类目

| 项目 | 说明 |
|------|------|
| **接口** | `POST /api/admin/category` |
| **鉴权** | 是（管理员） |
| **描述** | 新增一级或二级类目 |

**请求体：**

```json
{
  "name": "考研资料",
  "parentId": 1,
  "icon": "book",
  "sortOrder": 10,
  "isVisible": true
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 类目名称 |
| parentId | Long | 否 | 父类目 ID，为空表示一级类目 |
| icon | String | 否 | 图标标识 |
| sortOrder | Integer | 否 | 排序，默认 0 |
| isVisible | Boolean | 否 | 是否前台可见，默认 true |

**响应 data：** 新增的类目对象。

---

### 10.11 修改类目

| 项目 | 说明 |
|------|------|
| **接口** | `PUT /api/admin/category/{id}` |
| **鉴权** | 是（管理员） |
| **描述** | 修改类目名称、排序、可见性 |

**请求体：** 同新增。

**响应 data：** 更新后的类目对象。

---

### 10.12 删除类目

| 项目 | 说明 |
|------|------|
| **接口** | `DELETE /api/admin/category/{id}` |
| **鉴权** | 是（管理员） |
| **描述** | 删除类目（级联软删除子类目） |

**响应 data：** `null`

**业务规则：** 类目下存在关联商品时返回 409，不允许删除。

---

### 10.13 数据看板 - 概览

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/admin/dashboard/overview` |
| **鉴权** | 是（管理员） |
| **描述** | 获取平台核心统计数据 |

**响应 data：**

```json
{
  "totalUsers": 1200,
  "totalProducts": 350,
  "totalOrders": 200,
  "completedOrders": 150,
  "todayNewUsers": 15,
  "todayNewProducts": 8,
  "todayNewOrders": 12,
  "pendingReviewProducts": 5
}
```

---

### 10.14 数据看板 - 趋势统计

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/admin/dashboard/trends` |
| **鉴权** | 是（管理员） |
| **描述** | 获取商品发布量/订单成交量趋势数据 |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| period | String | 否 | 维度：`DAILY`(按日，默认) / `WEEKLY`(按周) / `MONTHLY`(按月) |
| days | int | 否 | 回溯天数，默认 30 |

**响应 data：**

```json
{
  "productTrend": [
    { "date": "2026-07-01", "count": 12 },
    { "date": "2026-07-02", "count": 8 }
  ],
  "orderTrend": [
    { "date": "2026-07-01", "count": 5 },
    { "date": "2026-07-02", "count": 3 }
  ]
}
```

---

### 10.15 数据看板 - 类目统计

| 项目 | 说明 |
|------|------|
| **接口** | `GET /api/admin/dashboard/category-stats` |
| **鉴权** | 是（管理员） |
| **描述** | 商品类目占比统计 |

**响应 data：**

```json
[
  { "categoryId": 1, "categoryName": "教材", "count": 120, "ratio": 0.34 },
  { "categoryId": 2, "categoryName": "数码", "count": 80, "ratio": 0.23 },
  { "categoryId": 3, "categoryName": "宿舍用品", "count": 60, "ratio": 0.17 },
  { "categoryId": 4, "categoryName": "运动器材", "count": 50, "ratio": 0.14 },
  { "categoryId": 5, "categoryName": "其他", "count": 40, "ratio": 0.11 }
]
```

---

## 11. WebSocket 接口

### 11.1 连接

| 项目 | 说明 |
|------|------|
| **端点** | `ws://host:port/ws/chat` |
| **鉴权** | 是，连接时通过 `token` 参数传递 JWT |
| **协议** | STOMP over WebSocket |

### 11.2 消息目的地

| 目的地 | 方向 | 说明 |
|--------|------|------|
| `/app/chat.send/{conversationId}` | 客户端→服务端 | 向指定会话发送私信 |
| `/topic/chat.{conversationId}` | 服务端→客户端 | 接收指定会话的实时消息 |
| `/user/queue/chat.new` | 服务端→客户端 | 收到新会话通知 |

### 11.3 消息体格式

```json
{
  "id": 101,
  "conversationId": 5,
  "senderId": 10,
  "senderNickname": "张三",
  "senderAvatar": "/file/avatar/1.jpg",
  "content": "你好，还在吗？",
  "messageType": "TEXT",
  "createdAt": "2026-07-09T14:00:00Z"
}
```

---

## 附录 A：接口一览表

| 序号 | 模块 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|------|
| 1 | 用户 | POST | `/api/user/sms/send` | 否 | 发送短信验证码 |
| 2 | 用户 | POST | `/api/user/register` | 否 | 手机号注册 |
| 3 | 用户 | POST | `/api/user/login` | 否 | 密码登录 |
| 4 | 用户 | POST | `/api/user/login/sms` | 否 | 验证码登录 |
| 5 | 用户 | GET | `/api/user/profile` | 是 | 获取个人信息 |
| 6 | 用户 | PUT | `/api/user/profile` | 是 | 修改个人信息 |
| 7 | 用户 | PUT | `/api/user/password` | 是 | 修改密码 |
| 8 | 用户 | PUT | `/api/user/phone` | 是 | 更换手机号 |
| 9 | 用户 | POST | `/api/user/auth/student` | 是 | 学号认证 |
| 10 | 用户 | GET | `/api/user/address` | 是 | 地址列表 |
| 11 | 用户 | POST | `/api/user/address` | 是 | 新增地址 |
| 12 | 用户 | PUT | `/api/user/address/{id}` | 是 | 修改地址 |
| 13 | 用户 | DELETE | `/api/user/address/{id}` | 是 | 删除地址 |
| 14 | 用户 | PUT | `/api/user/address/{id}/default` | 是 | 设置默认地址 |
| 15 | 类目 | GET | `/api/category` | 否 | 获取类目树 |
| 16 | 商品 | GET | `/api/product` | 否 | 商品列表 |
| 17 | 商品 | GET | `/api/product/{id}` | 否 | 商品详情 |
| 18 | 商品 | POST | `/api/product` | 是 | 发布商品 |
| 19 | 商品 | PUT | `/api/product/{id}` | 是 | 修改商品 |
| 20 | 商品 | PUT | `/api/product/{id}/shelf` | 是 | 上架/下架 |
| 21 | 商品 | DELETE | `/api/product/{id}` | 是 | 删除商品 |
| 22 | 商品 | GET | `/api/product/my` | 是 | 我的发布 |
| 23 | 收藏 | POST | `/api/favorite/{productId}` | 是 | 添加收藏 |
| 24 | 收藏 | DELETE | `/api/favorite/{productId}` | 是 | 取消收藏 |
| 25 | 收藏 | GET | `/api/favorite` | 是 | 收藏列表 |
| 26 | 收藏 | GET | `/api/favorite/check/{productId}` | 是 | 检查收藏状态 |
| 27 | 订单 | POST | `/api/order` | 是 | 创建订单 |
| 28 | 订单 | GET | `/api/order` | 是 | 订单列表 |
| 29 | 订单 | GET | `/api/order/{id}` | 是 | 订单详情 |
| 30 | 订单 | PUT | `/api/order/{id}/status` | 是 | 更新订单状态 |
| 31 | 订单 | PUT | `/api/order/{id}/cancel` | 是 | 取消订单 |
| 32 | 私信 | GET | `/api/conversation` | 是 | 会话列表 |
| 33 | 私信 | POST | `/api/conversation` | 是 | 创建会话 |
| 34 | 私信 | GET | `/api/conversation/{id}` | 是 | 会话详情 |
| 35 | 私信 | GET | `/api/conversation/{id}/messages` | 是 | 消息列表 |
| 36 | 私信 | POST | `/api/conversation/{id}/message` | 是 | 发送消息 |
| 37 | 求购 | GET | `/api/wanted` | 否 | 求购列表 |
| 38 | 求购 | GET | `/api/wanted/{id}` | 否 | 求购详情 |
| 39 | 求购 | POST | `/api/wanted` | 是 | 发布求购 |
| 40 | 求购 | PUT | `/api/wanted/{id}` | 是 | 修改求购 |
| 41 | 求购 | PUT | `/api/wanted/{id}/close` | 是 | 关闭求购 |
| 42 | 求购 | DELETE | `/api/wanted/{id}` | 是 | 删除求购 |
| 43 | 求购 | GET | `/api/wanted/my` | 是 | 我的求购 |
| 44 | 文件 | POST | `/api/file/upload` | 是 | 上传文件 |
| 45 | 管理 | GET | `/api/admin/product/review` | 是(管理员) | 商品审核列表 |
| 46 | 管理 | POST | `/api/admin/product/{id}/audit` | 是(管理员) | 商品审核 |
| 47 | 管理 | POST | `/api/admin/product/{id}/violation` | 是(管理员) | 违规下架 |
| 48 | 管理 | POST | `/api/admin/product/{id}/relieve` | 是(管理员) | 解除违规 |
| 49 | 管理 | GET | `/api/admin/user` | 是(管理员) | 用户列表 |
| 50 | 管理 | PUT | `/api/admin/user/{id}/blacklist` | 是(管理员) | 加入黑名单 |
| 51 | 管理 | DELETE | `/api/admin/user/{id}/blacklist` | 是(管理员) | 解除黑名单 |
| 52 | 管理 | GET | `/api/admin/blacklist` | 是(管理员) | 黑名单列表 |
| 53 | 管理 | GET | `/api/admin/category` | 是(管理员) | 类目管理列表 |
| 54 | 管理 | POST | `/api/admin/category` | 是(管理员) | 新增类目 |
| 55 | 管理 | PUT | `/api/admin/category/{id}` | 是(管理员) | 修改类目 |
| 56 | 管理 | DELETE | `/api/admin/category/{id}` | 是(管理员) | 删除类目 |
| 57 | 管理 | GET | `/api/admin/dashboard/overview` | 是(管理员) | 看板概览 |
| 58 | 管理 | GET | `/api/admin/dashboard/trends` | 是(管理员) | 趋势统计 |
| 59 | 管理 | GET | `/api/admin/dashboard/category-stats` | 是(管理员) | 类目统计 |

---

## 附录 B：错误码规范

| code | 说明 |
|------|------|
| 200 | 请求成功 |
| 400 | 请求参数校验失败 |
| 401 | 未登录或 Token 已过期 |
| 403 | 无权限访问（角色不足 / 未认证 / 黑名单） |
| 404 | 目标资源不存在 |
| 409 | 业务规则冲突（重复操作 / 非法状态流转 / 不可购买自己商品等） |
| 500 | 服务器内部错误 |

业务层面的错误信息通过 `message` 字段返回可读描述，如 `"该商品已被他人下单"`、`"同一商品不可重复收藏"`。

---

## 附录 C：请求/响应 Header 规范

| Header | 值 | 说明 |
|--------|-----|------|
| `Authorization` | `Bearer {jwt_token}` | 鉴权 Token |
| `Content-Type` | `application/json` | 请求体为 JSON |
| `Content-Type` | `multipart/form-data` | 文件上传 |
| `Accept` | `application/json` | 期望响应格式 |

---

> **文档维护说明**：本文档为 v1.0 初始版本，API 路径以 `/api` 为前缀，所有接口的入参/出参均与 `Database.md` 中的表结构及 `PRD.md` 中的功能需求对齐。后续模块开发时如需调整接口，应同步更新本文档并注明版本变更。
