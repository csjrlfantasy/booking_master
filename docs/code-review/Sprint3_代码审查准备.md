# Sprint 3 代码审查准备 - 订单/支付模块

## 审查信息

| 项目 | 内容 |
|------|------|
| 审查日期 | 预填：Sprint 3 开发完成后 |
| 审查人 | Code Reviewer |
| Sprint | Sprint 3 |
| 审查范围 | ☐ 订单模块 ☐ 支付模块 ☐ 收藏模块 ☐ 购物车模块 |
| 审查阶段 | ☐ 开发完成 ☐ 提测前 ☐ 上线前 |

---

## 一、待审查模块清单

### 1.1 订单模块

| 功能点 | 优先级 | 开发状态 | 审查状态 |
|--------|--------|----------|----------|
| 订单创建 | P0 | ☐ | ☐ |
| 订单查询 | P0 | ☐ | ☐ |
| 订单取消 | P0 | ☐ | ☐ |
| 订单状态流转 | P0 | ☐ | ☐ |
| 订单超时处理 | P1 | ☐ | ☐ |

### 1.2 支付模块

| 功能点 | 优先级 | 开发状态 | 审查状态 |
|--------|--------|----------|----------|
| 支付下单 | P0 | ☐ | ☐ |
| 支付回调 | P0 | ☐ | ☐ |
| 支付查询 | P0 | ☐ | ☐ |
| 支付取消 | P1 | ☐ | ☐ |

### 1.3 收藏/购物车模块

| 功能点 | 优先级 | 开发状态 | 审查状态 |
|--------|--------|----------|----------|
| 添加收藏 | P2 | ☐ | ☐ |
| 取消收藏 | P2 | ☐ | ☐ |
| 购物车添加 | P1 | ☐ | ☐ |
| 购物车结算 | P1 | ☐ | ☐ |

---

## 二、审查清单

### 2.1 订单模块必检项

| # | 检查项 | 检查标准 | 状态 | 备注 |
|---|--------|---------|------|------|
| 1 | 库存校验 | 下单前必须校验库存 | ☐ | |
| 2 | 并发控制 | 库存扣减需原子操作或分布式锁 | ☐ | |
| 3 | 幂等键 | 订单号/支付流水号唯一性 | ☐ | |
| 4 | 超时取消 | 订单超时自动释放库存 | ☐ | |
| 5 | 事务边界 | 事务范围明确，不含远程调用 | ☐ | |
| 6 | 状态机 | 订单状态流转正确性 | ☐ | |
| 7 | 金额计算 | 使用BigDecimal | ☐ | |
| 8 | 操作日志 | 关键操作记录日志 | ☐ | |

### 2.2 支付模块必检项

| # | 检查项 | 检查标准 | 状态 | 备注 |
|---|--------|---------|------|------|
| 1 | 回调验签 | 支付回调必须验证签名 | ☐ | |
| 2 | 幂等处理 | 防止回调重复处理 | ☐ | |
| 3 | 状态同步 | 支付状态与订单状态同步 | ☐ | |
| 4 | 异步通知 | 支付结果异步通知机制 | ☐ | |
| 5 | 密钥管理 | 支付密钥不硬编码 | ☐ | |
| 6 | 回调日志 | 回调处理详细日志 | ☐ | |
| 7 | 失败重试 | 支付失败有重试机制 | ☐ | |

### 2.3 通用安全检查

| # | 检查项 | 检查标准 | 状态 | 备注 |
|---|--------|---------|------|------|
| 1 | SQL注入 | 使用参数化查询 | ☐ | |
| 2 | XSS防护 | 输入输出转义 | ☐ | |
| 3 | 敏感数据 | 脱敏处理 | ☐ | |
| 4 | 权限校验 | 接口权限控制 | ☐ | |
| 5 | 参数校验 | @Valid注解使用 | ☐ | |

### 2.4 性能检查

| # | 检查项 | 检查标准 | 状态 | 备注 |
|---|--------|---------|------|------|
| 1 | 数据库索引 | 常用查询字段有索引 | ☐ | |
| 2 | 缓存使用 | 热数据适当缓存 | ☐ | |
| 3 | 大数据量 | 分页处理 | ☐ | |
| 4 | 连接池 | 数据库连接池配置 | ☐ | |

---

## 三、预期代码结构

### 3.1 后端目录结构

```
booking-api/src/main/java/com/ticket/app/
├── controller/
│   ├── order/              # 订单Controller
│   │   ├── OrderController.java
│   │   └── dto/
│   │       ├── CreateOrderRequest.java
│   │       └── OrderResponse.java
│   └── payment/            # 支付Controller
│       └── PaymentController.java
├── service/
│   ├── order/              # 订单Service
│   │   ├── OrderService.java
│   │   └── OrderServiceImpl.java
│   └── payment/            # 支付Service
│       ├── PaymentService.java
│       └── PaymentServiceImpl.java
├── repository/
│   └── order/              # 订单Repository
│       ├── OrderRepository.java
│       └── OrderItemRepository.java
├── entity/
│   ├── Order.java
│   └── OrderItem.java
├── config/
│   └── lock/               # 分布式锁配置
│       └── RedissonConfig.java
└── common/
    └── constant/            # 常量定义
        └── OrderStatus.java
```

