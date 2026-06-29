# 支付模块 API 文档

## 📋 文档信息

| 项目 | 内容 |
|------|------|
| 模块 | 支付管理 |
| 版本 | V1.0 |
| 更新日期 | 2026-04-17 |

---

## 1. 概述

支付模块提供完整的支付、退款和回调处理功能，支持微信支付、支付宝等主流支付渠道。

### 1.1 功能列表

| 功能 | 接口 | 说明 |
|------|------|------|
| 创建支付 | `POST /api/payment/create` | 为订单创建支付请求 |
| 查询支付状态 | `GET /api/payment/status/{orderNo}` | 查询订单支付状态 |
| 申请退款 | `POST /api/payment/refund` | 对已支付订单申请退款 |
| 查询退款状态 | `GET /api/payment/refund/{orderNo}` | 查询退款进度 |
| 取消支付 | `DELETE /api/payment/{orderNo}` | 取消未支付的订单 |
| 支付回调 | `POST /api/payment/callback` | 支付渠道回调通知 |

### 1.2 支付状态

| 状态码 | 说明 | 描述 |
|--------|------|------|
| `WAITING` | 待支付 | 支付单已创建，等待用户完成支付 |
| `SUCCESS` | 支付成功 | 用户已完成支付 |
| `FAILED` | 支付失败 | 支付失败 |
| `EXPIRED` | 已过期 | 支付单过期 |
| `REFUNDING` | 退款中 | 退款处理中 |
| `REFUNDED` | 已退款 | 退款完成 |

### 1.3 支付渠道

| 渠道码 | 说明 |
|--------|------|
| `wechat` | 微信支付 |
| `alipay` | 支付宝 |
| `bankcard` | 银行卡支付 |

---

## 2. 接口详情

### 2.1 创建支付

创建支付订单，获取支付二维码或跳转链接。

**请求信息**

```
POST /api/payment/create
Content-Type: application/json
Authorization: Bearer {token}
```

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | ✅ | 订单号 |
| payChannel | String | ✅ | 支付渠道: wechat / alipay / bankcard |
| clientIp | String | ❌ | 客户端IP（自动获取时可省略） |

**请求示例**

```json
{
  "orderNo": "ORD202604170001",
  "payChannel": "wechat"
}
```

**响应参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| paymentId | String | 支付单号 |
| orderNo | String | 订单号 |
| orderId | Long | 订单ID |
| amount | BigDecimal | 支付金额 |
| payChannel | String | 支付渠道 |
| payUrl | String | H5支付跳转URL（微信/支付宝） |
| qrCode | String | 扫码支付二维码（Base64图片） |
| expireTime | LocalDateTime | 支付过期时间 |
| status | String | 支付状态: WAITING |

**响应示例**

```json
{
  "code": 0,
  "message": "支付订单创建成功",
  "data": {
    "paymentId": "PAY202604170001",
    "orderNo": "ORD202604170001",
    "orderId": 1001,
    "amount": 199.00,
    "payChannel": "wechat",
    "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
    "expireTime": "2026-04-17 15:30:00",
    "status": "WAITING"
  }
}
```

**业务规则**

1. 订单状态必须为 `待支付`（status = 1）
2. 订单必须属于当前登录用户
3. 支付单有效期为 15 分钟（普通票）/ 5 分钟（热门票）
4. 同一订单只能创建一个支付单

**错误码**

| 错误码 | 说明 |
|--------|------|
| 40001 | 订单不存在 |
| 40002 | 订单状态不允许支付 |
| 40006 | 不支持的支付方式 |

---

### 2.2 查询支付状态

查询订单的支付状态和详情。

**请求信息**

```
GET /api/payment/status/{orderNo}
Authorization: Bearer {token}
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | ✅ | 订单号 |

**响应参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| paymentId | String | 支付单号 |
| orderNo | String | 订单号 |
| amount | BigDecimal | 支付金额 |
| payChannel | String | 支付渠道 |
| status | String | 支付状态 |
| payTime | LocalDateTime | 支付成功时间 |
| failReason | String | 失败原因 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "paymentId": "PAY202604170001",
    "orderNo": "ORD202604170001",
    "amount": 199.00,
    "payChannel": "wechat",
    "status": "SUCCESS",
    "payTime": "2026-04-17 14:30:00"
  }
}
```

---

### 2.3 申请退款

对已支付订单申请退款。

**请求信息**

```
POST /api/payment/refund
Content-Type: application/json
Authorization: Bearer {token}
```

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | ✅ | 订单号 |
| refundAmount | BigDecimal | ❌ | 退款金额（为空则全额退款） |
| refundReason | String | ❌ | 退款原因 |

**请求示例**

```json
{
  "orderNo": "ORD202604170001",
  "refundReason": "行程变更"
}
```

**响应参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| refundId | String | 退款单号 |
| orderNo | String | 订单号 |
| refundAmount | BigDecimal | 退款金额 |
| status | String | 退款状态 |
| applyTime | LocalDateTime | 申请时间 |
| expectTime | LocalDateTime | 预计到账时间 |

**响应示例**

