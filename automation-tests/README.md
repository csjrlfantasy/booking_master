# BookingApi 自动化测试运行手册

`automation-tests` 是 BookingApi 的 Python + Pytest + Playwright 自动化测试框架。它覆盖客户、管理端、接口和关键购票链路；所有写入测试均通过显式开关、课程密钥和受保护清理机制进行约束。

## 开始前

- Python 3.11 或更高版本。
- PowerShell 7（Windows 推荐）或可执行 Python 的终端。
- UI 套件需要安装对应 Playwright 浏览器；默认使用 Chromium。
- 被测项目必须已启动。环境地址以 `config/local.yaml` 和 `config/public.yaml` 为准。
- 管理端与写入测试所需凭据只能通过当前进程环境变量或 Jenkins 凭据注入，不能提交到仓库。

所有命令均在本目录执行：

```powershell
cd D:\AIProjects\BookingApi\automation-tests
```

## 目录说明

```text
automation-tests/
├── config/                         # local/public 被测环境地址、超时和 YAML schema
│   ├── local.yaml                  # 本机前端/API 地址
│   ├── public.yaml                 # 公网前端/API 地址
│   └── schema.yaml                 # 配置字段校验约束
├── framework/                      # 可复用测试底座，不直接放业务用例
│   ├── api/                        # HTTP 客户端与 BookingApi 业务接口封装
│   ├── auth/                       # 客户登录态和浏览器会话注入
│   ├── cleanup/                    # 仅回收本次 runId 登记资源的清理注册表
│   ├── config/                     # Pytest 参数、环境加载和写入门禁
│   ├── data/                       # 隔离测试数据工厂
│   └── reporting/                  # JUnit、HTML、Allure 与脱敏证据产物
├── pages/                          # Playwright Page Object
│   ├── customer/                   # 客户端页面对象
│   └── admin/                      # 管理端页面对象
├── tests/                          # 当前维护的测试入口，pytest 默认只收集此目录
│   ├── framework/                  # 离线框架、门禁和报告单元测试
│   ├── api/                        # 客户与管理端接口测试
│   │   ├── customer/               # 登录、票务、购物车、订单、支付、优惠券、身份信息
│   │   └── admin/                  # 管理员认证、用户、票务、订单、统计、优惠券
│   ├── ui/                         # Chromium/跨浏览器客户与管理端 UI 测试
│   │   ├── customer/
│   │   └── admin/
│   └── e2e/                        # 串行的完整购票、优惠券和临时客户生命周期
├── scripts/                        # 统一套件启动、Allure HTML 和发布敏感信息扫描
│   ├── run-suite.ps1               # 推荐的唯一套件启动入口
│   └── generate-report.ps1         # 将 Allure 原始结果生成 HTML
├── docs/                           # 报告口径、迁移矩阵、发布安全和阶段验证记录
├── report/                         # 每次执行的独立报告目录，默认不提交
├── legacy/api-tests-v1/            # 历史教学归档，不属于当前回归套件，禁止直接执行
├── conftest.py                     # 全局夹具、临时账户、认证与清理生命周期
├── pytest.ini                      # pytest 默认收集范围、标记和报告选项
├── requirements.txt                # Python 依赖
├── .env.example                    # 环境变量名称清单，不会被框架自动读取
├── run_tests.bat                   # Windows 兼容入口，转调 run-suite.ps1
└── TEST_REPORT_SPRINT4_5.md        # 历史测试报告
```

`api-tests/` 为空的历史目录不参与当前执行；不要依据它或旧版批处理参数定位用例。

## 首次安装

建议使用虚拟环境，避免污染系统 Python：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
python -m playwright install chromium
```

需要运行跨浏览器冒烟时，再安装 Firefox 与 WebKit：

```powershell
python -m playwright install firefox webkit
```

验证框架与环境配置可以被收集，不会访问业务接口、创建数据或要求凭据：

```powershell
python -m pytest --collect-only -q --env=local
python -m pytest --collect-only -q --env=public
```

## 环境和凭据

`--env` 仅接受 `local` 或 `public`，并分别读取 `config/local.yaml`、`config/public.yaml`。修改地址应修改对应 YAML，不能把密码或课程密钥写进去。

`.env.example` 只是变量名称清单；框架没有加载 `.env` 文件的依赖或逻辑。执行前请在当前 PowerShell 会话注入真实值：

```powershell
$env:BOOKING_ADMIN_USERNAME = '<管理员账号>'
$env:BOOKING_ADMIN_PASSWORD = '<管理员密码>'
$env:COURSE_TEST_KEY = '<课程测试清理密钥>'
```

| 场景 | 必需变量 | 说明 |
| --- | --- | --- |
| `framework`、只读客户 UI、收集模式 | 无 | 不登录、不写入业务数据 |
| `api-read` | `BOOKING_ADMIN_USERNAME`、`BOOKING_ADMIN_PASSWORD` | 包含管理员只读查询 |
| 任意写入套件 | `COURSE_TEST_KEY` | 同时必须传 `-AllowWrite` / `--allow-write` |
| `api-write`、`admin-ui`、`e2e`、`full` | 上述三项 | 管理端写入或完整链路 |
| `e2e`、`full` 的交易测试 | 上述三项 | 还必须显式传 `-AllowDestructiveCommerce` |

不要把真实值写入 README、YAML、`.env`、批处理文件、截图、报告或命令历史。公共环境写入前，确认后端的受保护测试清理接口已经部署且课程密钥有效。

## 推荐启动方式

推荐使用 `scripts/run-suite.ps1`。它会：创建唯一报告目录、固定串行执行、校验写入/交易开关和所需变量，并保留 pytest 退出码。

```powershell
# 1. 离线框架测试：不访问业务服务
.\scripts\run-suite.ps1 -Suite framework

