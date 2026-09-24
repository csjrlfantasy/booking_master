# Phase 2 验证与发布证据

## 结论与范围

2026-09-23 离线实现验证通过，公网业务发布为 **NO-GO**。本次执行了 396 项框架测试，全部通过；local/public 均收集到 504 项。504 是收集数量，不是线上通过数量。公网只执行未认证 GET 连通检查，未创建用户、下单、清理数据或修改服务器/Jenkins/GitHub。

本机 Chromium 的离线页面验证和 Allure HTML 生成已实际通过；客户/管理员真实 UI、交易 E2E、Firefox/WebKit、服务端补偿清理及 Jenkins 归档尚未验收。三项 strict-xfail 业务用例只被收集，未执行，不能报告为已观察到的 xfail。离线执行的 skipped、xfail、xpass 均为 0。

## 可复现基线

- 验证代码提交：`573d74a8ffaa6c2e46e0efb07e6782175eb10cd1`。
- 分支：`codex/baseline-track-source`；开始时暂存区为空，存在 111 条无关工作区状态记录。完整工作区不是纯净 HEAD，端点源文件也有既存未提交修改，不能声称只检出该提交即可重现全部环境。
- 执行时间：2026-09-23 22:13 起，Asia/Shanghai（UTC+08:00）。本地命令在 `D:\AIProjects\BookingApi\automation-tests` 执行，Git 扫描在仓库根执行。
- Windows 10.0.26200，PowerShell 7.6.5，Python 3.14.7，Git 2.47.1.windows.1。
- pytest 8.4.2、Playwright 1.63.0、pytest-playwright 0.9.0、pytest-xdist 3.8.0、allure-pytest 2.16.0、pytest-html 4.2.0、Allure CLI 2.46.1。
- 环境选择：离线执行为 `local`；收集覆盖 `local`、`public`。配置 `public.yaml` 的 API 为公网 `/api`，页面入口为站点根。

## 本次命令与结果

| 命令 | 实际结果 | 证据口径 |
| --- | --- | --- |
| `python -m pytest tests/framework -q -o addopts= -o log_cli=false` | exit 0，396 passed，28.75 秒 | 执行离线 mock/框架/报告/门禁测试，不连接真实业务服务 |
| `python -m pytest --collect-only -q --env=local -o addopts= -o log_cli=false` | exit 0，504 collected | executed=0；默认 legacy nodeid=0 |
| `python -m pytest --collect-only -q --env=public -o addopts= -o log_cli=false` | exit 0，504 collected | executed=0；不验证凭据或接口可用性 |
| `./scripts/run-suite.ps1 -Suite <下表套件> -Environment public -CollectOnly` | 八个命令全部 exit 0 | 每个套件产生独立 collectionOnly 报告，零执行 |
| `python -m pytest legacy/api-tests-v1 --collect-only -q -o addopts= -o log_cli=false` | exit 0，150 collected | 显式指定目录会覆盖默认 testpaths；归档不是绝对禁止收集，不应执行 |
| `python -m pytest --markers -o addopts=` | exit 0 | 确认动态 `destructive_commerce` 与 `xdist_group` 注册 |
| `python -m playwright install --list` | exit 0 | 当前 1.63.0 有 Chromium 1243；Firefox/WebKit 缺失 |
| `python -c "from playwright.sync_api import sync_playwright; p=sync_playwright().start(); b=p.chromium.launch(headless=True); page=b.new_page(); page.set_content('<title>Offline prerequisite</title><main>ready</main>'); print('OFFLINE_CHROMIUM_LAUNCH=' + str(page.title() == 'Offline prerequisite')); b.close(); p.stop()"` | exit 0，True | 离线引擎启动和 DOM 验证；不是客户 UI 通过 |
| `./scripts/generate-report.ps1 -ResultsDirectory report/20260923T141310251392Z-local-6720ed333132/allure-results` | exit 0，已生成 `allure-html-a4d4356e79c4/index.html` | Allure HTML 实际生成，未声称已上传 Jenkins |
| `git diff --check` / `git diff --cached --check` / `git diff --check -- automation-tests` | 各 exit 0 | 全工作区有 52 行换行规范提示；无空白错误。开始时 automation-tests 无未提交修改 |
| `git remote` | 空 | 尚未配置远程，未推送或合并 |