```json
{
  "code": 0,
  "message": "退款申请已提交",
  "data": {
    "refundId": "REF202604170001",
    "orderNo": "ORD202604170001",
    "refundAmount": 199.00,
    "status": "REFUNDING",
    "applyTime": "2026-04-17 14:35:00",
    "expectTime": "2026-04-17 16:35:00"
  }
}
```

**业务规则**

1. 订单必须已支付（status = 1, payStatus = 1）
2. 退款金额不能超过实际支付金额
3. 已完成（status = 3）的订单需要在演出前24小时申请
4. 部分退款后不能再申请二次退款

**错误码**

| 错误码 | 说明 |
|--------|------|
| 40003 | 订单未支付 |
| 40005 | 退款金额超过支付金额 |
| 40007 | 订单状态不允许退款 |

---

### 2.4 查询退款状态

查询订单的退款进度。

**请求信息**

```
GET /api/payment/refund/{orderNo}
Authorization: Bearer {token}
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | ✅ | 订单号 |

**响应参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| refundId | String | 退款单号 |
| orderNo | String | 订单号 |
| refundAmount | BigDecimal | 退款金额 |
| status | String | 退款状态: REFUNDING / REFUNDED |
| applyTime | LocalDateTime | 申请时间 |
| refundTime | LocalDateTime | 退款完成时间 |
| failReason | String | 失败原因 |

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "refundId": "REF202604170001",
    "orderNo": "ORD202604170001",
    "refundAmount": 199.00,
    "status": "REFUNDED",
    "applyTime": "2026-04-17 14:35:00",
    "refundTime": "2026-04-17 15:30:00"
  }
}
```

---

### 2.5 取消支付

取消未支付的订单，释放支付单。

**请求信息**

```
DELETE /api/payment/{orderNo}
Authorization: Bearer {token}
```

**路径参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | ✅ | 订单号 |

**响应示例**

```json
{
  "code": 0,
  "message": "支付已取消",
  "data": null
}
```

**业务规则**

1. 订单状态必须为 `待支付`（status = 1）
2. 取消后用户可以重新发起支付

**错误码**

| 错误码 | 说明 |
|--------|------|
| 40001 | 订单不存在 |
| 40002 | 订单已支付，无法取消 |

---

### 2.6 支付回调

支付渠道回调接口，接收支付结果通知。

**⚠️ 安全说明**

此接口不需要用户认证，但需要签名验证。支付渠道必须使用正确的签名算法生成签名。

**请求信息**

```
POST /api/payment/callback
Content-Type: application/x-www-form-urlencoded
```

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | ✅ | 订单号 |
| amount | String | ✅ | 支付金额（字符串格式） |
| timestamp | String | ✅ | 时间戳（毫秒） |
| sign | String | ✅ | 签名 |
| payChannel | String | ✅ | 支付渠道: wechat / alipay |
| tradeNo | String | ❌ | 第三方交易号 |

**签名算法**

```
signSource = "orderNo={orderNo}&amount={amount}&timestamp={timestamp}"
sign = HMAC-SHA256(signSource, secretKey)
```

**签名生成示例**

```java
String orderNo = "ORD202604170001";
String amount = "199.00";
String timestamp = String.valueOf(System.currentTimeMillis());
String signSource = "orderNo=" + orderNo + "&amount=" + amount + "&timestamp=" + timestamp;
String sign = calculateHmacSha256(signSource, secretKey);
```

**响应参数**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码，0表示成功 |
| message | String | 处理结果信息 |

**响应示例**

```json
{
  "code": 0,
  "message": "处理成功"
}
```

**业务规则**

1. 签名验证失败返回错误
2. 时间戳超过5分钟视为过期
3. 同一订单号回调会幂等处理
4. 回调处理后会更新订单状态

**安全要求**

- 签名密钥必须保密，仅在支付系统和本系统间共享
- 必须验证签名，防止伪造回调
- 必须校验金额，防止篡改
- 必须保证幂等，防止重复处理
- 记录完整回调日志

---

## 3. 配置说明

### 3.1 环境变量

| 变量名 | 必填 | 说明 |
|--------|------|------|
| `JWT_SECRET` | ✅ | JWT签名密钥（生产环境必须设置） |
| `PAYMENT_CALLBACK_SECRET_KEY` | ✅ | 支付回调签名密钥（生产环境必须设置） |

### 3.2 配置文件

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET}  # 从环境变量读取

payment:
  callback:
    secret-key: ${PAYMENT_CALLBACK_SECRET_KEY}
