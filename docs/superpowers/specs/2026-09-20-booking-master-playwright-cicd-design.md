# Booking Master Playwright 自动化测试与 CI/CD 设计

## 1. 目标

在现有 BookingApi 单仓库中，把 `automation-tests` 原地升级为适合教学和企业实践的 Python Playwright 自动化测试框架，并将其接入 GitHub 与服务器上的 Jenkins。

最终交付需要同时满足：

- 覆盖客户端、管理端、REST API 和跨端业务流程；
- 支持本地环境与公网教学环境切换；
- 演示 pytest fixture 的 setup/teardown、API 创建数据、Token 注入和自动清理；
- 保留旧 API 测试基线，并对有效用例进行迁移；
- 在 Jenkins 中完成构建、部署、健康检查、自动化测试、报告发布和失败回滚；
- 所有密码、Token 和测试清理密钥均由环境变量或 Jenkins Credentials 提供，不进入公开 Git 仓库。

## 2. 已确认范围

- 代码仓库：`https://github.com/csjrlfantasy/booking_master`，当前为空且为公开仓库。
- 本地代码：`D:\AIProjects\BookingApi`。
- 自动化框架目录：`automation-tests`。
- 客户端入口：`/shop/login?returnTo=%2Fshop`。
- 管理端入口：`/login`。
- 公网服务器作为专用教学环境，允许执行注册、下单、模拟支付、取消、退款和清理等写操作。
- Jenkins 已运行在教学服务器的 8080 端口。
- React Native 客户端不纳入本期 Playwright Web 自动化范围。

## 3. 改造策略

采用原地改造方案，不创建第二套长期并存的自动化框架。

现有 `automation-tests` 主要是 pytest + Requests 接口测试，不具备 Playwright UI 自动化、可靠测试数据隔离和完整清理能力。改造时：

1. 为改造前状态建立 `api-tests-v1-baseline` Git 标签；
2. 将可用旧用例迁移到新版 `tests/api`；
3. 将暂时不兼容的旧用例放入 `legacy/api-tests-v1`，并禁止 pytest 默认收集；
4. 维护迁移清单，标明已迁移、接口不存在、业务变化和待补充用例；
5. Jenkins 仅执行新版有效测试。

Git 历史和迁移清单共同保证旧 API 测试可追溯，不通过复制两套活动框架造成维护分叉。

## 4. 自动化框架架构

```text
automation-tests/
├── framework/
│   ├── config/              # 配置加载、校验和命令行选项
│   ├── api/                 # 客户端、管理员和测试支持 API 客户端
│   ├── auth/                # Token 与浏览器登录态注入
│   ├── data/                # runId 和领域测试数据工厂
│   ├── cleanup/             # 资源登记、清理和残留审计
│   └── reporting/           # 脱敏日志和附件辅助函数
├── pages/
│   ├── customer/            # 商城 Page Objects
│   └── admin/               # 管理端 Page Objects
├── tests/
│   ├── api/                 # REST API 测试
│   ├── ui/                  # 客户端和管理端 UI 测试
│   ├── e2e/                 # API/UI 跨层业务流程
│   └── framework/           # 框架自身单元测试
├── legacy/api-tests-v1/     # 不参与默认收集的旧测试快照
├── config/
│   ├── local.yaml
│   ├── public.yaml
│   └── schema.yaml
├── conftest.py
├── pytest.ini
├── requirements.txt
├── Dockerfile
├── Jenkinsfile
└── README.md
```

公共能力与业务测试分离。Page Object 只封装页面语义和操作，API Client 只封装接口契约，fixture 负责编排生命周期，测试用例负责描述业务行为和断言。

## 5. 多环境配置

框架提供 `local` 和 `public` 两套非敏感配置：

| 环境 | Web 默认地址 | API 默认地址 |
| --- | --- | --- |
| local | `http://127.0.0.1:3000` | `http://127.0.0.1:8088/api` |
| public | `http://154.12.20.250` | `http://154.12.20.250/api` |

通过 `pytest --env=local` 或 `pytest --env=public` 选择环境。URL 可再由环境变量覆盖，优先级为：命令行显式选项、环境变量、环境 YAML 默认值。

