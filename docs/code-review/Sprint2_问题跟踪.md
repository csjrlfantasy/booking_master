# Sprint 2 问题跟踪记录

## 审查日期: 2026-04-13

---

## 待修复问题

### P0 - 严重问题 (必须立即修复)

| ID | 问题 | 文件 | 修复人 | 截止日期 | 状态 | 验证人 |
|----|------|------|--------|----------|------|--------|
| P0-1 | JWT密钥硬编码 | JwtUtils.java | | 2026-04-14 | ☐ 待修复 | |

**P0-1 详情:**
```
问题: JWT密钥使用硬编码默认值
位置: com/ticket/app/common/util/JwtUtils.java:24
当前代码:
@Value("${jwt.secret:booking-api-secret-key-change-in-production-2026}")
private String secret;

修复要求:
1. 删除默认值，改为必填配置
2. 添加@PostConstruct校验密钥强度
3. 密钥长度不少于32字节
```

---

### P1 - 高优先级问题 (Sprint内修复)

| ID | 问题 | 文件 | 修复人 | 截止日期 | 状态 | 验证人 |
|----|------|------|--------|----------|------|--------|
| P1-1 | SQL注入风险 | TicketMapper.xml | | 2026-04-14 | ☐ 待修复 | |
| P1-2 | 缺少参数校验 | TicketServiceImpl.java | | 2026-04-15 | ☐ 待修复 | |
| P1-3 | 缺少分页上限 | TicketServiceImpl.java | | 2026-04-15 | ☐ 待修复 | |

**P1-1 详情:**
```
问题: LIKE查询未转义特殊字符
位置: booking-api/src/main/resources/mapper/TicketMapper.xml:61
修复建议: 使用REPLACE函数转义%和_字符
```

**P1-2 详情:**
```
问题: 分页参数和日期参数缺少校验
位置: TicketServiceImpl.getTicketList()
修复建议: 添加参数范围校验，pageSize上限100
```

**P1-3 详情:**
```
问题: pageSize无上限，可能导致全表查询
位置: TicketServiceImpl.getTicketList()
修复建议: if (pageSize > 100) pageSize = 100;
```

---

### P2 - 中优先级问题 (建议修复)

| ID | 问题 | 文件 | 修复人 | 截止日期 | 状态 | 验证人 |
|----|------|------|--------|----------|------|--------|
| P2-1 | VO类内联 | TicketServiceImpl.java | | 2026-04-15 | ☐ 待修复 | |
| P2-2 | Controller代码重复 | TicketController.java | | 2026-04-15 | ☐ 待修复 | |
| P2-3 | 前端any类型 | TicketListScreen.tsx | | 2026-04-15 | ☐ 待修复 | |

**P2-1 详情:**
```
问题: TicketListVO、TicketDetailVO、PriceLevelVO内联在ServiceImpl中
位置: TicketServiceImpl.java:204-325
修复建议: 抽取到 vo/ticket/ 目录下的独立类
```

**P2-2 详情:**
```
问题: /search端点与/list端点代码重复
位置: TicketController.java:101-114
修复建议: 抽取共用方法或直接复用
```

**P2-3 详情:**
```
问题: useState<any[]> 类型过于宽泛
位置: TicketListScreen.tsx:71
修复建议: 导入并使用Ticket类型
```

---

### P3 - 低优先级 (可选修复)

| ID | 问题 | 文件 | 修复人 | 截止日期 | 状态 | 验证人 |
|----|------|------|--------|----------|------|--------|
| P3-1 | 状态常量未使用 | Ticket.java | | - | ☐ 可选 | |
| P3-2 | useEffect依赖不完整 | TicketListScreen.tsx | | - | ☐ 可选 | |
| P3-3 | 缺少@author注释 | TicketServiceImpl.java | | - | ☐ 可选 | |
| P3-4 | 缺少SQL注释 | TicketMapper.xml | | - | ☐ 可选 | |
| P3-5 | TicketCard可memo化 | TicketListScreen.tsx | | - | ☐ 可选 | |

---

## 已修复问题

| ID | 问题 | 修复日期 | 验证人 | 验证日期 |
|----|------|----------|--------|----------|
| - | - | - | - | - |

---

## 问题统计

```
P0: 1 待修复
P1: 3 待修复  
P2: 3 待修复
P3: 5 可选

总计: 12 问题
```

---

## 更新日志

| 日期 | 更新内容 | 更新人 |
|------|---------|--------|
| 2026-04-13 | 初始创建问题清单 | Code Reviewer |

