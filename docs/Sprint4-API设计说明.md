# Sprint 4 API 设计说明文档

## 📋 文档信息

| 项目 | 内容 |
|------|------|
| Sprint | Sprint 4 - 支付与用户中心 |
| 日期 | 2026-04-16 |
| 版本 | V1.0 |

---

## 1. 支付模块 API 设计

### 1.1 创建支付单

**接口地址**: `POST /api/payment/create`

**认证要求**: ✅ 需要 (Bearer Token)

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderId | Long | ✅ | 订单ID |
| paymentMethod | String | ✅ | 支付方式: `wechat` / `alipay` / `bankcard` |
| amount | BigDecimal | ❌ | 支付金额(后端校验) |

**请求示例**:
```json
{
  "orderId": 12345,
  "paymentMethod": "wechat",
  "amount": 199.00
}
```

**响应参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| paymentId | String | 支付单号 |
| orderId | Long | 订单ID |
| orderNo | String | 订单编号 |
| amount | BigDecimal | 支付金额 |
| paymentMethod | String | 支付方式 |
| payUrl | String | H5支付跳转URL(微信/支付宝) |
| qrCode | String | 扫码支付二维码(可选) |
| expireTime | LocalDateTime | 支付过期时间 |
| status | String | 支付状态: WAITING/EXPIRED |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "paymentId": "PAY202604160001",
    "orderId": 12345,
    "orderNo": "ORD202604160001",
    "amount": 199.00,
    "paymentMethod": "wechat",
    "payUrl": "https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?...",
    "expireTime": "2026-04-16T15:30:00",
    "status": "WAITING"
  }
}
```

**业务逻辑**:
1. 校验订单状态必须为 `PENDING_PAYMENT`
2. 校验订单是否属于当前用户
3. 校验支付金额与订单金额是否一致
4. 生成支付单，记录支付方式
5. 设置支付过期时间(根据票品类型: 普通15分钟/热门5分钟)
6. 返回支付跳转URL或二维码

---

### 1.2 支付回调

**接口地址**: `POST /api/payment/callback`

**认证要求**: ❌ 不需要 (使用签名验证)

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderNo | String | ✅ | 订单号 |
| amount | String | ✅ | 支付金额 |
| timestamp | String | ✅ | 时间戳 |
| sign | String | ✅ | 签名 |
| payChannel | String | ✅ | 支付渠道: wechat / alipay |
| tradeNo | String | ❌ | 第三方交易号 |

**签名算法**:
```
sign = MD5(orderNo + amount + timestamp + secretKey)
```

**响应格式**:
```json
{
  "code": 0,
  "message": "处理成功"
}
```

**业务逻辑**:
1. **签名验证**: 使用 `PaymentSignatureUtils.verifyPaymentCallback()`
2. **幂等处理**: 使用订单号作为分布式锁Key，防止重复处理
3. **金额校验**: 回调金额与订单金额必须一致
4. **状态更新**: 更新支付单状态为 `SUCCESS`
5. **订单流转**: 更新订单状态为 `PAID`
6. **库存确认**: 确认库存锁定
7. **触发出票**: 异步触发出票流程

**安全要求**:
- 必须验证签名，防止伪造回调
- 必须校验金额，防止篡改
- 必须保证幂等，防止重复扣款
- 记录回调日志，便于追溯

---

### 1.3 查询支付状态

**接口地址**: `GET /api/payment/status/{paymentId}`

**认证要求**: ✅ 需要 (Bearer Token)

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| paymentId | String | ✅ | 支付单号 |

**响应参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| paymentId | String | 支付单号 |
| orderId | Long | 订单ID |
| status | String | 支付状态 |
| payTime | LocalDateTime | 支付时间(成功时) |
| failReason | String | 失败原因(失败时) |

**支付状态枚举**:

| 状态码 | 说明 |
|--------|------|
| WAITING | 待支付 |
| SUCCESS | 支付成功 |
| FAILED | 支付失败 |
| EXPIRED | 已过期 |
| REFUNDING | 退款中 |
| REFUNDED | 已退款 |

---

### 1.4 取消支付

**接口地址**: `DELETE /api/payment/{paymentId}`

**认证要求**: ✅ 需要 (Bearer Token)

**业务逻辑**:
1. 校验支付单属于当前用户
2. 校验支付单状态为 `WAITING`
3. 更新支付单状态为 `EXPIRED`
4. 订单状态保持 `PENDING_PAYMENT`，用户可重新发起支付

---

## 2. 用户中心 API 设计

### 2.1 获取用户资料

**接口地址**: `GET /api/user/profile`

**认证要求**: ✅ 需要 (Bearer Token)

**响应参数**:

| 参数名 | 类型 | 说明 |
|--------|------|------|
| userId | Long | 用户ID |
| username | String | 用户名 |
| phone | String | 手机号 |
| email | String | 邮箱 |
| nickname | String | 昵称 |
| avatar | String | 头像URL |
| gender | Integer | 性别: 0-未知, 1-男, 2-女 |
| birthday | String | 生日: YYYY-MM-DD |
| userType | Integer | 用户类型: 1-普通, 2-VIP, 3-企业 |
| userLevel | Integer | 用户等级 |
| memberLevel | String | 会员等级名称 |
| points | Integer | 积分 |
| totalOrderAmount | BigDecimal | 累计消费金额 |
| totalOrders | Integer | 累计订单数 |
| createdAt | LocalDateTime | 注册时间 |

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 10001,
    "username": "ticket_fan",
    "phone": "138****8888",
    "email": "user@example.com",
    "nickname": "票务达人",
    "avatar": "https://cdn.example.com/avatar/10001.jpg",
    "gender": 1,
    "birthday": "1995-06-15",
    "userType": 1,
    "userLevel": 3,
    "memberLevel": "黄金会员",
    "points": 5800,
    "totalOrderAmount": 12580.00,
    "totalOrders": 45,
    "createdAt": "2026-01-15T10:30:00"
  }
}
```

