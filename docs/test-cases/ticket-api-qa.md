# 票务模块API测试用例

## 测试环境
- 基础URL: http://localhost:8080
- API前缀: /api

## 测试前置条件
用户需先注册登录获取Token：
```bash
# 1. 注册
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser001","password":"Pass1234","phone":"13800000001"}'

# 2. 登录获取Token
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser001","password":"Pass1234"}'
```

---

## TC001: 用户登录

| 项目 | 内容 |
|------|------|
| 用例ID | TC001 |
| 用例名称 | 用户登录-POST /api/user/login |
| 优先级 | P0 |
| 前置条件 | 用户已注册 |

### 测试步骤
```bash
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser001","password":"Pass1234"}'
```

### 预期结果
- HTTP状态码: 200
- 响应code: 0
- 返回数据包含: userId, username, token

---

## TC002: 获取票务列表

| 项目 | 内容 |
|------|------|
| 用例ID | TC002 |
| 用例名称 | 获取票务列表-GET /api/tickets |
| 优先级 | P0 |
| 前置条件 | 已登录并获取Token |

### 测试步骤
```bash
# 将 <TOKEN> 替换为登录获取的Token
curl -X GET http://localhost:8080/api/tickets \
  -H "Authorization: Bearer <TOKEN>"
```

### 预期结果
- HTTP状态码: 200
- 响应code: 0
- 返回分页数据: list, total, pageNum, pageSize

### 边界条件
- 无Token: 返回401未登录
- Token过期: 返回401

---

## TC003: 获取票务详情

| 项目 | 内容 |
|------|------|
| 用例ID | TC003 |
| 用例名称 | 获取票务详情-GET /api/tickets/{id} |
| 优先级 | P0 |
| 前置条件 | 已登录 |

### 测试步骤
```bash
curl -X GET http://localhost:8080/api/tickets/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### 预期结果 (有数据时)
- code: 0
- data: 包含ticketId, name, venueName, showTime等

### 预期结果 (无数据时)
- code: 404
- message: 票务不存在

---

## TC004: 获取分类列表

| 项目 | 内容 |
|------|------|
| 用例ID | TC004 |
| 用例名称 | 获取分类列表-GET /api/categories |
| 优先级 | P1 |
| 前置条件 | 已登录 |

### 测试步骤
```bash
curl -X GET http://localhost:8080/api/categories \
  -H "Authorization: Bearer <TOKEN>"
```

### 预期结果
- code: 0
- data: 分类数组

### 当前状态
⚠️ 返回500错误 - 需要开发修复

---

## 测试数据

### 注册用户
```json
{
  "username": "testuser001",
  "password": "Pass1234",
  "phone": "13800000001"
}
```

### Token (当前有效)
```
eyJhbGciOiJIUzM4NCJ9.eyJ1c2VySWQiOjgsInVzZXJuYW1lIjoidGVzdHVzZXIwMDEiLCJzdWIiOiI4LXRlc3R1c2VyMDAxIiwiaWF0IjoxNzc1OTIxMjgyLCJleHAiOjE3NzYwMDc2ODJ9.p8SaUS-bGrkwrnVd1FX3oTh4r5MM5I9OwxuQY8Dg7_lm1K6agXtIZdDa9LzXYkox
```

---

## 执行结果

| 用例ID | 接口 | 状态 | 说明 |
|--------|------|------|------|
| TC001 | POST /api/user/login | ✅ PASS | 登录成功返回token |
| TC002 | GET /api/tickets | ✅ PASS | 返回空列表(无测试数据) |
| TC003 | GET /api/tickets/1 | ⚠️ PASS | 无数据返回404 |
| TC004 | GET /api/categories | ❌ FAIL | 500系统繁忙 |

---

## Bug列表

| BugID | 描述 | 严重程度 | 状态 |
|------|------|----------|------|
| BUG001 | /api/categories 返回500 | High | Open |
| BUG002 | 票务表无测试数据 | Medium | 待初始化 |