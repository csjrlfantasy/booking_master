# API v1 迁移矩阵

本矩阵记录已冻结的 pytest requests API v1 用例向当前 Playwright 测试框架的迁移状态。有效状态**仅**为：`migrated`、`invalid-endpoint`、`business-changed`、`pending`。阶段完成前，后续任务必须移除每一条 `pending` 状态。

| 旧文件 | 领域 | 状态 | 目标路径 | 原因/证据 |
| --- | --- | --- | --- | --- |
| `test_user.py` | 客户认证与身份 | `migrated` | `tests/api/customer/test_auth_api.py`、`tests/api/customer/test_identity_api.py` | 认证、个人资料、身份及跨用户归属覆盖已替代旧文件。 |
| `test_ticket.py` | 票务目录 | `migrated` | `tests/api/customer/test_catalog_api.py` | 已迁移至当前 TicketController 目录、详情（含场次/票档）、分类、热门和即将开售契约；不支持的旧版独立场次/票档路由已移除。 |
| `test_cart.py` | 购物车 | `migrated` | `tests/api/customer/test_cart_api.py` | 已迁移至当前 CartController 新增、数量更新、删除、批量删除、清空及未授权契约；不支持的旧版路由已移除。 |
| `test_coupon.py` | 优惠券 | `pending` | `tests/api/customer/test_coupon_api.py` | Task 3 将覆盖领券、重复领券、校验和未授权行为。 |
| `test_order.py` | 订单 | `pending` | `tests/api/customer/test_order_api.py` | Task 3 将覆盖创建、查询、取消、权限和状态迁移。 |
| `test_payment.py` | 支付与退款 | `pending` | `tests/api/customer/test_payment_api.py` | Task 3 将覆盖支付、重复支付、退款申请和归属校验。 |
| `test_integration.py` | 跨层购买流程 | `pending` | `tests/e2e/test_purchase_lifecycle.py`、`tests/e2e/test_coupon_purchase.py`、`tests/e2e/test_admin_customer_visibility.py` | Task 7 将以 API 准备和 UI 验证重建跨层业务流程。 |
| `test_performance.py` | 性能/负载 | `business-changed` | 不迁移为 pytest 功能用例 | 性能验证不作为功能 pytest 负载测试迁移；其负载场景需由独立性能工具和环境容量计划处理。 |
