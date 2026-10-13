# 票务App后端API接口文档

## 项目信息
- 版本：1.0.1
- 更新日期：2026-10-13
- 基础路径：`/api`

> **以在线 Swagger 为准**：本文档为概览参考。实时的、从代码生成的接口定义见
> `http://127.0.0.1:8088/api/swagger-ui/index.html`（机读 JSON：`/api/v3/api-docs`；
> dev 环境默认开启，可用环境变量 `SWAGGER_ENABLED=false` 关闭）。
> 2026-10-13 已按 Swagger 实测修正购物车/分类/优惠券路径，并补充下单 `attendees`
> 强制规则与 `payment/mock-pay` 模拟支付端点。

---

## 目录
1. [用户管理](#用户管理)
2. [支付管理](#支付管理)
3. [订单管理](#订单管理)
4. [票务查询](#票务查询)
5. [购物车](#购物车)
6. [优惠券](#优惠券)

---

## 用户管理

### 1.1 用户注册
- **接口路径**: `POST /user/register`
- **是否需要认证**: 否
- **请求参数**:
```json
{
    "username": "string (必填, 3-20字符)",
    "password": "string (必填, 6-20字符)",
    "phone": "string (必填, 手机号格式)",
    "email": "string (选填, 邮箱格式)",
    "nickname": "string (选填)"
}
```
- **响应示例**:
```json
{
    "code": 0,
    "message": "success",
    "data": 123456
}
```

### 1.2 用户登录
- **接口路径**: `POST /user/login`
- **是否需要认证**: 否
- **请求参数**:
```json
{
    "username": "string (必填)",
    "password": "string (必填)"
}
```
- **响应示例**:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "userId": 123456,
        "username": "test_user",
        "nickname": "测试用户",
        "phone": "13800138000",
        "email": "test@example.com",
        "userType": 1,
        "userLevel": 1,
        "avatar": "https://example.com/avatar.jpg",
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresIn": 86400
    }
}
```

### 1.3 获取用户信息
- **接口路径**: `GET /user/{userId}`
- **是否需要认证**: 否
- **响应示例**:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "userId": 123456,
        "username": "test_user",
        "nickname": "测试用户",
        "phone": "13800138000",
        "email": "test@example.com",
        "userType": 1,
        "userLevel": 1,
        "avatar": "https://example.com/avatar.jpg"
    }
}
```

### 1.4 更新用户信息
- **接口路径**: `PUT /user/profile`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "nickname": "string (选填, 最大50字符)",
    "phone": "string (选填, 手机号格式)",
    "email": "string (选填, 邮箱格式)",
    "gender": "integer (选填, 0-未知, 1-男, 2-女)",
    "birthday": "string (选填, 格式: yyyy-MM-dd)",
    "avatar": "string (选填, 头像URL)"
}
```

### 1.5 修改密码
- **接口路径**: `PUT /user/password`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "oldPassword": "string (必填)",
    "newPassword": "string (必填, 6-20字符)",
    "confirmPassword": "string (必填)"
}
```

### 1.6 用户登出
- **接口路径**: `POST /user/logout`
- **是否需要认证**: 是 (Bearer Token)

---

## 支付管理

### 2.1 创建支付
- **接口路径**: `POST /payment/create`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "orderNo": "string (必填, 订单号)",
    "amount": "decimal (必填, 支付金额)",
    "payChannel": "string (必填, alipay|wechat|bank|unionpay)",
    "clientIp": "string (选填, 客户端IP)",
    "returnUrl": "string (选填, 支付完成后跳转URL)",
    "paymentScene": "string (选填, APP|H5|WEB|NATIVE, 默认H5)"
}
```
- **响应示例**:
```json
{
    "code": 0,
    "message": "支付订单创建成功",
    "data": {
        "paymentId": 123456789,
        "orderNo": "T202604161234567890",
        "amount": 299.00,
        "status": "PENDING",
        "statusText": "待支付",
        "payChannel": "alipay",
        "qrCodeUrl": "https://api.example.com/qrcode/xxx",
        "payUrl": "https://pay.example.com/h5?orderNo=xxx",
        "expireTime": "2026-04-16T15:30:00"
    }
}
```

### 2.2 查询支付状态
- **接口路径**: `GET /payment/status/{orderNo}`
- **是否需要认证**: 是 (Bearer Token)
- **响应示例**:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "paymentId": 123456789,
        "orderNo": "T202604161234567890",
        "amount": 299.00,
        "status": "SUCCESS",
        "statusText": "支付成功",
        "payChannel": "alipay",
        "tradeNo": "202604161234567890",
        "payTime": "2026-04-16T15:00:00"
    }
}
```
- **支付状态说明**:
  - `PENDING`: 待支付
  - `SUCCESS`: 支付成功
  - `FAILED`: 支付失败
  - `CLOSED`: 已关闭(超时/取消)

### 2.3 申请退款
- **接口路径**: `POST /payment/refund`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "orderNo": "string (必填, 订单号)",
    "reason": "string (选填, 退款原因)",
    "refundAmount": "string (选填, 不传则全额退款)",
    "proofImages": "string (选填, 退款凭证图片URL)"
}
```
- **响应示例**:
```json
{
    "code": 0,
    "message": "退款申请已提交",
    "data": {
        "refundId": 123456789,
        "orderNo": "T202604161234567890",
        "payAmount": 299.00,
        "refundAmount": 299.00,
        "status": "PROCESSING",
        "statusText": "退款处理中",
        "refundType": "USER",
        "reason": "行程变更",
        "applyTime": "2026-04-16T15:30:00"
    }
}
```

### 2.4 查询退款状态
- **接口路径**: `GET /payment/refund/{orderNo}`
- **是否需要认证**: 是 (Bearer Token)

### 2.5 取消支付
- **接口路径**: `DELETE /payment/{orderNo}`
- **是否需要认证**: 是 (Bearer Token)

### 2.6 支付回调
- **接口路径**: `POST /payment/callback`
- **是否需要认证**: 签名验证
- **注意**: 此接口供支付系统回调使用，需要携带签名参数

### 2.7 模拟支付（开发/教学用）
- **接口路径**: `POST /payment/mock-pay`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**: 与"创建支付"相同（`orderNo`、`amount`、`payChannel` 必填）
- **说明**: 不触发真实支付渠道，直接把订单标记为支付成功，用于本地开发与自动化测试中走通"下单 → 支付 → 已支付"链路

### 2.8 订单支付成功回调
- **接口路径**: `POST /orders/pay/callback`
- **查询参数**: `orderNo`、`amount`、`timestamp`、`sign`（必填），`payChannel`、`tradeNo`（选填）
- **注意**: 供支付系统回调使用

---

## 订单管理

### 3.1 创建订单
- **接口路径**: `POST /orders`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "ticketId": 1,
    "sessionId": 1,
    "items": [
        {
            "priceLevelId": 1,
            "priceLevelName": "VIP区",
            "quantity": 2,
            "unitPrice": 299.00,
            "seatId": 1,
            "seatNo": "A1",
            "attendees": [
                { "realName": "张三", "idCardNo": "110101199001011234" },
                { "realName": "李四", "idCardNo": "110101199002022345" }
            ]
        }
    ],
    "addressId": 1,
    "contactName": "张三",
    "contactPhone": "13800138000",
    "contactIdCard": "110101199001011234",
    "couponId": 1,
    "userRemark": "需要发票",
    "source": "app",
    "usePoints": false,
    "pointsDiscount": 0.00
}
```
- **注意（实测强制规则）**:
  - `items[].attendees` 观演人列表必填，**每张票一个观演人**（数量必须与 `quantity` 一致，否则返回业务错误码 100600）
  - `attendees[].idCardNo` 必须通过身份证校验位验证
  - 观演人也可先通过 `POST /user/identities` 维护常用观演人，再传 `identityId`
- **响应示例**:
```json
{
    "code": 0,
    "message": "success",
    "data": {
        "orderId": 123456,
        "orderNo": "T202604161234567890",
        "totalAmount": 598.00,
        "discountAmount": 0.00,
        "payAmount": 598.00,
        "expireTime": "2026-04-16T15:30:00"
    }
}
```

### 3.2 获取订单列表
- **接口路径**: `GET /orders`
- **是否需要认证**: 是 (Bearer Token)
- **查询参数**:
  - `status`: 订单状态 (1-待支付, 2-已支付, 3-已完成, 4-已取消, 5-已退款, 6-退款中)
  - `page`: 页码 (默认1)
  - `pageSize`: 每页数量 (默认10)

### 3.3 获取订单详情
- **接口路径**: `GET /orders/{id}`
- **是否需要认证**: 是 (Bearer Token)

### 3.4 取消订单
- **接口路径**: `PUT /orders/{id}/cancel`
- **是否需要认证**: 是 (Bearer Token)

### 3.5 确认完成订单
- **接口路径**: `PUT /orders/{id}/complete`
- **是否需要认证**: 是 (Bearer Token)

---

## 票务查询

### 4.1 获取票务列表
- **接口路径**: `GET /tickets`
- **是否需要认证**: 否

### 4.2 获取票务详情
- **接口路径**: `GET /tickets/{id}`
- **是否需要认证**: 否

### 4.3 获取热门票务
- **接口路径**: `GET /tickets/hot`
- **是否需要认证**: 否

### 4.4 获取票务分类
- **接口路径**: `GET /categories`
- **是否需要认证**: 否

---

## 购物车

### 5.1 获取购物车列表
- **接口路径**: `GET /cart`
- **是否需要认证**: 是 (Bearer Token)

### 5.2 添加购物车
- **接口路径**: `POST /cart/add`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "ticketId": 1,
    "sessionId": 1,
    "priceLevelId": 1,
    "priceLevelName": "VIP区",
    "quantity": 2,
    "unitPrice": 299.00
}
```
（以上字段均必填）

### 5.3 更新购物车项数量
- **接口路径**: `PUT /cart/quantity`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "cartId": 1,
    "quantity": 3
}
```
（两字段均必填）