**会员等级映射逻辑**:

```java
public String getMemberLevel(Integer userType, Integer userLevel) {
    if (userType == 3) return "企业用户";
    if (userType == 2) return "VIP会员";
    
    return switch (userLevel) {
        case 1 -> "普通会员";
        case 2 -> "白银会员";
        case 3 -> "黄金会员";
        case 4 -> "铂金会员";
        case 5 -> "钻石会员";
        default -> "普通会员";
    };
}
```

---

### 2.2 更新用户资料

**接口地址**: `PUT /api/user/profile`

**认证要求**: ✅ 需要 (Bearer Token)

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 | 校验规则 |
|--------|------|------|------|----------|
| nickname | String | ❌ | 昵称 | 1-20字符 |
| avatar | String | ❌ | 头像URL | 有效的URL格式 |
| email | String | ❌ | 邮箱 | 邮箱格式校验 |
| gender | Integer | ❌ | 性别 | 0/1/2 |
| birthday | String | ❌ | 生日 | YYYY-MM-DD格式 |

**请求示例**:
```json
{
  "nickname": "新昵称",
  "avatar": "https://cdn.example.com/avatar/new.jpg",
  "email": "newemail@example.com",
  "gender": 1,
  "birthday": "1995-06-15"
}
```

**响应参数**:
```json
{
  "code": 0,
  "message": "更新成功",
  "data": {
    "userId": 10001,
    "nickname": "新昵称",
    "avatar": "https://cdn.example.com/avatar/new.jpg",
    "email": "newemail@example.com",
    "gender": 1,
    "birthday": "1995-06-15"
  }
}
```

**不可修改的字段** (请求中忽略):
- userId, username, phone, userType, userLevel, points, totalOrderAmount

**业务逻辑**:
1. 校验用户登录状态
2. 校验参数格式
3. 邮箱唯一性校验(如修改)
4. 更新用户信息
5. 返回更新后的信息

---

## 3. 接口调用时序图

### 3.1 支付流程时序

```
┌──────┐    ┌─────────┐    ┌────────┐    ┌─────────┐    ┌────────┐
│ 用户  │    │ 前端App  │    │ 后端API │    │ 订单服务 │    │ 支付渠道 │
└──┬───┘    └────┬────┘    └────┬───┘    └────┬────┘    └────┬───┘
   │             │              │              │              │
   │ 1.去支付    │              │              │              │
   │────────────>│              │              │              │
   │             │ 2.创建支付单 │              │              │
   │             │─────────────>│              │              │
   │             │              │ 3.校验订单   │              │
   │             │              │─────────────>│              │
   │             │              │<─────────────│              │
   │             │              │ 4.创建支付单  │              │
   │             │              │─────────────>│              │
   │             │<─────────────│              │              │
   │ 5.展示支付页│              │              │              │
   │<────────────│              │              │              │
   │             │              │              │              │
   │ 6.选择支付方式              │              │              │
   │────────────>│              │              │              │
   │             │ 7.确认支付    │              │              │
   │             │─────────────>│              │              │
   │             │              │ 8.跳转支付    │              │
   │             │              │──────────────────────────>│
   │             │              │              │              │
   │ 9.支付中... │              │              │              │
   │<────────────│              │              │              │
   │             │              │              │              │
   │             │              │ 10.异步回调   │              │
   │             │              │<───────────────────────────│
   │             │              │ 11.更新订单状态              │
   │             │              │─────────────>│              │
   │             │              │ 12.触发出票  │              │
   │             │              │─────────────>│              │
   │             │              │              │              │
   │             │ 13.查询支付状态              │              │
   │             │<─────────────│              │              │
   │ 14.展示结果 │              │              │              │
   │<────────────│              │              │              │
   │             │              │              │              │
```

### 3.2 用户资料更新时序

