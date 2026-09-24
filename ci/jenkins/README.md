# Booking Master Jenkins 配置与验收

根 `Jenkinsfile` 是版本化流水线入口。默认运行源码扫描、Java 聚焦测试及打包、前端构建、Python 离线框架验证和业务套件收集；不会访问教学业务服务。安装依赖需要包仓库网络。此版本是本地验证过的 CI 资产，尚未创建 Jenkins Job、验证服务器或执行部署，不能作为已完成的上线证据。

## Job 与 Agent

创建 Pipeline Job `booking-master-cicd`，选择 Pipeline script from SCM / Git，仓库 `https://github.com/csjrlfantasy/booking_master.git`，SCM 凭据 `github-booking-master`，分支 `*/main`，Script Path `Jenkinsfile`。首次发布到 main 前可由维护人填入已审查的实现分支。关闭不受信任分支/PR 自动写环境触发；只有维护人可以执行公网参数构建。

Agent 标签 `booking-linux`，使用独立可清理工作区。Checkout 会删除该 Job 的旧工作区；不能让它指向开发者源码目录或共享数据目录。工具：Git、Java 17、Maven、Node/npm、Python 3.11+、PowerShell 7 (`pwsh`)、OpenSSH、tar、Allure CLI。系统管理员先安装 Playwright 三引擎所需 Linux 系统库；流水线仅下载 Chromium/Firefox/WebKit，不通过 sudo 安装系统组件。

插件：Pipeline（含 Declarative、Basic Steps）、Git、Credentials Binding、JUnit、Allure Jenkins Plugin、Lockable Resources、Pipeline Utility Steps（`findFiles`/`readJSON`）。在 Jenkins Tools 配置 Allure CLI。配置锁资源 `booking-public-environment`；整个流水线持锁，覆盖部署和后续验收，其他修改此环境的 Job 也必须使用同一锁。

## 凭据

| ID | 类型 / 用途 |
| --- | --- |
| `github-booking-master` | GitHub checkout 凭据；在 SCM 配置中绑定 |
| `booking-deploy-ssh` | SSH Username with private key；专用非 root 部署账户、无交互密钥 |
| `booking-known-hosts` | Secret file；经可信渠道核实的服务器 SSH host key，禁止运行时自动信任 |
| `booking-admin-username-password` | Username with password；测试管理员 |
| `booking-course-test-key` | Secret text；受保护 cleanup 密钥 |
| `booking-migration-evidence` | Secret file；本次 DBA 审计证明 JSON（见下文） |

所有秘密由 Credentials Binding 注入进程环境，不写入参数、源码、报告或 shell 命令行。部署账户须能运行批准的 release 入口及写入它创建的 staging 目录，禁止通用 root/sudo 权限。数据库、JWT、支付签名、课程密钥等服务配置由运维在服务器受限文件内预先配置；流水线不会把测试凭据写入服务器配置。默认离线构建不会读取以上部署/测试凭据。

## 参数和首次演示

所有布尔参数默认 `false`：`DEPLOY_APP`、`RUN_PUBLIC_TESTS`、`ALLOW_WRITE_TESTS`、`ALLOW_DESTRUCTIVE_COMMERCE`、`MIGRATION_ACK`、`TEST_SUPPORT_READY`。首次 Build with Parameters 保持全部关闭。这次绿色构建的含义仅为构建、离线框架执行和业务收集通过。

本地等价验证命令（仓库根，PowerShell）：

```powershell
./ci/jenkins/verify-prerequisites.ps1 -Mode offline
python -m pytest automation-tests/tests/framework -c automation-tests/pytest.ini -q
./automation-tests/scripts/run-suite.ps1 -Suite full -Environment public -CollectOnly
```

公网完整回归包含真实写入和交易，必须显式开启 `RUN_PUBLIC_TESTS` 以及四个资格开关 `ALLOW_WRITE_TESTS`、`ALLOW_DESTRUCTIVE_COMMERCE`、`MIGRATION_ACK`、`TEST_SUPPORT_READY`；并具备有效管理员、cleanup 和数据库证明凭据。每个写用例仍执行实际空 cleanup 预检，然后才创建数据；运行 `full`（API/UI/E2E、Chromium）及 `smoke`（三引擎），统一 `-n 0`。缺凭据/清理失败/未知库存事实都会终止，不能由假密钥或只收集成功代替。

部署需另开启 `DEPLOY_APP`，同样强制上述四个资格开关，另需 SSH 与 known-hosts 凭据。可单独部署而不执行公网套件，但该构建不能宣称业务验收通过。部署和公网回归均完成才能演示完整交付链路。

## 数据库与部署门禁

运维须先备份，审查并应用 `scripts/db/20260921_add_order_inventory_restored.sql`，逐笔核实历史订单的库存回补事实；未知 NULL 不能根据可变订单状态猜测回填。流水线不执行 SQL、建表、回填或数据库回滚。

