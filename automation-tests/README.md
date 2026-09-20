# BookingApi Playwright 自动化测试运行手册

## 当前框架能力

本目录提供面向 BookingApi 的安全自动化测试基础设施：

- `local` 与 `public` 两套非敏感配置；
- 统一 API 客户端与 BookingApi 业务封装；
- 每个测试独立的 `runId` 测试数据和数据工厂；
- 仅按已登记资源执行的受保护清理；
- 写操作标记、显式 `--allow-write` 开关和课程测试密钥校验；
- 临时客户夹具：注册、登录后提供已认证会话，且不保留明文密码；
- 在创建首个页面之前向浏览器上下文注入客户令牌及会话信息；
- Pytest HTML、Allure 报告配置与测试标记。

## 目录

```text
automation-tests/
├── config/                 # local/public 配置及配置 schema
├── framework/              # 配置、API、认证、测试数据和清理基础设施
├── tests/
│   ├── framework/          # 不依赖服务端的框架单元测试
│   └── e2e/                # 生命周期端到端测试
├── conftest.py             # 共享夹具与完整生命周期
└── pytest.ini              # Pytest、报告和标记配置
```

## 安装

需要 Python 3.11 或更高版本。在本目录执行：

```bash
python -m pip install -r requirements.txt
python -m playwright install chromium
```

## 选择环境与执行

默认环境为 `local`；也可显式选择：

```bash
python -m pytest --env=local
python -m pytest --env=public
```

以下收集命令不需要任何密钥，也不会连接服务端：

```bash
python -m pytest --collect-only -q --env=local
```

运行框架测试：

```bash
python -m pytest tests/framework -q
```

公共环境的临时客户生命周期命令（用于 Task 9B 的在线验证，**不代表本阶段已通过**）：

```bash
python -m pytest tests/e2e/test_temporary_customer_lifecycle.py --env=public --allow-write -v
```

该公共生命周期命令还要求进程环境或 Jenkins 凭据中提供 `COURSE_TEST_KEY`；密钥只能来自这些受控来源，绝不能写入源码、配置或文档。

## 临时客户生命周期

设置阶段按以下顺序完成：API 注册临时用户，立即把返回的精确 ID 登记到清理注册表，API 登录，再在打开第一个页面之前把令牌和客户会话信息注入浏览器上下文。

拆除阶段先对该临时用户执行受保护的单次 `DELETE`，随后按该精确 ID 验证用户已不存在。无论测试结果如何，清理注册表都会作为兜底执行可幂等的批量清理，只处理本次 `runId` 已登记的资源。

公共环境写入必须同时满足：测试带 `write` 标记、命令显式带 `--allow-write`、进程/Jenkins 中存在非空 `COURSE_TEST_KEY`，且执行前先调用空清理预检验证受控清理端点。任一条件不满足即拒绝执行。

## 标记

当前 `pytest.ini` 定义九个标记：

| 标记 | 用途 |
| --- | --- |
| `smoke` | 冒烟验证 |
| `regression` | 回归测试 |
| `api` | API 测试 |
| `ui` | 用户界面测试 |
| `e2e` | 端到端流程测试 |
| `write` | 创建、修改或删除数据的测试，受写入保护 |
| `public` | 面向公共环境的测试 |
| `chromium` | Chromium 浏览器测试 |
| `cross_browser` | 跨浏览器测试 |

## 报告与后续失败产物

Pytest 已配置 HTML 与 Allure 报告输出目录，并通过标记支持选择测试范围。截图、trace、视频及更细粒度的失败诊断产物将在后续 suite 任务中补齐；本手册不将它们描述为当前已完整实现的能力。

## 本阶段基线

完整 Java reactor 当前存在五个与本改造无关、预先存在的 order 模块失败。本阶段以聚焦的 test-support/security 测试以及上述 Python 框架测试为质量门禁，而不是以完整 reactor 全绿作为前提。
