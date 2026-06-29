# Sprint 2 代码审查报告 - 票务模块

## 审查信息

| 项目 | 内容 |
|------|------|
| 审查日期 | 2026-04-13 |
| 审查人 | Code Reviewer (Senior Full-Stack Engineer Agent) |
| Sprint | Sprint 2 |
| 审查范围 | 后端票务模块 + 前端票务页面 |
| 审查阶段 | 开发完成Review |

---

## 一、审查汇总

### 1.1 总体评价

| 指标 | 评分 | 说明 |
|------|------|------|
| 代码规范 | 7/10 | VO类内联、类型使用any、魔法数字 |
| 安全性 | 7/10 | JWT密钥硬编码、SQL注入风险 |
| 性能 | 8/10 | 分页合理、索引使用正确 |
| 可维护性 | 7/10 | 代码重复、组件较大 |
| **总体评分** | **7.25/10** | 良好，需修复中等问题 |

### 1.2 问题统计

| 严重程度 | 数量 | 状态 |
|----------|------|------|
| 🔴 严重问题 | 1 | 待修复 |
| 🟡 中等问题 | 6 | 待修复 |
| 🟢 优化建议 | 8 | 可选修复 |

---

## 二、问题详情记录

### 🔴 严重问题 (必须修复)

| # | 文件位置 | 问题描述 | 建议修复方案 | 状态 |
|---|----------|---------|-------------|------|
| 1 | `JwtUtils.java:24` | JWT密钥硬编码默认值 | 必须从环境变量或配置中心获取 | ☐ 待修复 |

#### 问题详情

**问题1: JWT密钥硬编码默认值**
- **文件:** `com/ticket/app/common/util/JwtUtils.java`
- **位置:** 第24行
- **问题:** 
```java
@Value("${jwt.secret:booking-api-secret-key-change-in-production-2026}")
private String secret;
```
默认值密钥直接暴露在代码中，生产环境如果未配置环境变量会使用默认密钥，存在严重安全风险。

- **风险:** 如果部署时忘记配置JWT密钥，系统会使用弱默认密钥，攻击者可伪造任意Token。

- **建议:** 
1. 删除默认值，配置必填
2. 启动时校验密钥强度
3. 使用至少32字节的随机密钥

```java
// ✅ 正确示例
@Value("${jwt.secret}")
private String secret;

@PostConstruct
public void validateSecret() {
    if (secret == null || secret.length() < 32) {
        throw new IllegalStateException("JWT密钥必须配置且长度不少于32字符");
    }
}
```

---

### 🟡 中等问题 (建议修复)

| # | 文件位置 | 问题描述 | 建议修复方案 | 状态 |
|---|----------|---------|-------------|------|
| 1 | `TicketMapper.xml:61` | LIKE查询未转义特殊字符 | 需对keyword进行SQL转义 | ☐ 待修复 |
| 2 | `TicketServiceImpl.java` | VO类内联在Service实现 | 抽取到独立VO类 | ☐ 待修复 |
| 3 | `TicketController.java:101` | /search与/list代码重复 | 抽取共用逻辑 | ☐ 待修复 |
| 4 | `TicketServiceImpl.java:47` | 缺少参数校验 | 添加@Valid注解和校验 | ☐ 待修复 |
| 5 | `TicketListScreen.tsx:71` | 使用any类型 | 使用明确的类型定义 | ☐ 待修复 |
| 6 | `TicketMapper.xml:93` | 缺少分页上限限制 | 添加pageSize上限校验 | ☐ 待修复 |

#### 问题详情

