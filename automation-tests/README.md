# 接口自动化测试框架

## 项目说明

基于 Python + Requests + Pytest + Allure 的接口自动化测试框架，用于票务App后端接口测试。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Python | 3.10+ | 运行环境 |
| requests | 2.31.0 | HTTP客户端 |
| pytest | 7.4.0 | 测试框架 |
| pytest-allure | - | Allure报告集成 |
| pytest-html | 4.1.1 | HTML报告 |

## 项目结构

```
automation-tests/
├── api-tests/
│   ├── tests/          # 测试用例
│   │   ├── __init__.py
│   │   ├── conftest.py
│   │   ├── test_user.py
│   │   ├── test_ticket.py
│   │   ├── test_order.py
│   │   ├── test_cart.py
│   │   └── test_coupon.py
│   ├── utils/          # 工具类
│   │   ├── __init__.py
│   │   ├── http_client.py
│   │   ├── logger.py
│   │   ├── asserts.py
│   │   └── token_manager.py
│   ├── data/          # 测试数据
│   │   ├── __init__.py
│   │   └── test_data.py
│   ├── config.py      # 配置
│   └── runner.py      # 运行器
├── requirements.txt
├── pytest.ini
└── README.md
```

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境

编辑 `api-tests/config.py` 修改环境配置：

```python
# 测试环境
BASE_URL = "http://localhost:8080"
```

### 3. 运行测试

```bash
# 运行所有测试
pytest api-tests/tests/

# 运行指定模块
pytest api-tests/tests/test_user.py

# 生成Allure报告
pytest api-tests/tests/ --alluredir=./allure-results
allure serve allure-results
```

## 测试模块

| 模块 | 说明 |
|------|------|
| test_user.py | 用户模块：注册、登录、实名认证 |
| test_ticket.py | 票务模块：列表、详情、场次 |
| test_order.py | 订单模块：创建、支付、取消 |
| test_cart.py | 购物车模块 |
| test_coupon.py | 优惠券模块 |

## 接口规范

### 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1709980800000,
  "requestId": "abc123"
}
```

### 错误码规范

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1001xx | 用户模块错误 |
| 1002xx | 票务模块错误 |
| 1003xx | 订单模块错误 |
| 1004xx | 支付模块错误 |
| 2001xx | 系统错误 |

## 业务规则

详见 `docs/业务规则文档.md`

### 订单状态

| 状态码 | 说明 |
|--------|------|
| 1 | 待支付 |
| 2 | 已支付 |
| 3 | 已出票 |
| 4 | 已使用 |
| 5 | 已取消 |
| 6 | 已退款 |

### 退款规则

| 距离演出时间 | 退款比例 |
|-------------|---------|
| ≥7天 | 100% |
| 3-7天 | 70% |
| 1-3天 | 50% |
| <1天 | 0% |

## 报告示例

```
测试概况
========
用例总数: 100
通过: 95
失败: 3
未执行: 2
```

## 维护者

- 测试团队