YAML 不保存账号密码或密钥。敏感值使用下列环境变量或对应 Jenkins Credentials 注入：

- `BOOKING_ADMIN_USERNAME`
- `BOOKING_ADMIN_PASSWORD`
- `COURSE_TEST_KEY`

公网写测试需要同时满足：显式 `--allow-write`、清理密钥存在、清理接口预检查通过。任一条件不满足时，`write` 测试在收集或 setup 阶段拒绝执行，而不是执行后再跳过清理。

## 6. 测试数据生命周期

每个写测试生成唯一 `runId`，所有测试用户名、票品名、优惠券名和备注均带 `pw-course-{runId}-` 前缀。资源创建成功后立即登记 ID，不等待测试完成才登记。

标准临时用户 fixture 流程：

1. 生成 `runId` 和唯一用户资料；
2. 调用注册 API 并登记 `userId`；
3. 调用登录 API 获取 Token；
4. 在打开页面前，通过 `browser_context.add_init_script` 写入商城使用的 localStorage 键；
5. 测试直接访问受保护页面，验证免 UI 登录的认证状态；
6. `yield` 后进入 teardown，先清理用户关联资源，再删除用户；
7. 查询确认用户已不存在；
8. 会话结束时调用批量清理接口，兜底处理前序 teardown 遗留资源。

setup 中途失败也必须清理已经登记的资源。清理失败作为独立 teardown 错误和 Allure 附件报告，不覆盖原始业务断言失败。

## 7. 测试支持接口

测试支持能力不作为普通用户管理业务接口暴露。它仅在明确启用的教学测试环境注册，并要求 `X-Course-Test-Key`。

### 7.1 单用户清理

```http
DELETE /api/test-support/users/{userId}?runId={runId}
X-Course-Test-Key: <secret>
```

后端在删除前验证：

- `runId` 格式合法；
- 用户存在；
- 用户名以 `pw-course-{runId}-` 开头；
- 请求密钥使用常量时间方式比较。

删除按数据库依赖顺序处理订单明细、退款、支付、用户优惠券、订单、身份信息、认证信息、地址、购物车、角色关系，最后删除用户。重复调用保持幂等。

### 7.2 批量兜底清理

```http
POST /api/test-support/cleanup
X-Course-Test-Key: <secret>
```

请求包含 `runId` 及测试实际登记的用户、订单、票品和优惠券 ID。每个资源均执行前缀归属验证。响应分别返回 `deleted`、`notFound` 和 `rejected` 统计，不允许按 runId 扫描并删除未登记资源。

后端需要通过控制器和服务测试验证：错误密钥、非法 runId、非测试数据拒绝、依赖删除顺序、部分资源不存在、重复清理和事务回滚。

## 8. 测试范围

### 8.1 客户端 API

- 注册、登录和认证异常；
- 用户资料与常用观演人；
- 票品列表、搜索、筛选、详情、场次和票档；
- 购物车增删改查；
- 优惠券领取、查询、校验和重复领取；
- 订单创建、列表、详情、取消；
- 模拟支付、支付状态、退款申请和结果；
- 参数边界、资源不存在、未认证、越权、重复提交和响应契约。

### 8.2 管理端 API

- 管理员登录和错误凭据；
- 用户列表、详情、创建、状态和角色；
- 票品、订单和优惠券的核心管理流程；
- 仪表盘统计；
- 普通用户访问管理接口的权限校验。

### 8.3 客户端 UI

- 登录、注册、错误提示和必填校验；
- API 登录后注入 Token 并免登录访问；
- 商城搜索、分类筛选、空结果和票品详情；
- 购物车、观演人、优惠券、结算、订单和模拟支付；
- 订单取消、退款和状态展示；
- Chromium 为主的完整回归，以及 Firefox/WebKit 核心冒烟。

### 8.4 管理端 UI

- 登录成功、登录失败和退出；
- 仪表盘；
- 用户列表、搜索、创建、详情和状态操作；
- 票品、订单和优惠券的列表、筛选及关键操作；
- 页面操作后的 API 副作用校验。

### 8.5 端到端主流程

代表性流程为：注册临时用户、注入登录态、选择票品、加入购物车、提交订单、模拟支付、管理端查询订单、客户端核对状态，最后清理订单和用户。

