# API v1 迁移矩阵

本矩阵记录已冻结的 pytest requests API v1 用例向当前 Playwright 测试框架的迁移状态。有效状态**仅**为：`migrated`、`invalid-endpoint`、`business-changed`、`pending`。阶段完成前，后续任务必须移除每一条 `pending` 状态。

| 旧文件 | 领域 | 状态 | 目标路径 | 原因/证据 |
| --- | --- | --- | --- | --- |
| `test_user.py` | 客户认证与身份 | `migrated` | `tests/api/customer/test_auth_api.py`、`tests/api/customer/test_identity_api.py` | 认证、个人资料、身份及跨用户归属覆盖已替代旧文件。 |
| `test_ticket.py` | 票务目录 | `migrated` | `tests/api/customer/test_catalog_api.py` | 已迁移至当前 TicketController 目录、详情（含场次/票档）、分类、热门和即将开售契约；不支持的旧版独立场次/票档路由已移除。 |
| `test_cart.py` | 购物车 | `migrated` | `tests/api/customer/test_cart_api.py` | 已迁移至当前 CartController 新增、数量更新、删除、批量删除、清空及未授权契约；不支持的旧版路由已移除。 |
| `test_coupon.py` | 优惠券 | `migrated` | `tests/api/customer/test_coupon_api.py` | 已迁移至当前 CouponController 的领券中心、我的优惠券、领取、重复领取、校验、不可用及未授权契约；补偿式 cleanup 完成前禁止 live/Jenkins，二次开关仅可用于紧急隔离环境。 |
| `test_order.py` | 订单 | `migrated` | `tests/api/customer/test_order_api.py` | 已迁移至当前 OrderController 的动态库存创建、列表、详情、无效票档、取消、重复取消、归属及状态迁移契约；补偿式 cleanup 完成前禁止 live/Jenkins，二次开关仅可用于紧急隔离环境。 |
| `test_payment.py` | 支付与退款 | `migrated` | `tests/api/customer/test_payment_api.py` | 已迁移至当前 PaymentController 的创建、模拟支付、状态、重复支付、退款申请、退款查询及归属契约；补偿式 cleanup 完成前禁止 live/Jenkins，二次开关仅可用于紧急隔离环境。 |
| `test_integration.py` | 跨层购买流程 | `pending` | `tests/e2e/test_purchase_lifecycle.py`、`tests/e2e/test_coupon_purchase.py`、`tests/e2e/test_admin_customer_visibility.py` | Task 7 将以 API 准备和 UI 验证重建跨层业务流程。 |
| `test_performance.py` | 性能/负载 | `business-changed` | 不迁移为 pytest 功能用例 | 性能验证不作为功能 pytest 负载测试迁移；其负载场景需由独立性能工具和环境容量计划处理。 |

## 管理员新增覆盖（不属于旧版文件迁移状态）

以下为 Task 4 新增能力；`implemented-pending-live` 仅代表离线契约通过、用例可收集，公网执行仍待部署与凭据，不能等同于业务验收通过。

| 能力 | 状态 | 用例 | 证据或限制 |
| --- | --- | --- | --- |
| 登录、缺失凭据、客户越权 | `implemented-pending-live` | `tests/api/admin/test_admin_auth_api.py` | 账号密码仅来自环境变量；错误密码使用临时客户，避免锁定共享管理员。 |
| 用户列表、搜索、详情、创建、禁用/恢复 | `implemented-pending-live` | `tests/api/admin/test_admin_user_api.py` | 动态创建并登记清理；回读状态并验证客户登录效果。 |
| 角色目录 | `implemented-pending-live` | `tests/api/admin/test_admin_user_api.py` | 当前后端返回内置角色目录。 |
| 角色分配业务效果 | `blocked-backend-todo` | `test_assigned_super_admin_role_grants_admin_access` | ADMIN-TODO-001：`AdminUserServiceImpl.assignUserRole` 仅记录日志。严格 xfail 回读新登录账户权限；请求成功不算业务通过。 |
| 票务列表、详情、创建、上下架 | `implemented-pending-live` | `tests/api/admin/test_admin_ticket_api.py` | 仅修改 runId 前缀票务，查询验证筛选与状态。 |
| 订单列表、详情、取消、退款 | `implemented-pending-live` | `tests/api/admin/test_admin_order_api.py` | 自建票务、客户和订单，双写入开关仍保留；管理员和客户两侧回读。 |
| 任意订单状态更新 | `blocked-backend-todo` | `test_admin_status_update_persists_on_owned_paid_order` | ADMIN-TODO-002：`AdminOrderController.updateOrderStatus` 是空实现。严格 xfail 验证已支付订单完成状态持久化。 |
| 优惠券创建、更新、状态、发放 | `implemented-pending-live` | `tests/api/admin/test_admin_coupon_api.py` | 仅自建模板/接收者；发放检查客户可见与库存、单人限额，保留交易开关。 |
| 概览、订单和用户统计 | `implemented-pending-live` | `tests/api/admin/test_admin_stats_api.py` | 检查单次响应内统计关系，避免共享环境并发造成跨请求计数假失败。 |