### 3.2 前端目录结构

```
booking-app/src/
├── screens/
│   ├── order/
│   │   ├── OrderListScreen.tsx
│   │   ├── OrderDetailScreen.tsx
│   │   └── CreateOrderScreen.tsx
│   └── payment/
│       └── PaymentScreen.tsx
├── services/api/
│   ├── orderApi.ts
│   └── paymentApi.ts
├── store/slices/
│   ├── orderSlice.ts
│   └── cartSlice.ts
└── types/
    ├── order.ts
    └── payment.ts
```

---

## 四、风险评估模板

### 4.1 高风险点识别

| # | 风险点 | 风险等级 | 影响 | 缓解措施 |
|---|--------|----------|------|----------|
| 1 | 库存超卖 | 🔴 高 | 用户下单成功但无票 | 分布式锁/乐观锁 |
| 2 | 支付状态不一致 | 🔴 高 | 订单状态错误 | 状态机+幂等处理 |
| 3 | 订单重复创建 | 🟡 中 | 多次扣款 | 幂等键校验 |
| 4 | 回调处理失败 | 🟡 中 | 支付成功但订单失败 | 重试机制+告警 |

### 4.2 测试场景清单

| # | 场景 | 预期结果 | 测试状态 |
|---|------|---------|----------|
| 1 | 并发下单同一票 | 只有1人成功 | ☐ |
| 2 | 支付超时 | 订单自动取消 | ☐ |
| 3 | 重复回调 | 只处理1次 | ☐ |
| 4 | 库存不足 | 提示用户 | ☐ |
| 5 | 网络异常 | 重试机制 | ☐ |

---

## 五、代码规范提醒

### 5.1 订单模块规范

```java
// ✅ 正确的库存扣减方式
@DistributedLock(key = "stock:lock:ticket:{ticketId}")
public void createOrder(Long ticketId, Integer quantity) {
    // 1. 校验库存
    Ticket ticket = ticketRepository.selectById(ticketId);
    if (ticket.getAvailableStock() < quantity) {
        throw new BusinessException("库存不足");
    }
    
    // 2. 原子扣减
    ticketRepository.decrementStock(ticketId, quantity);
    
    // 3. 创建订单
    // ...
}

// ❌ 错误的库存扣减方式
public void createOrder(Long ticketId, Integer quantity) {
    Ticket ticket = ticketRepository.selectById(ticketId);
    if (ticket.getAvailableStock() >= quantity) {
        ticketRepository.updateStock(ticketId, ticket.getAvailableStock() - quantity);
        // 并发时可能导致超卖
    }
}
```

### 5.2 支付回调规范

```java
// ✅ 正确的回调处理
@PostMapping("/notify")
public String paymentNotify(@RequestBody PaymentNotifyRequest request) {
    // 1. 验签
    if (!verifySignature(request)) {
        return "fail";
    }
    
    // 2. 幂等处理
    String idempotentKey = request.getTradeNo();
    if (paymentLogRepository.existsByIdempotentKey(idempotentKey)) {
        return "success"; // 已处理过
    }
    
    // 3. 业务处理
    // ...
    
    // 4. 记录处理日志
    paymentLogRepository.insert(new PaymentLog(idempotentKey, ...));
    
    return "success";
}
```

### 5.3 金额计算规范

```java
// ✅ 正确
BigDecimal totalPrice = ticket.getPrice()
    .multiply(BigDecimal.valueOf(quantity))
    .add(BigDecimal.valueOf(serviceFee));

// ❌ 错误 - 使用double/float
double totalPrice = ticket.getPrice() * quantity;
```

---

## 六、审查会议准备

### 6.1 会议议程

1. **代码Walkthrough** (30分钟)
   - 订单创建流程
   - 支付回调流程

2. **问题讨论** (20分钟)
   - 高风险点确认
   - 测试场景讨论

3. **行动项确认** (10分钟)
   - 修复任务分配
   - 复查时间确认

### 6.2 参会人员建议

- 后端开发 (代码作者)
- 前端开发 (相关模块)
- 测试负责人
- 技术负责人
- Code Reviewer

### 6.3 会前准备

- [ ] 代码已提交到审查分支
- [ ] 相关文档已更新
- [ ] 测试用例已准备
- [ ] 环境已部署测试版本

---

## 七、模板使用说明

1. Sprint 3 开发过程中，按模块逐步更新清单状态
2. 开发完成后，Code Reviewer 依据此清单执行审查
3. 审查发现的问题记录到 `Sprint3_xxx_代码审查报告.md`
4. 修复完成后更新状态并重新审查

---

**创建时间**: 2026-04-13  
**下次更新**: Sprint 3 开发开始时