测试标记至少包括：`smoke`、`regression`、`api`、`ui`、`e2e`、`write`、`public`、`chromium` 和 `cross_browser`。

## 9. 可观察性与失败处理

- 登录和清理日志不得打印密码、完整 Token 或测试密钥；
- UI 失败自动附加截图、页面 HTML、浏览器控制台、网络摘要和 Playwright Trace；
- 只在教学演示或失败重放场景保存视频，避免每次回归产生过大制品；
- API 失败记录脱敏后的请求方法、路径、状态码、耗时和响应摘要；
- 前后端不可用时由会话级预检查快速失败并给出准确地址；
- flaky 重试只用于已登记的基础设施类瞬时故障，业务断言默认不重试；
- 性能和压力测试与功能回归分离，不使用普通 pytest 并行测试冒充负载测试。

## 10. GitHub 交付

推送前扫描并排除：密码、Token、私钥、Jenkins 密钥、日志、数据库导出、上传文件、`node_modules`、构建产物和本地缓存。

现有工作区包含大量未提交修改。初始发布必须先生成变更清单，只提交经验证应进入仓库的源代码、配置模板、测试和文档，不使用宽泛的 `git add .`。自动化改造通过功能分支交付，验证通过后再合入 `main`。

## 11. Jenkins CI/CD

创建参数化 Pipeline Job：`booking-master-cicd`。

参数包括：

- `TARGET_ENV`: `public` 或 `local`；
- `TEST_SUITE`: `smoke`、`api`、`ui`、`e2e` 或 `regression`；
- `DEPLOY_APP`: 是否部署应用；
- `ALLOW_WRITE_TESTS`: 是否启用写测试；
- `BROWSER`: `chromium`、`firefox` 或 `webkit`。

流水线阶段：

1. Checkout；
2. 敏感信息与配置检查；
3. Java 后端单元测试；
4. React 前端测试和构建；
5. 自动化框架单元测试与 `pytest --collect-only`；
6. 构建后端 JAR 和前端静态资源；
7. 部署新版本；
8. 应用健康检查；
9. API 冒烟；
10. Playwright UI/E2E；
11. 发布 JUnit、Allure、截图、Trace 和必要视频。

`post always` 无条件执行资源清理、用户残留审计和报告归档。清理本身失败时流水线不得显示成功。

Jenkins Credentials 保存 GitHub、SSH、管理员账号、测试清理密钥以及部署所需的应用密钥。Jenkinsfile 只引用凭据 ID。

## 12. 部署和回滚

服务器采用版本化发布目录：

```text
/opt/booking-master/
├── releases/<BUILD_NUMBER>/
├── current
├── shared/config/
├── shared/logs/
└── shared/uploads/
```

构建产物先进入新 release，服务启动并通过健康检查和核心冒烟后再切换 `current`。若启动、API 冒烟或核心 UI 冒烟失败，则切回上一 release 并重启服务。

数据库结构变更使用版本化迁移脚本。流水线不得清空、重建或用测试快照覆盖现有数据库。共享上传和日志目录不随版本回滚。

## 13. 验证与完成标准

- 新框架可以分别对 local 和 public 环境完成配置校验与测试收集；
- 旧 API 用例均有明确迁移状态，新版套件不使用占位 Token 或固定共享测试用户；
- 每个创建用户的 API、UI 和 E2E 测试都能在成功和失败路径执行 teardown；
- 单用户删除和批量清理接口通过安全性、幂等性和事务测试；
- 核心 API、客户端 UI、管理端 UI 和主 E2E 流程在公网教学环境通过；
- 测试结束后不存在本次 runId 所属用户或关联资源；
- Jenkins 能完成构建、部署、测试、报告归档，以及故障时回滚；
- GitHub 公开仓库不包含明文凭据和本地运行产物；
- README 提供教师可直接复制执行的本地、公网和 Jenkins 演示步骤。

## 14. 非目标

- 不把测试支持删除接口开放为普通生产业务接口；
- 不让测试删除无法证明由当前 runId 创建的数据；
- 不在本期覆盖 React Native 原生自动化；
- 不把功能回归套件当作专业压力测试工具；
- 不借本次改造重构与自动化、部署或清理无关的业务代码。