管理员用户、订单列表控制器目前未将 `startDate/endDate` 传给服务；此阶段没有声称日期筛选已覆盖。优惠券详情返回请求 DTO 而非带 ID 实体；订单状态更新使用查询参数，退款 `proofImages` 是 JSON 字符串，框架保留这些真实后端契约。

## 客户 UI 新增覆盖（Task 5）

以下均为 `implemented-pending-live`：代码与离线契约完成，尚未执行真实浏览器或公网写入。

| 旅程 | 用例 | 覆盖与限制 |
| --- | --- | --- |
| 登录、注册与返回目标页面 | `tests/ui/customer/test_customer_login.py` | 临时账号、错误密码、必填禁用、注册并即时登记用户清理、returnTo。仅无写入的必填验证标记跨浏览器。 |
| 目录搜索、分类与详情 | `tests/ui/customer/test_shop_catalog.py` | 从当前 API 选择票务；搜索/空结果、分类、访客与 token 注入后的账号状态；搜索冒烟含桌面与 390px 手机视口。 |
| UI business-changed/direct checkout | `tests/ui/customer/test_customer_checkout.py` | 当前 Web 无购物车 UI，详情页直接下单；购物车仅由 API 用例覆盖。动态库存、API 观演人、UI 选择与提交、即时订单登记、API 持久化回读。优惠券专门旅程保留给 Task 7。 |
| 订单列表、详情、取消、模拟支付、退款 | `tests/ui/customer/test_customer_orders.py` | runId 自有订单，UI 变更后 API 回读，再刷新验证。退款验收为 PROCESSING/退款中，不声称自动退款完成。 |

客户交易 UI 用例仍保留 `write + destructive_commerce` 双重门禁，等待 MySQL 补偿清理验证。以上不替代 Task 7 跨层端到端集成验收，`test_integration.py` 仍为 pending。

## 管理员 UI 新增覆盖（Task 6）

下列实现均待真实浏览器与公网验证；离线契约通过和可收集不代表业务验收完成。

| 旅程 | 状态 | 用例与限制 |
| --- | --- | --- |
| 登录、必填、拒绝客户权限、路由保护 | `implemented-pending-live` | `tests/ui/admin/test_admin_login.py`；真实管理员凭据仅来自环境变量，错误密码使用自建客户；必填/匿名路由为跨浏览器冒烟。 |
| Token 免登录、数据概览 | `implemented-pending-live` | `tests/ui/admin/test_admin_dashboard.py`；API 登录后按 `auth-storage` 的 Zustand v0 结构创建内存 storage_state；用浏览器本次概览响应核对数值，避免二次请求计数竞态。 |
| 创建、搜索、详情、禁用用户 | `implemented-pending-live` | `tests/ui/admin/test_admin_users.py`；UI 创建立即登记清理，API 回读身份/状态并验证禁用后客户无法登录。 |
| 用户编辑保存 | `blocked-frontend-todo` | UI-TODO-001：`UserEditPage` 仅 toast 与跳转，没有更新 API。严格 xfail 仅接受专用持久化回读异常，其他 UI/网络失败不能被掩盖。 |
| 角色分配、通用订单状态更新 | `unavailable-ui/backend-todo` | 当前 UI 无角色分配和任意订单状态控件，未虚构操作。ADMIN-TODO-001/002 继续由 Task 4 API strict xfail 检查真实业务效果。 |
| 票务列表、筛选、详情、下架 | `implemented-pending-live` | `tests/ui/admin/test_admin_tickets.py`；API 自建 runId 票务后 UI 操作；API 回读并重新载入列表按新状态筛选。 |
| 订单列表、筛选、详情、取消、退款 | `implemented-pending-live` | `tests/ui/admin/test_admin_orders.py`；自有客户/票务/订单，双门禁；UI 操作后管理员和客户 API 均回读，退款仅验证订单/支付状态，不宣称账务退款完成。 |
| 优惠券创建、编辑、结束、定向发券 | `implemented-pending-live` | `tests/ui/admin/test_admin_coupons.py`；仅自有模板/用户，即时登记，API 回读券值/状态/客户归属/库存；发券保留 `write + destructive_commerce`。 |

本阶段前端只为概览卡片与待处理计数新增精确 `data-testid`，其余定位使用已有 testid、关联 label、role、placeholder 和精确文本。上下文均在 finally 关闭，认证状态仅存内存。
