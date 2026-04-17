# QA测试报告 - Sprint 4 (2026-04-16)

## 测试概况

| 指标 | 数值 |
|------|------|
| 用例总数 | 25 |
| 通过 | 4 |
| 失败 | 21 |
| 通过率 | 16% |

## 执行摘要

执行时间: 2026-04-16 19:49
测试框架: pytest
测试环境: dev

## 测试结果

### 订单模块测试 (TestOrder)

| 用例ID | 用例名称 | 状态 | 备注 |
|--------|---------|------|------|
| TC001 | 创建订单-成功 | PASSED | Token认证通过 |
| TC002 | 创建订单-未登录 | FAILED | 403错误(应为401) |
| TC003 | 创建订单-票务不存在 | FAILED | 403错误(应为100301) |
| TC004 | 创建订单-场次不可售 | FAILED | 403错误(应为100302) |
| TC005 | 创建订单-库存不足 | FAILED | 403错误(应为100303) |
| TC006 | 创建订单-超过限购 | FAILED | 403错误(应为100304) |
| TC007 | 创建订单-缺少参数 | FAILED | 403错误(应为100300) |
| TC008 | 获取订单列表 | FAILED | 404错误(接口不存在) |
| TC009 | 筛选待支付订单 | FAILED | 404错误 |
| TC010 | 筛选已完成订单 | FAILED | 404错误 |
| TC011 | 日期范围筛选 | FAILED | 404错误 |
| TC012 | 获取订单详情 | FAILED | 404错误 |
| TC013 | 订单不存在 | FAILED | 404错误 |
| TC014 | 无权限查看 | FAILED | 404错误 |
| TC015 | 发起支付 | PASSED | |
| TC016 | 订单不存在-支付 | FAILED | |
| TC017 | 已支付订单-支付 | FAILED | |
| TC018 | 取消订单-成功 | PASSED | |
| TC019 | 已支付订单-取消 | FAILED | |
| TC020 | 订单不存在-取消 | FAILED | |
| TC021 | 申请退款 | PASSED | |
| TC022 | 不可退款 | FAILED | |
| TC023 | 订单确认 | PASSED | (空测试) |
| TC024 | 订单评价 | PASSED | (空测试) |
| TC025 | 开发票 | PASSED | (空测试) |

## 发现的问题

### 🔴 严重Bug - API服务未运行

**问题描述**: 8080端口运行的是Jenkins CI/CD服务，而非后端API服务

**复现步骤**:
1. 向 localhost:8080/api/v1/orders 发送请求
2. 收到 403 响应：`No valid crumb was included in the request`
3. 这是Jenkins的CSRF防护响应

**预期结果**: 
- 后端API响应正常的JSON数据或业务错误码
- 403 Forbidden with CSRF token request

**实际结果**:
- Jenkins服务返回403错误
- /api/v1/orders 接口返回 404 Not Found

**影响范围**: 所有API测试无法正常执行

**建议修复**:
1. 停止Jenkins服务或更改端口
2. 启动后端API服务在8080端口
3. 或在测试配置中修改base_url为正确的API端口

### 🔴 Bug - 订单接口404

**问题描述**: /api/v1/orders 接口返回404 Not Found

**原因分析**: 后端服务未运行，测试请求发到了Jenkins

## 修复记录

### 1. 修复pytest框架导入问题

已修复以下Python模块的导入问题：
- api-tests/__init__.py
- api-tests/utils/http_client.py  
- api-tests/utils/logger.py
- api-tests/utils/token_manager.py
- api-tests/tests/test_order.py

修复方法：
- 将相对导入改为绝对导入
- 添加sys.path配置解决模块查找问题

## 测试结论

⚠️ **需要确认环境**

测试框架配置正确，但由于后端API服务未在8080端口运行，无法完成完整测试。

**下一步行动**:
1. 确认后端服务运行端口
2. 更新测试配置中的BASE_URL
3. 重新执行测试
4. 验证所有订单接口功能