`booking-migration-evidence` 为运维提供的断言，不是本地脚本自行查询数据库的证明。内容字段：`environment` 必须为 `public`；`inventoryRestoredColumn` 为 JSON 布尔 `true`；`unknownInventoryRestoredRows` 为 JSON 整数 `0`；`backupReference`、`auditedBy` 为非空审计引用；`auditedAtUtc` 为 UTC ISO 时间，距运行不超过 24 小时、不超前 5 分钟。不要在该文件写 DB 密码或客户数据。每次启用公网交易前更新真实审计结果；服务器 release verify 还须即时确认数据库状态。

本仓库现有 `build_and_run*.ps1` 与 `start-all.ps1` 是 Windows 开发启动逻辑。尚没有经过服务器验证的 Linux 部署实现。因此 `deploy-public.ps1` 调用一个必须由运维另行实现/审查的固定入口 `/opt/booking-master/bin/release`；当前未提供或声称该远端入口已安装。未满足该依赖时 SSH/verify 返回非零，禁止继续上传或切换。

入口约定：每次 SSH 传入 `ACTION RELEASE_ID JAR_SHA256 FRONTEND_SHA256`，ID 仅含构建号和 Git hash；服务端必须再次校验参数。动作依次为：

1. `verify`：只读确认运行时、空间、服务配置、test-support、备份与实际数据库迁移状态；未知历史行非零必须拒绝。
2. `prepare`：创建新的 `/opt/booking-master/staging/RELEASE_ID`，拒绝已有目录、路径穿越及符号链接覆盖。
3. Jenkins scp 上传 `booking-api.jar`、`frontend.tar.gz`；`install` 校验两项 SHA256，安全展开前端，建立不可变 release，保留当前版本。
4. `activate`：记录前一版本并原子切换实际应用，使用服务器已审查的服务管理方式启动；不得猜测服务名。
5. `health`：有界等待后端健康和客户/管理员前端版本一致，失败退出非零。
6. `rollback`：只恢复本次 activation 的上一应用版本，重新健康检查；不删除事实列、不逆向修改业务数据。

入口必须能在 `activate` 部分失败后幂等回滚，校验 release ID 与本次部署归属。测试凭据不通过 SSH 命令传输。远端输出被抑制，只记录动作名和成功/失败，详细服务日志由授权运维在服务器查看。

支持本地验证和预演：

```powershell
./ci/jenkins/verify-prerequisites.ps1 -Mode deploy -WhatIf
./ci/jenkins/deploy-public.ps1 -Jar booking-api/target/booking-api-1.0.0-SNAPSHOT.jar -FrontendArchive frontend-release.tar.gz -ReleaseId '<build>-<commit>' -WhatIf
```

上面 release ID 是说明性占位，实际须使用数字构建号和 7–40 位十六进制 hash。没有 `-Execute` 时只验证文件、门禁及校验和；`-Execute -WhatIf` 也不调用 SSH/scp。`-WhatIf` 不绕过缺凭据、审计或参数校验。

## 结果、归档与回退

每次套件须产生唯一 summary，collectionOnly/executed/exitCode 必须与请求匹配。失败、setup/teardown error、XPASS 或非零退出码均为失败；公网 skipped/xfail 标记 UNSTABLE 并展示计数；离线框架不允许 skip/xfail。无自动重试。Java 是明确列出的聚焦测试，不宣称完整 reactor 全绿。

JUnit、pytest HTML、summary、Allure 脱敏 JSON/txt 和裁剪动作 JSON 按白名单发布。Allure 插件实际生成 HTML；无结果时不声称已生成。构建保留 14 天/20 次，归档保留 7 天/10 次。完整 trace ZIP、storage、`.env`、依赖目录及工作区不归档；截图和视频的像素不能保证脱敏，因此本流水线不发布它们。Allure API JSON 附件由框架脱敏，异常文本/页面仍只允许合成教学数据，报告须限制访问。

部署 activate 或 health 失败时适配器调用 rollback；rollback 失败让 Job 保持失败并要求运维恢复。公网业务回归失败将阻止放行，但不自动回滚已经发生的业务数据：禁用后续交易、按 runId 排查清理和库存；由运维在同一环境锁下调用已审查 release rollback 恢复上一应用版本并复验健康。不得把恢复应用版本当作恢复数据库的证据。

## 尚待真实验收

GitHub 安全发布/干净检出、有效专用 SSH、Jenkins Agent 和插件、远端 release 实现、运行配置、数据库真实审计、清理权限均待确认；还需 Jenkins Declarative linter、首次离线构建编号、健康失败回滚演示、真实 API/UI/E2E 运行及报告访问证据。本地合同测试仅校验资产结构、PowerShell 分支与模拟退出码，不能替代这些验收。
