# framework：把知识点收成一个测试框架

根目录的单例代码"一个文件讲一个知识点"，适合逐点教学；
这个 `framework/` 目录演示的是下一步：**同样的知识，在真实项目里应该怎么组织**。

## 单例 → 框架组件对照

| 单例里重复出现的东西 | 框架里的归宿 |
| --- | --- |
| `_data()` 两层响应断言（HTTP 200 ≠ 业务成功） | `api/booking_api.py` 的 `unwrap()` |
| 登录拿 token、拼 Bearer 头 | `BookingApi.admin_login() / user_login()` + `_auth()` |
| 创建票务那一坨固定字段 | `BookingApi.create_ticket()`（合理默认值 + 参数覆盖） |
| 下单的 attendees / 身份证校验位 / userRemark 归属规则 | `BookingApi.create_order()` + `make_idcard()` |
| 清理的 runId 登记 + 双模式清理（`config/settings.py` 填 `COURSE_TEST_KEY` 真实删除，留空打印演示，失败降级不阻塞） | `BookingApi.cleanup()` |
| 管理员 UI 登录那串固定流程 | `conftest.py` 的 `admin_page` fixture |
| Mock 数据那一坨字段字面量 | `mocks/hot_tickets.py` 的 `make_hot_ticket()` 数据工厂 |
| 每次运行唯一的手机号 | `api/booking_api.py` 的 `make_phone()` |
| 每个写入用例开头的配置读取 + skip 判断 | `conftest.py` 的 `write_guard` fixture |
| 每条用例开头的 UI 登录流程 | `conftest.py` 的 `admin_storage_state`（会话级登录一次）+ `admin_page`（每用例加载登录态） |
| 后台登录/订单列表/订单详情/票务详情的页面操作 | `pages/` 的页面对象（POM） |
| `playwright.request.new_context()` + 手动 dispose | `conftest.py` 的 `api_client` fixture |
| Mock 的路由正则、handler 骨架、seen_get 哨兵 | `mocks/hot_tickets.py` 的 `HotTicketsMock` 等安装器 |

## 分层

```
framework/
├── conftest.py      环境配置 + api_client + write_guard + 登录态复用（admin_storage_state / admin_page）
├── api/             接口知识：端点、字段、校验规则、清理
│   └── booking_api.py   数据准备全家桶（全部实测验证）：票/订单/支付沙箱/评论/
│                        Banner/优惠券(创建·发放·领取)/余额/观演人/购物车/秒杀
│                        + 清理（cleanup 批量 / delete_* 单个，真实删除）
├── pages/           页面知识：定位器与页面流程（POM）
│   ├── admin_pages.py   后台登录页 / 订单列表页 / 订单详情页
│   └── shop_pages.py    前台票务详情页
├── mocks/           Mock 知识：路由、响应包装、哨兵、数据工厂
│   └── hot_tickets.py
└── tests/           用例层：只剩业务主线
    ├── test_hot_tickets_mock.py          只读，5 个 Mock 场景
    ├── test_api_prepare_ui_verify.py     写入：API 造票 → UI 验证 → 清理
    ├── test_ui_action_api_verify.py      写入：UI 取消订单 → API 核实
    ├── test_payment_flow.py              写入：mock-pay 造"已支付" → UI 验证 → API 复核
    ├── test_comment_api_to_ui.py         写入：API 发评论 → 详情页可见
    ├── test_coupon_order_flow.py         写入：造券 → 领券 → 用券下单 → 金额算术断言
    └── test_banner_real_create_delete.py 写入：Banner 真实创建 → 首页可见 → 公开 DELETE 接口清理
```

判断一件东西该放哪一层的简单标准：

- **接口知识**（URL、字段名、响应结构）→ `api/`
- **页面知识**（定位器、页面流程、弹窗处理）→ `pages/`
- **Mock 知识**（拦哪个路由、响应怎么包装）→ `mocks/`
- **环境知识**（地址、凭据、开关）→ `conftest.py` + `config/settings.py`
- **业务主线**（先干什么、再验证什么）→ `tests/`

## 运行

只读 Mock 演示（需要本地 Web 前端在 3000 端口）：

```powershell
Set-Location course-code/lesson11
python -m pytest framework/tests/test_hot_tickets_mock.py -q
```

写入演示（本地教学环境直接运行；`config/settings.py` 填入 `COURSE_TEST_KEY` 时数据真实创建、真实删除，详见 lesson11 根 README）：

```powershell
python -m pytest framework/tests -q -k "not hot_tickets_mock"
```

## 与 final-framework 的关系

这里是"第 11 课知识点的最小框架化"，只收 API/Mock/防护三件事；
毕业实战的 `final-framework` 在此之上还有 POM、配置管理、报告与 CI，
两者是"看懂原理"和"完整形态"的关系。
