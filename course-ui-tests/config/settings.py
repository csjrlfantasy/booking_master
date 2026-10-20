"""集中配置：本地改这里的默认值即可；CI 里同名环境变量优先（Jenkins 注入）。

为什么 CI 要环境变量：同一套测试代码要打到不同环境（本地 / 云服务器），
代码不能改来改去，环境差异交给环境变量——这就是多环境配置的同一思想。
"""

import os

# ── 目标环境 ─────────────────────────────────────────────
WEB_URL = os.getenv("WEB_URL", "http://127.0.0.1:3000")        # 票多星前端
API_URL = os.getenv("API_URL", "http://127.0.0.1:8088/api")    # 票多星后端（必须含 /api）

# ── 管理员账号 ───────────────────────────────────────────
ADMIN_USERNAME = os.getenv("BOOKING_ADMIN_USERNAME", "admin")
ADMIN_PASSWORD = os.getenv("BOOKING_ADMIN_PASSWORD", "Admin@123456")

# ── 教学清理接口密钥 ─────────────────────────────────────
# 空字符串 = 演示清理（只打印不真删）；CI 里由 Jenkins 凭据注入真实密钥。
COURSE_TEST_KEY = os.getenv("COURSE_TEST_KEY", "n2P+aoMgjrH0KiR3fIpa3RlBZYJJgM6L")