### 5.4 删除购物车项
- **接口路径**: `DELETE /cart/{id}`
- **是否需要认证**: 是 (Bearer Token)

### 5.5 清空购物车
- **接口路径**: `DELETE /cart`
- **是否需要认证**: 是 (Bearer Token)

### 5.6 批量删除购物车项
- **接口路径**: `DELETE /cart/batch`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**: 请求体为购物车项 ID 数组，如 `[1, 2, 3]`

### 5.7 获取购物车数量
- **接口路径**: `GET /cart/count`
- **是否需要认证**: 是 (Bearer Token)

---

## 优惠券

### 6.1 获取可用优惠券
- **接口路径**: `GET /coupons`
- **是否需要认证**: 是 (Bearer Token)
- **查询参数**: `orderAmount`、`ticketId`、`categoryId`（均选填，用于过滤可用券）

### 6.2 领取优惠券
- **接口路径**: `POST /coupons/receive`
- **是否需要认证**: 是 (Bearer Token)
- **查询参数**: `couponId`（必填）

### 6.3 验证优惠券
- **接口路径**: `POST /coupons/validate`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**:
```json
{
    "userCouponId": 301,
    "orderAmount": 299.00,
    "ticketId": 1,
    "categoryId": 1
}
```
（`userCouponId`、`orderAmount` 必填）