```
┌──────┐    ┌─────────┐    ┌────────┐
│ 用户  │    │ 前端App  │    │ 后端API │
└──┬───┘    └────┬────┘    └────┬───┘
   │             │              │
   │ 1.进入用户中心              │
   │────────────>│              │
   │             │ 2.获取用户资料              │
   │             │─────────────>│
   │             │<─────────────│
   │ 3.展示资料  │              │
   │<────────────│              │
   │             │              │
   │ 4.修改资料  │              │
   │────────────>│              │
   │             │ 5.更新资料   │
   │             │─────────────>│
   │             │<─────────────│
   │ 6.展示结果  │              │
   │<────────────│              │
   │             │              │
```

---

## 4. 错误处理规范

### 4.1 统一错误响应格式

```json
{
  "code": 错误码,
  "message": "错误信息",
  "data": null,
  "timestamp": 1709980800000,
  "requestId": "req-uuid-xxx"
}
```

### 4.2 支付模块错误码

| 错误码 | HTTP状态码 | 说明 | 处理建议 |
|--------|------------|------|----------|
| 40001 | 404 | 支付单不存在 | 重新创建支付单 |
| 40002 | 400 | 支付单已过期 | 提示用户，重新下单 |
| 40003 | 400 | 支付单已支付 | 跳转到支付成功页 |
| 40004 | 400 | 签名验证失败 | 记录日志，排查问题 |
| 40005 | 400 | 支付金额不匹配 | 联系客服处理 |
| 40006 | 400 | 不支持的支付方式 | 提示用户选择其他方式 |
| 40007 | 400 | 订单状态不允许支付 | 刷新订单状态 |

### 4.3 用户模块错误码

| 错误码 | HTTP状态码 | 说明 | 处理建议 |
|--------|------------|------|----------|
| 100101 | 404 | 用户不存在 | 跳转登录页 |
| 100102 | 403 | 用户已禁用 | 提示联系客服 |
| 100107 | 401 | Token无效 | 跳转登录页 |
| 100108 | 401 | Token已过期 | 跳转登录页 |
| 100201 | 400 | 邮箱格式错误 | 提示正确格式 |
| 100202 | 400 | 邮箱已被使用 | 提示更换邮箱 |

---

## 5. 数据库设计变更

### 5.1 新增支付表 (payment)

```sql
CREATE TABLE `payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付单ID',
  `payment_no` VARCHAR(32) NOT NULL COMMENT '支付单号',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `payment_method` VARCHAR(20) NOT NULL COMMENT '支付方式: wechat/alipay/bankcard',
  `pay_channel` VARCHAR(20) DEFAULT NULL COMMENT '支付渠道',
  `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '第三方交易号',
  `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '状态: WAITING/SUCCESS/FAILED/EXPIRED/REFUNDING/REFUNDED',
  `pay_url` TEXT DEFAULT NULL COMMENT '支付跳转URL',
  `qr_code` TEXT DEFAULT NULL COMMENT '支付二维码',
  `expire_time` DATETIME NOT NULL COMMENT '支付过期时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';
```

### 5.2 用户表变更

```sql
-- 新增字段
ALTER TABLE `user` ADD COLUMN `birthday` DATE DEFAULT NULL COMMENT '生日';
ALTER TABLE `user` ADD COLUMN `total_order_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计消费金额';
```

---

## 6. 缓存策略

### 6.1 用户资料缓存

| 缓存Key | 过期时间 | 说明 |
|---------|----------|------|
| `user:profile:{userId}` | 30分钟 | 用户资料缓存 |
| `user:profile:{userId}:version` | - | 缓存版本号 |

**更新策略**:
- 读取时检查版本号
- 更新时删除缓存并递增版本号
- 确保缓存一致性

### 6.2 支付单缓存

| 缓存Key | 过期时间 | 说明 |
|---------|----------|------|
| `payment:{paymentId}` | 支付过期时间+10分钟 | 支付单状态缓存 |

**更新策略**:
- 创建支付单时写入缓存
- 回调成功时更新缓存
- 过期时自动失效

---

## 7. 日志规范

### 7.1 支付日志要求

```java
// 创建支付单
log.info("创建支付单: paymentNo={}, orderId={}, userId={}, amount={}, method={}", 
    paymentNo, orderId, userId, amount, paymentMethod);

// 支付回调
log.info("收到支付回调: orderNo={}, amount={}, channel={}, tradeNo={}", 
    orderNo, amount, payChannel, tradeNo);
log.warn("支付回调签名验证失败: orderNo={}", orderNo);

// 支付成功处理
log.info("支付成功处理完成: orderNo={}, paymentNo={}", orderNo, paymentNo);
```

### 7.2 用户操作日志

```java
// 更新用户资料
log.info("用户资料更新: userId={}, fields={}", userId, updatedFields);
```

---

**文档版本历史**

| 版本 | 日期 | 变更内容 | 负责人 |
|------|------|----------|--------|
| 1.0 | 2026-04-16 | 初始版本 | 产品经理 |