```

### 3.3 密钥要求

| 密钥类型 | 最小长度 | 要求 |
|----------|----------|------|
| JWT密钥 | 32字符 | 建议使用随机字符串 |
| 支付签名密钥 | 32字符 | 建议使用随机字符串 |

---

## 4. 错误码

### 4.1 支付模块错误码

| 错误码 | HTTP状态 | 说明 | 处理建议 |
|--------|----------|------|----------|
| 40001 | 404 | 订单不存在 | 确认订单号是否正确 |
| 40002 | 400 | 订单状态不允许支付 | 刷新订单状态 |
| 40003 | 400 | 订单未支付 | 确认是否已完成支付 |
| 40004 | 400 | 签名验证失败 | 检查签名算法和密钥 |
| 40005 | 400 | 退款金额超过支付金额 | 核实退款金额 |
| 40006 | 400 | 不支持的支付方式 | 使用支持的支付渠道 |
| 40007 | 400 | 订单状态不允许退款 | 确认订单是否可退款 |

### 4.2 认证错误码

| 错误码 | HTTP状态 | 说明 |
|--------|----------|------|
| 100107 | 401 | Token无效 |
| 100108 | 401 | Token已过期 |

---

## 5. 时序图

### 5.1 正常支付流程

```
┌──────┐    ┌─────────┐    ┌────────┐    ┌────────┐    ┌────────┐
│ 用户  │    │ 前端App  │    │ 后端API │    │ 订单服务 │    │ 支付渠道 │
└──┬───┘    └────┬────┘    └────┬───┘    └────┬────┘    └────┬───┘
   │             │              │              │              │
   │ 1.去支付    │              │              │              │
   │────────────>│              │              │              │
   │             │ 2.创建支付    │              │              │
   │             │─────────────>│              │              │
   │             │              │ 3.校验订单   │              │
   │             │              │─────────────>│              │
   │             │              │<─────────────│              │
   │             │              │ 4.生成支付单  │              │
   │             │              │─────────────>│              │
   │             │<─────────────│              │              │
   │ 5.展示二维码│              │              │              │
   │<────────────│              │              │              │
   │             │              │              │              │
   │ 6.扫码支付  │              │              │              │
   │───────────────────────────>│              │              │
   │             │              │              │              │
   │             │              │ 7.异步回调   │              │
   │             │              │<───────────────────────────│
   │             │              │ 8.更新订单状态              │
   │             │              │─────────────>│              │
   │             │              │              │              │
   │             │ 9.轮询支付状态              │              │
   │             │<─────────────│              │              │
   │ 10.展示成功 │              │              │              │
   │<────────────│              │              │              │
```

### 5.2 退款流程

```
┌──────┐    ┌─────────┐    ┌────────┐    ┌────────┐    ┌────────┐
│ 用户  │    │ 前端App  │    │ 后端API │    │ 订单服务 │    │ 退款渠道 │
└──┬───┘    └────┬────┘    └────┬───┘    └────┬────┘    └────┬───┘
   │             │              │              │              │
   │ 1.申请退款  │              │              │              │
   │────────────>│              │              │              │
   │             │ 2.提交退款申请│              │              │
   │             │─────────────>│              │              │
   │             │              │ 3.校验订单   │              │
   │             │              │─────────────>│              │
   │             │              │<─────────────│              │
   │             │              │ 4.更新状态    │              │
   │             │              │─────────────>│              │
   │             │<─────────────│              │              │
   │ 5.退款中... │              │              │              │
   │<────────────│              │              │              │
   │             │              │              │              │
   │             │              │ 6.发起退款  │              │
   │             │              │───────────────────────────>│
   │             │              │              │              │
   │             │              │ 7.退款完成  │              │
   │             │              │<───────────────────────────│
   │             │              │ 8.更新退款状态              │
   │             │              │─────────────>│              │
   │             │              │              │              │
   │             │ 9.查询退款状态              │              │
   │             │<─────────────│              │              │
   │ 10.展示结果 │              │              │              │
   │<────────────│              │              │              │
```

---

## 6. 日志规范

### 6.1 支付日志

```java
// 创建支付单
log.info("创建支付单: orderNo={}, userId={}, amount={}, channel={}", 
    orderNo, userId, amount, payChannel);

// 支付回调
log.info("支付回调: orderNo={}, amount={}, channel={}, tradeNo={}", 
    orderNo, amount, payChannel, tradeNo);

// 签名验证
log.warn("支付回调签名验证失败: orderNo={}, sign={}", orderNo, sign);

// 支付成功
log.info("支付成功: orderNo={}, paymentNo={}", orderNo, paymentNo);
```

### 6.2 退款日志

```java
// 申请退款
log.info("申请退款: orderNo={}, userId={}, amount={}, reason={}", 
    orderNo, userId, refundAmount, refundReason);

// 退款完成
log.info("退款完成: orderNo={}, refundId={}, amount={}", 
    orderNo, refundId, refundAmount);
```

---

## 7. FAQ

### Q1: 支付超时后如何处理？
A: 支付单过期后状态变为 `EXPIRED`，用户可以重新发起支付。

### Q2: 如何保证回调的幂等性？
A: 使用订单号作为幂等Key，同一订单号重复回调会直接返回成功。

### Q3: 退款多久到账？
A: 微信支付一般1-3个工作日，支付宝即时到账，实际到账时间取决于支付渠道。

### Q4: 部分退款后还能再退款吗？
A: 不可以，部分退款后该订单视为已完成退款，不可再次申请。

---

**文档版本历史**

| 版本 | 日期 | 变更内容 | 负责人 |
|------|------|----------|--------|
| 1.0 | 2026-04-17 | 初始版本 | 后端开发 |