### 6.4 领券中心
- **接口路径**: `GET /coupons/center`
- **是否需要认证**: 是 (Bearer Token)
- **查询参数**: `page`、`pageSize`（选填）

### 6.5 我的优惠券列表
- **接口路径**: `GET /coupons/list`
- **是否需要认证**: 是 (Bearer Token)
- **查询参数**: `status`、`page`、`pageSize`（选填）

### 6.6 使用优惠券
- **接口路径**: `POST /coupons/use`
- **是否需要认证**: 是 (Bearer Token)
- **请求参数**: `{ "userCouponId": 301, "orderId": 123456 }`（`userCouponId` 必填）

### 6.7 优惠券详情
- **接口路径**: `GET /coupons/{id}`
- **是否需要认证**: 是 (Bearer Token)

---

## 响应格式

### 成功响应
```json
{
    "code": 0,
    "message": "success",
    "data": { ... }
}
```

### 错误响应
```json
{
    "code": 100100,
    "message": "用户不存在",
    "data": null
}
```

### 错误码说明
- `0`: 成功
- `1001xx`: 用户模块错误
- `1002xx`: 票务模块错误
- `1003xx`: 订单模块错误
- `1004xx`: 支付模块错误
- `1005xx`: 退款模块错误
- `1006xx`: 通用业务错误
- `2001xx`: 系统错误
- `3001xx`: 第三方服务错误

---

## 认证方式

API使用JWT Token进行认证，请在请求头中携带：
```
Authorization: Bearer <token>
```

Token在用户登录成功后返回，有效期为24小时。