| 收集套件 | 选中 / 原始数量 |
| --- | --- |
| framework | 396 / 396 |
| api-read | 21 / 73 |
| api-write | 33 / 73 |
| customer-ui | 12 / 17 |
| admin-ui | 11 / 14 |
| smoke | 12 / 51（Chromium、Firefox、WebKit 参数化） |
| e2e | 4 / 4（3 条 UI 跨层旅程加 1 条 API 临时客户生命周期） |
| full | 504 / 504 |

上述分套件数量有交集，不能相加当作唯一用例数量。full 默认 Chromium；客户/管理员单独套件排除 commerce，完整交易用例在 full/e2e 双开关套件中执行。

主要本机证据目录（相对于 automation-tests）：

- 离线执行：`report/20260923T141310251392Z-local-6720ed333132/`，含 summary、JUnit、HTML、Allure raw/HTML。
- local 收集：`report/20260923T141310221732Z-local-026e9702f5ac/`。
- public 收集：`report/20260923T141310251164Z-public-e33c870c0854/`。
- 八套件收集：`report/20260923T1416448668820Z-public-341907d58e8b/` 至 `report/20260923T1416569104388Z-public-9b8008f04bcb/`（按本次时间范围筛选，各有独立 summary）。

报告保存在忽略目录，不随源码提交；迁移矩阵的每条旧文件均已指向现有用例或说明性能负载业务调整。七条功能迁移记录为 `implemented-pending-live`，性能记录为 `business-changed`。

## 只读环境预检

执行 `Invoke-WebRequest -Method Get -TimeoutSec 10 -MaximumRedirection 0 -SkipHttpErrorCheck` 检查以下路径，均为 HTTP 200：客户 `/shop/login`、管理员 `/login`、`/api/tickets?page=1&pageSize=1`、Jenkins `:8080/login`。Jenkins 响应版本为 2.568.3。HTTP 200 仅证明路由响应，不代表业务断言、登录权限或部署版本正确；未保存响应正文。

使用 `TcpClient.ConnectAsync('154.12.20.250',22)`，5 秒限时，TCP 可达；本次未尝试 SSH 认证。前序记录的密码认证失败仍未解决，TCP 成功不能将 SSH 状态改为可用。Jenkins 登录页可达，管理权限、Agent 工具链、凭据与 Job 均未验证。

通过 `[Environment]::GetEnvironmentVariable` 仅检查非空状态：`COURSE_TEST_KEY`、`BOOKING_ADMIN_USERNAME`、`BOOKING_ADMIN_PASSWORD` 三项均缺失，不输出值。未调用受保护 cleanup POST/DELETE；因此远端 test-support 是否启用、当前密钥是否有效及补偿清理部署版本均未知。禁止通过省略标记、注入假密钥或绕过第二开关执行交易。

本机浏览器 executable_path 存在性检查：Chromium=True，Firefox=False，WebKit=False。CI Linux 节点浏览器和系统依赖尚未检查。

## 敏感默认与发布扫描

扫描 626 个 HEAD 跟踪文件，其中 automation-tests 110 个。先用 `git grep -n -I -E -e <pattern> HEAD`，再只输出匹配路径、行号、规则名；不输出匹配文本。规则包含私钥头、GitHub/AWS token 模式、三段 JWT 模式、敏感环境变量非空 fallback，以及 password/secret 字面量赋值。字面量规则也会命中 mock、变量名和示例，因此必须分流，不能按候选数判断真实泄漏数量。

