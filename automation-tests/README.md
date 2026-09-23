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

默认环境为 `local`；以下命令可安全地显式选择环境并仅收集用例：

```bash
python -m pytest --collect-only -q --env=local
python -m pytest --collect-only -q --env=public
```

实际执行时必须选择测试套件；若该套件包含 `write` 测试，还必须提供 `--allow-write` 和 `COURSE_TEST_KEY`。

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

当前 `pytest.ini` 定义九个业务标记，框架及 xdist 插件还动态注册以下表内的交易门禁和分组标记：

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
| `destructive_commerce` | 库存、支付或优惠券交易，另需 `--allow-destructive-commerce`；只有补偿清理部署及验收通过后才可在线执行 |
| `xdist_group` | xdist 同组执行约束；E2E 默认使用 `-n 0` 串行 |

旧 API 测试保存在 `legacy/api-tests-v1`，默认 `tests` 收集范围不包含它。显式把该归档目录作为 pytest 参数会绕过默认收集范围，仍能收集旧用例；不要直接执行归档套件。

## 套件命令与报告

在本目录通过 PowerShell 运行，每次生成独立的报告目录：

```powershell
./scripts/run-suite.ps1 -Suite framework
./scripts/run-suite.ps1 -Suite api-read -Environment public
./scripts/run-suite.ps1 -Suite api-write -Environment local -AllowWrite
./scripts/run-suite.ps1 -Suite customer-ui -AllowWrite
./scripts/run-suite.ps1 -Suite admin-ui -AllowWrite
./scripts/run-suite.ps1 -Suite smoke
./scripts/run-suite.ps1 -Suite e2e -AllowWrite -AllowDestructiveCommerce
./scripts/run-suite.ps1 -Suite full -AllowWrite -AllowDestructiveCommerce
```

`api-read` 包含管理员查询，需要 `BOOKING_ADMIN_USERNAME` / `BOOKING_ADMIN_PASSWORD`；`api-write`、`admin-ui`、`e2e`、`full` 同样需要管理员凭据。写套件还需要 `COURSE_TEST_KEY`。脚本先检查开关和变量是否齐备，再由框架预检清理权限。凭据由进程/Jenkins 注入，不在命令或文件中填写。

`api-write`、`customer-ui`、`admin-ui` 默认排除会消耗库存/优惠券的 commerce 用例；订单、支付、优惠券发放等完整业务通过 `full` 和串行 `e2e` 执行，必须提供第二个显式开关。客户/管理员 UI 默认 Chromium；`smoke` 运行客户只读跨浏览器冒烟，需安装 Chromium、Firefox、WebKit。当前命令统一串行，防止完整回归中的共享业务并发相互干扰。

加 `-DryRun` 只打印参数数组，不运行测试、不创建输出目录；加 `-CollectOnly` 只收集用例。查看 `-Help` 获取选项。套件脚本保留 pytest 退出码，JUnit、HTML、Allure 原始结果与 `summary.json` 保存在本次目录。Allure CLI 生成 HTML 是单独步骤。

失败用例保留截图、视频及裁剪后的 JSON 动作时间线；完整 trace 默认不归档。清理失败作为独立 teardown error 保留。详情、脱敏边界与 Jenkins 保留建议见 [报告与证据口径](docs/reporting-and-evidence.md)。当前离线验证不代表浏览器、公网回归或 Jenkins 已实跑通过。

## 本阶段基线

2026-09-23 的 Task 9 验证为 396 项离线框架测试通过、local/public 各 504 项可收集，公网业务套件未执行。已实际生成 Allure HTML，并验证本机 Chromium 离线启动；Firefox/WebKit 尚未安装。当前发布仍受凭据、清理部署、历史库存事实审计及源码敏感默认整改阻断。完整命令、证据与下一步门禁见 [阶段验证记录](docs/phase-verification.md)。

完整 Java reactor 当前存在五个与本改造无关、预先存在的 order 模块失败。本阶段以聚焦的 test-support/security 测试以及上述 Python 框架测试为质量门禁，而不是以完整 reactor 全绿作为前提。