**问题1: SQL注入风险 - LIKE查询未转义**
- **文件:** `TicketMapper.xml`
- **位置:** 第61行
```xml
<if test="keyword != null and keyword != ''">
    AND (name LIKE CONCAT('%', #{keyword}, '%') OR sub_name LIKE CONCAT('%', #{keyword}, '%'))
</if>
```
- **问题:** 用户输入的特殊字符（如 `%`、`_`、`\`）未被转义，可能导致意外的查询结果或性能问题。

- **建议:** 
```xml
<if test="keyword != null and keyword != ''">
    AND (
        name LIKE CONCAT('%', REPLACE(REPLACE(#{keyword}, '\\', '\\\\'), '%', '\\%'), '%')
        OR sub_name LIKE CONCAT('%', REPLACE(REPLACE(#{keyword}, '\\', '\\\\'), '%', '\\%'), '%')
    )
</if>
```
或在Service层先转义。

---

**问题2: VO类内联违反分层设计**
- **文件:** `TicketServiceImpl.java`
- **位置:** 第204-325行
- **问题:** `TicketListVO`、`TicketDetailVO`、`PriceLevelVO` 内联在Service实现类中。

- **风险:** 
  - 违反单一职责原则
  - VO类无法被其他模块复用
  - Service类过于庞大（326行）

- **建议:** 抽取到 `vo/ticket/` 目录下的独立文件。

---

**问题3: Controller代码重复**
- **文件:** `TicketController.java`
- **位置:** 第101-114行
- **问题:** `/search` 端点完全调用了 `getTicketList`，只是参数不同。

```java
@GetMapping("/search")
public Result<PageResult<?>> search(...) {
    // 代码与下方getTicketList几乎完全相同
    PageResult<?> result = ticketService.getTicketList(null, keyword, ...);
    return Result.success(result);
}
```

- **建议:** 
```java
@GetMapping("/search")
public Result<PageResult<?>> search(@RequestParam String keyword, ...) {
    // 复用 list 方法，内部可抽取私有方法
    return list(null, keyword, startDate, endDate, minPrice, maxPrice, 
                sortBy, sortOrder, pageNum, pageSize);
}
```

---

**问题4: Service层缺少参数校验**
- **文件:** `TicketServiceImpl.java`
- **位置:** 第35-78行 `getTicketList` 方法
- **问题:** 未校验分页参数范围、日期格式等。

```java
public PageResult<?> getTicketList(
    Integer categoryId,
    String keyword,
    String startDate,
    String endDate,
    Double minPrice,
    Double maxPrice,
    String sortBy,
    String sortOrder,
    Integer pageNum,
    Integer pageSize
) {
    // 缺少校验：pageSize可能为负数或极大值
```

- **建议:** 添加参数校验
```java
public PageResult<?> getTicketList(...) {
    // 参数校验
    if (pageNum == null || pageNum < 1) pageNum = 1;
    if (pageSize == null || pageSize < 1) pageSize = 10;
    if (pageSize > 100) pageSize = 100; // 设置上限
    
    // 日期校验
    if (startDate != null && endDate != null) {
        if (LocalDate.parse(startDate).isAfter(LocalDate.parse(endDate))) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
    }
    ...
}
```

---

**问题5: 前端使用any类型**
- **文件:** `TicketListScreen.tsx`
- **位置:** 第71行
```typescript
const [ticketList, setTicketList] = useState<any[]>([]);
```

- **问题:** 失去TypeScript类型安全优势。

- **建议:** 定义并使用具体类型
```typescript
import type { Ticket } from '../../types';

const [ticketList, setTicketList] = useState<Ticket[]>([]);
```

---

**问题6: 缺少分页上限**
- **文件:** `TicketMapper.xml`
- **位置:** 第93行附近
- **问题:** `pageSize` 参数无上限，可被设置为极大值。

- **建议:** 在Service层或SQL中限制
```xml
<if test="pageSize > 100">
    LIMIT 100
</if>
```

---

## 三、优化建议 (可选修复)

| # | 文件位置 | 优化建议 | 预期效果 | 状态 |
|---|----------|---------|---------|------|
| 1 | `Ticket.java:217-221` | 状态常量已定义但未使用 | 统一使用常量替代魔法数字 | ☐ 待处理 |
| 2 | `TicketServiceImpl.java:187-199` | getStatusName可使用Ticket常量 | 消除重复代码 | ☐ 待处理 |
| 3 | `TicketListScreen.tsx` | TicketCard组件可使用memo | 减少不必要的重渲染 | ☐ 待处理 |
| 4 | `ticketApi.ts` | 响应类型使用any | 定义统一响应类型 | ☐ 待处理 |
| 5 | `TicketController.java` | 缺少接口级@CrossOrigin | 方便前端跨域调试 | ☐ 待处理 |
| 6 | `TicketServiceImpl.java` | 缺少作者@author注释 | 明确代码负责人 | ☐ 待处理 |
| 7 | `TicketListScreen.tsx:119` | useEffect依赖数组不完整 | 避免闭包陷阱 | ☐ 待处理 |
| 8 | `TicketMapper.xml` | SQL缺少SQL注释 | 便于维护 | ☐ 待处理 |

---

## 四、代码亮点

| # | 文件位置 | 亮点描述 |
|---|----------|---------|
| 1 | `UserServiceImpl.java:105-107` | 登录失败锁定机制完善，防止暴力破解 |
| 2 | `TicketMapper.xml:54-56` | 使用deleted_at软删除标识，数据安全 |
| 3 | `TicketMapper.xml:56,104` | 只查询status=2(售票中)的票务，合理的数据筛选 |
| 4 | `storage.ts` | 使用MMKV替代AsyncStorage，性能优秀 |
| 5 | `http/index.ts` | 完善的响应拦截器，统一错误处理 |
| 6 | `TicketListScreen.tsx` | FlatList正确使用keyExtractor、分页加载 |
| 7 | `JwtUtils.java` | Token过期检测机制完善 |
| 8 | `Ticket.java:217-221` | 状态常量定义规范，便于维护 |

---

## 五、审查结论

### 审查结论

- [ ] ✅ 可以合并到主分支
- [x] ⚠️ **需要修复严重问题后重新审查**
- [ ] ❌ 需要重构后重新审查

### 后续行动

| 行动项 | 负责人 | 截止日期 | 优先级 |
|--------|--------|----------|--------|
| 修复JWT密钥硬编码问题 | 后端开发 | 2026-04-14 | P0 |
| 修复SQL注入风险 (LIKE转义) | 后端开发 | 2026-04-14 | P1 |
| 添加参数校验 | 后端开发 | 2026-04-15 | P1 |
| 抽取VO类到独立文件 | 后端开发 | 2026-04-15 | P2 |
| 修复前端any类型 | 前端开发 | 2026-04-15 | P2 |

### 审查备注

```
1. 整体代码质量良好，遵循了大部分编码规范
2. 安全意识需要加强，特别是JWT密钥管理
3. 前端使用Mock数据，需要确保后续与真实API对接时类型一致
4. 建议Sprint 3开始前完成上述问题的修复
```

---

## 六、附录：检查清单状态

### 后端代码规范检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ 类名使用PascalCase | 通过 | TicketController, TicketService |
| ✅ 方法名使用camelCase | 通过 | getTicketList, getTicketDetail |
| ✅ 变量命名有意义 | 通过 | ticketId, pageNum等命名清晰 |
| ⚠️ 代码格式和缩进 | 通过 | 需使用格式化工具统一 |
| ⚠️ 类注释完整性 | 部分通过 | TicketServiceImpl缺少@author |
| ❌ VO类抽取 | 需修复 | VO类内联在Service中 |

### 后端安全检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ 密码加密存储 | 通过 | 使用BCrypt |
| ✅ SQL注入防护 | 需修复 | LIKE查询需转义 |
| ✅ XSS防护 | 通过 | API返回JSON不涉及HTML |
| ❌ JWT Token安全 | 需修复 | 密钥硬编码默认值 |
| ✅ 登录失败限制 | 通过 | 5次失败锁定30分钟 |

### 后端性能检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ 分页查询 | 通过 | 使用MyBatis-Plus分页 |
| ⚠️ 分页上限 | 需修复 | pageSize无上限 |
| ✅ 索引使用 | 通过 | 查询字段有索引支持 |

### 后端最佳实践检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ Controller层职责单一 | 通过 | 只处理请求响应 |
| ⚠️ 参数校验 | 需修复 | Service层缺少校验 |
| ✅ 异常处理统一 | 通过 | 使用BusinessException |
| ⚠️ VO类分层 | 需修复 | VO应独立 |

### 前端代码规范检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ 组件命名规范 | 通过 | PascalCase命名 |
| ⚠️ TypeScript类型定义 | 需完善 | 存在any类型 |
| ✅ 代码格式 | 通过 | 风格统一 |

### 前端安全检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ Token存储安全 | 通过 | 使用MMKV |
| ✅ 请求拦截器 | 通过 | 自动添加Authorization |

### 前端性能检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ FlatList使用 | 通过 | 正确使用keyExtractor |
| ⚠️ useEffect依赖 | 需修复 | 依赖数组不完整 |
| ⚠️ 组件memo | 建议 | 可优化重渲染 |

### 前端最佳实践检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| ✅ Redux Toolkit | 通过 | 结构清晰 |
| ✅ API封装 | 通过 | 统一http服务 |

---

## 七、Sprint 3 审查准备建议

### 7.1 Sprint 3 预期功能
基于 `票多星项目的故事.txt`，Sprint 3 可能涉及：
- 订单模块
- 支付模块
- 用户收藏功能
- 购物车功能

### 7.2 审查重点
1. **订单模块**：并发控制、幂等性、库存超卖防护
2. **支付模块**：回调验签、幂等处理、状态同步
3. **分布式锁**：确保Redis/数据库锁正确使用

### 7.3 Sprint 3 开发规范建议

```markdown
## Sprint 3 代码规范要点

### 订单模块必检项
- [ ] 下单前库存校验
- [ ] 库存扣减使用原子操作或分布式锁
- [ ] 订单号唯一性（幂等键）
- [ ] 超时订单自动取消
- [ ] 事务边界明确

### 支付模块必检项
- [ ] 支付回调验签
- [ ] 回调幂等处理
- [ ] 支付状态与订单状态一致性
- [ ] 订单状态机正确性

### 通用安全检查
- [ ] 金额计算使用BigDecimal
- [ ] 敏感数据脱敏
- [ ] 操作日志记录
```

---

**审查报告生成时间**: 2026-04-13 22:58  
**下次审查预定**: Sprint 3 开发完成后