发布扫描 **未通过**，存在必须在发布阶段处理的凭据默认/候选：

| 文件与行 | 判断及后续 |
| --- | --- |
| `booking-api/src/main/java/com/ticket/app/config/AdminAccountInitializer.java:18` | `DEFAULT_ADMIN_PASSWORD` 为源码字面量；管理员初始化须改为受控凭据并轮换已有账户 |
| `automation-tests/legacy/api-tests-v1/config.py:114` | `TEST_USER_PASSWORD` 为归档字面量；评估脱敏归档、保留基线的方式及历史发布范围 |
| `docs/superpowers/plans/2026-08-21-playwright-course-bookingapi-integration.md:320` | 旧计划包含课程密钥非空 fallback 示例；发布前清除或替换无效占位符，验证历史可见性 |
| `docs/test-cases/ticket-api-qa.md:135` | JWT 样式字符串，未验证有效性；发布前作为候选处理，不试用该 token |
| `booking-api/src/main/resources/application.yml:50` | 注释样例，不是活动默认；仍需在发布内容审计中确认 |

其他文档测试密码、示例配置和 mock 合成字面量也有命中，未将它们全部判定为真实凭据。没有私钥头命中。对现有报告的 16,202 个 JSON/XML/HTML/properties 文件执行 token/私钥高置信模式扫描，命中为 0；该扫描发生在本次 Allure HTML 生成前，不能证明所有截图/视频或后续产物已脱敏。完整历史、无关未跟踪脚本、所有可能凭据格式未做穷尽扫描，GitHub 推送前仍需完整发布审计。本任务仅记录，未修改允许范围以外的历史文件。

## 风险回归与放行矩阵

命令须在满足同行入口条件之后执行；以下是后续操作清单，本次均未在线执行。CI 使用部署节点提供的 Python/Playwright 工具链，从 automation-tests 目录运行。Linux 可采用同行 python 命令；PowerShell 报告脚本是本机入口。