# 2. 只收集某套件：确认范围，不执行用例
.\scripts\run-suite.ps1 -Suite api-read -Environment public -CollectOnly

# 3. 公网只读接口回归：需要管理员账号
.\scripts\run-suite.ps1 -Suite api-read -Environment public

# 4. 本机可写接口回归：需要 COURSE_TEST_KEY
.\scripts\run-suite.ps1 -Suite api-write -Environment local -AllowWrite

# 5. 客户端或管理端 Chromium UI
.\scripts\run-suite.ps1 -Suite customer-ui -Environment local -AllowWrite
.\scripts\run-suite.ps1 -Suite admin-ui -Environment public -AllowWrite

# 6. 跨浏览器只读冒烟：需要已安装 Chromium、Firefox、WebKit
.\scripts\run-suite.ps1 -Suite smoke -Environment public

# 7. 完整购票/优惠券交易链路：必须串行并显式确认交易影响
.\scripts\run-suite.ps1 -Suite e2e -Environment public -AllowWrite -AllowDestructiveCommerce
.\scripts\run-suite.ps1 -Suite full -Environment public -AllowWrite -AllowDestructiveCommerce
```

Windows 下也可以使用兼容入口；它等价于调用 PowerShell 套件脚本：

```bat
run_tests.bat -Suite framework
run_tests.bat -Suite api-read -Environment public -CollectOnly
```

执行前想核对真实 pytest 参数而不运行测试，使用 `-DryRun`：

```powershell
.\scripts\run-suite.ps1 -Suite full -Environment public -AllowWrite -AllowDestructiveCommerce -DryRun
```

## 直接运行 Pytest

需要快速定位单文件或单用例时可以直接使用 Pytest；直接运行仍受同一写入门禁保护。

```powershell
# 离线框架测试
python -m pytest tests/framework -q

# 只运行某个客户接口用例文件
python -m pytest tests/api/customer/test_cart_api.py --env=local -q

# 收集完整套件，不执行
python -m pytest tests --collect-only --env=public -q

# 公网临时客户生命周期：会创建并删除测试客户
python -m pytest tests/e2e/test_temporary_customer_lifecycle.py --env=public --allow-write -n 0 -v
```

包含 `write` 标记的用例需要 `--allow-write` 与非空 `COURSE_TEST_KEY`；包含 `destructive_commerce` 的用例还需要 `--allow-destructive-commerce`。交易测试可能消耗库存或优惠券，必须确认清理补偿已经验收后再执行。

## 套件范围

| 套件 | 覆盖范围 | 额外要求 |
| --- | --- | --- |
| `framework` | 框架、配置、报告、清理、发布扫描的离线测试 | 无 |
| `api-read` | 只读客户/管理端 API | 管理员账号 |
| `api-write` | 非交易写入 API | 课程密钥、`-AllowWrite`、管理员账号 |
| `customer-ui` | 客户登录、目录、订单与结算界面 | Chromium、课程密钥、`-AllowWrite` |
| `admin-ui` | 管理端登录、概览、用户、票务、订单、优惠券 | Chromium、课程密钥、`-AllowWrite`、管理员账号 |
| `smoke` | 客户只读跨浏览器冒烟 | Chromium、Firefox、WebKit |
| `e2e` | 串行客户生命周期、购票与优惠券交易 | 三项变量、两个写入开关 |
| `full` | 当前 `tests/` 的完整套件 | 三项变量、两个写入开关 |

## 报告与排错

每次通过 `run-suite.ps1` 执行，都会生成独立目录：

```text
report/<UTC时间>-<local或public>-<runId>/
├── summary.json             # 执行状态与统计
├── environment.properties   # 环境和 runId
├── junit.xml                # Jenkins 可读取的 JUnit
├── pytest.html              # 自包含 HTML 报告
├── allure-results/          # Allure 原始结果
└── browser/                 # 失败 UI 用例的截图、视频与脱敏动作时间线
```

生成 Allure HTML：

```powershell
.\scripts\generate-report.ps1 -ResultsDirectory 'report\<本次运行目录>\allure-results'
```

该命令需要预先安装 Allure CLI；输出到同级的新 `allure-html-<id>` 目录。完整报告、脱敏边界与 Jenkins 归档建议见 [报告与证据口径](docs/reporting-and-evidence.md)。

## 重要门禁

- 当前 `pytest.ini` 默认只收集 `tests/`，`legacy/api-tests-v1/` 是归档内容，禁止作为当前回归执行。
- 每个测试都使用独立 `runId`；清理器仅处理本次登记的用户、订单、票务和优惠券资源。
- `-CollectOnly` 只证明用例可收集，不证明接口、浏览器、凭据、清理或线上业务通过。
- `-DryRun` 只展示将执行的 pytest 参数，不创建报告、不运行测试。
- `e2e` 与 `full` 固定 `-n 0` 串行执行，避免共享库存和优惠券并发干扰。
- 当前真实线上验收状态、已知阻断项和基线证据见 [阶段验证记录](docs/phase-verification.md)。

## Jenkins 入口

仓库根目录的 `Jenkinsfile` 默认运行离线框架测试和业务用例收集。Job 凭据、显式公网开关与部署前置条件见 [Jenkins 运行手册](../ci/jenkins/README.md)。版本化流水线资产不表示 Jenkins Job 已创建或公网交易测试已经通过。