| 优先级 / 范围 | 入口条件 | 后续命令 / 验收证据 | 阻断或回退条件 |
| --- | --- | --- | --- |
| P0 数据库库存事实、补偿清理 | 有效 SSH、数据库备份、受控测试数据；核对部署版本及迁移 | 由 DBA/部署步骤应用 `scripts/db/20260921_add_order_inventory_restored.sql`；按历史预订/取消记录逐笔审计 `inventory_restored` NULL，不能按订单状态批量猜测；验证取消/重复取消/支付/退款/领券后库存和券余量精确恢复 | MySQL 真实事务证据缺失；未知历史行须失败关闭。异常立即禁用交易运行，按备份和逐笔台账修复；禁止盲目回滚补偿事实列 |
| P0 临时用户清理 | test-support 已部署启用，课程密钥有效，空清理预检通过 | `python -m pytest tests/e2e/test_temporary_customer_lifecycle.py --env=public --allow-write -n 0 -v`；留存创建ID、单次删除、删除后查询、批量兜底和 rejected=0 | 任意 teardown error 阻断后续；按 runId 精确排查残留，不删除其他用户 |
| P1 API 读/写 | 读套件需要管理员凭据；写需上一行通过 | `./scripts/run-suite.ps1 -Suite api-read -Environment public`；`./scripts/run-suite.ps1 -Suite api-write -Environment public -AllowWrite`；核对每项执行及清理记录 | 默认写套件排除 commerce；不得把未选交易测试列为通过 |
| P1 客户/管理员 Chromium | 浏览器、管理员凭据、清理就绪；后端与前端版本匹配 | `./scripts/run-suite.ps1 -Suite customer-ui -Environment public -AllowWrite`；`./scripts/run-suite.ps1 -Suite admin-ui -Environment public -AllowWrite` | 定位、业务回读、清理任一失败阻断；已知 frontend TODO 保留 strict-xfail，不掩盖其他异常 |
| P2 Firefox/WebKit 冒烟 | 先 `python -m playwright install firefox webkit`；Linux 额外具备系统依赖 | `./scripts/run-suite.ps1 -Suite smoke -Environment public`；12 个浏览器/视口参数项实际执行 | 缺引擎即未验证，不用收集成功替代；失败记录浏览器版本、截图、动作摘要 |
| P0 串行 E2E + 完整交易 | P0 清理/MySQL/历史数据门禁全通过，课程密钥和管理员凭据齐备 | `./scripts/run-suite.ps1 -Suite e2e -Environment public -AllowWrite -AllowDestructiveCommerce`；随后 `./scripts/run-suite.ps1 -Suite full -Environment public -AllowWrite -AllowDestructiveCommerce` | 默认 `-n 0`；清理失败、库存不平、意外 XPASS 均不放行；停止后续交易、保留现场 |
| P1 报告 | 执行目录独立；Allure CLI可用；合成教学数据 | `./scripts/generate-report.ps1 -ResultsDirectory 'report/<run>/allure-results'`；验证JUnit/summary/HTML和实际失败截图/视频/动作摘要 | 完整 trace.zip 不归档；采集失败与业务失败分别保留；不能以raw结果代替HTML |
| P0 GitHub发布 | 敏感默认和历史审计完成；确认无关脏文件如何纳入；最终代码审查通过 | 按 `docs/superpowers/plans/2026-09-20-github-jenkins-deployment.md` 配置已指定仓库和凭据；先核对待推送提交与文件清单，再推送/合并 | 本次 remote 为空，未执行 push/merge；未通过敏感审计禁止公开推送 |
| P0 Jenkins部署 | SSH及Jenkins受控认证可用、GitHub版本确定、P0清理门禁通过 | 按同一部署计划创建/验证Job：构建→部署→健康检查→API→Chromium/E2E→归档；Linux执行 `python -m pytest tests --env=public --browser chromium -n 0 --allow-write --allow-destructive-commerce --junitxml=report/junit.xml --html=report/pytest.html --self-contained-html --alluredir=report/allure-results`（隔离工作区/唯一 evidence-dir） | 失败禁止后续发布；归档teardown错误；回退应用构建需保留兼容数据库事实。Job地址、build号及日志必须实际取得后才能标记完成 |

人工探索应重点检查管理员与客户权限边界、重复清理及退款后的库存/券余额、移动视口下结算，以及真实失败场景报告是否泄露教学身份信息。性能/负载本阶段排除，理由是它需要独立容量模型、工具和隔离环境；并未删除或宣称完成旧性能目标。

已知 TODO：ADMIN-TODO-001 角色赋权、ADMIN-TODO-002 任意订单状态更新、UI-TODO-001 用户编辑保存仍为严格预期失败设计；只有在线观察后才统计 xfail，修复后的 XPASS 必须促使用例转为正常回归。此前完整 Java reactor 的五个历史 order 失败未在本任务复跑，本次不将其视为已解决。

本阶段下一步是完成部署与凭据准备、历史库存事实审计、敏感发布整改，再取得真实业务与清理证据；当前离线通过不足以批准公网交易及发布。

## 版本化 CI 门禁补充

后续新增的根 `Jenkinsfile` 使用现有套件入口，默认构建、离线执行和业务收集。开启部署或公网执行须满足受控凭据、明确写入/交易开关、test-support 就绪声明和 24 小时内历史库存事实审计证明；公网用例仍执行真实空清理预检。失败/teardown error/XPASS 阻断，公网 skipped/xfail 记为 UNSTABLE，collectionOnly 保留零执行口径。远端 Linux release 入口尚待实现与验收；本地资产测试、参数/WhatIf 模拟均不计为真实 Jenkins 或部署通过。具体依赖与动作协议见 [Jenkins 手册](../../ci/jenkins/README.md)。
