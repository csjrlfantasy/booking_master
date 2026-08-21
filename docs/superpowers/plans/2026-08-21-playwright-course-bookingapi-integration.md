# Playwright Course BookingApi Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the Playwright Python course into a coherent, repeatable BookingApi teaching project with real UI/API flows and automatic cleanup of test-owned data.

**Architecture:** BookingApi exposes stable UI test IDs and a dev-only, key-protected cleanup endpoint. The course owns environment discovery, health checks, run-scoped data generation, API setup, Page Objects, and cleanup fixtures; lesson material reuses the same public UI/API contract while preserving official external sites only for unsupported browser features.

**Tech Stack:** Java 17, Spring Boot 3.2, JdbcTemplate, JUnit 5, Mockito, React, TypeScript, Node source-contract tests, Python 3, pytest, pytest-playwright, requests.

---

## Repository roots

- BookingApi: `D:\AIProjects\BookingApi`
- Playwright course: `D:\Education\playwright课程`
- 36-unit companion: `D:\Education\playwright课程\图文课程\Playwright_Python_36张图配套代码\playwright_python_course_code_01_36`

Do not stage, revert, or rewrite unrelated dirty files in BookingApi. Each BookingApi commit must name only files changed by its task. The course directory is not a Git repository, so verify it with tests and file checks instead of attempting to commit it.

## File structure

### BookingApi files

- Create `booking-api/src/main/java/com/ticket/app/controller/testsupport/CourseTestSupportController.java`: validates the course key and accepts cleanup requests.
- Create `booking-api/src/main/java/com/ticket/app/controller/testsupport/dto/CourseCleanupRequest.java`: validated `runId` request.
- Create `booking-api/src/main/java/com/ticket/app/controller/testsupport/dto/CourseCleanupResponse.java`: per-table cleanup counts.
- Create `booking-api/src/main/java/com/ticket/app/service/testsupport/CourseTestCleanupService.java`: prefix-protected, transaction-scoped SQL cleanup.
- Create `booking-api/src/test/java/com/ticket/app/controller/testsupport/CourseTestSupportControllerTest.java`: controller key and request tests.
- Create `booking-api/src/test/java/com/ticket/app/service/testsupport/CourseTestCleanupServiceTest.java`: run ID validation, SQL prefix, order, and idempotency tests.
- Modify `booking-api/src/main/java/com/ticket/app/config/WebMvcConfig.java`: bypass JWT only for the dev-only test-support controller.
- Modify `booking-api/src/main/resources/application-dev.yml`: local test-support enable flag and overridable key.
- Create `frontend/tests/coursePlaywrightContractSource.test.mjs`: source-level test-ID contract.
- Modify `frontend/src/pages/auth/LoginPage.tsx`, `frontend/src/pages/shop/ShopLoginPage.tsx`, `frontend/src/pages/shop/ShopPage.tsx`, `frontend/src/pages/user/UserListPage.tsx`, `frontend/src/pages/user/UserCreatePage.tsx`, `frontend/src/pages/ticket/TicketListPage.tsx`, `frontend/src/pages/order/OrderListPage.tsx`, and `frontend/src/pages/coupon/AdminCouponListPage.tsx`: add stable semantic test IDs without changing behavior.

### Course shared files

- Create `utils/course_config.py`: immutable environment configuration.
- Create `utils/course_data.py`: run ID and ticket-domain test data factory.
- Create `utils/booking_api.py`: Result-envelope API client and cleanup call.
- Create `tests/unit/test_course_config.py`, `tests/unit/test_course_data.py`, `tests/unit/test_booking_api.py`, and `tests/unit/test_course_content_contract.py`: unit and content contracts.
- Modify `requirements.txt`, `pytest.ini`, `conftest.py`, `config/base.json`, and `config/test.json`: shared dependencies, markers, fixtures, and BookingApi defaults.
- Replace `pages/login_page.py`, `pages/search_page.py`, and `pages/user_management_page.py`: real BookingApi Page Objects.
- Replace `tests/test_login.py`, `tests/test_search.py`, and `tests/test_user_management.py`: concise, deterministic real-project tests.
- Replace `data/json/login_data.json`, `data/json/search_data.json`, and `data/json/users.json`: ticket-domain datasets.
- Modify `README.md`: teacher-run service prerequisite and exact test commands.

### Lesson and companion files

- Create `lessons/course_targets.py`: shared BookingApi targets for 32-lesson examples.
- Modify the exact 32-lesson files listed in Tasks 8 and 9 where fake business URLs currently occur.
- Create `course_support.py` and `conftest.py` at the 36-unit companion root.
- Modify the exact 36-unit files listed in Tasks 10 and 11.
- Modify companion `README.md`, `.env.example`, `requirements.txt`, and `MANIFEST.json`; regenerate `Playwright_Python_36张图配套代码.zip`.

---

### Task 1: Specify the dev-only cleanup controller

**Files:**
- Create: `booking-api/src/test/java/com/ticket/app/controller/testsupport/CourseTestSupportControllerTest.java`
- Test: `booking-api/src/test/java/com/ticket/app/controller/testsupport/CourseTestSupportControllerTest.java`

- [ ] **Step 1: Write the failing controller test**

```java
package com.ticket.app.controller.testsupport;

import com.ticket.app.common.Result;
import com.ticket.app.controller.testsupport.dto.CourseCleanupRequest;
import com.ticket.app.controller.testsupport.dto.CourseCleanupResponse;
import com.ticket.app.service.testsupport.CourseTestCleanupService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseTestSupportControllerTest {
    private final CourseTestCleanupService service = mock(CourseTestCleanupService.class);
    private final CourseTestSupportController controller =
            new CourseTestSupportController(service, "playwright-course-local");

    @Test
    void cleanupRejectsWrongCourseKey() {
        CourseCleanupRequest request = new CourseCleanupRequest("a1b2c3d4");
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.cleanup("wrong-key", request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void cleanupReturnsServiceCountsForMatchingKey() {
        CourseCleanupRequest request = new CourseCleanupRequest("a1b2c3d4");
        CourseCleanupResponse response = new CourseCleanupResponse(
                "a1b2c3d4", Map.of("user", 1, "order", 2));
        when(service.cleanup("a1b2c3d4")).thenReturn(response);

        Result<CourseCleanupResponse> result =
                controller.cleanup("playwright-course-local", request);

        assertEquals(response, result.getData());
        verify(service).cleanup("a1b2c3d4");
    }
}
```

- [ ] **Step 2: Run the test and verify RED**

Run from `D:\AIProjects\BookingApi`:

```powershell
mvn.cmd -pl booking-api -Dtest=CourseTestSupportControllerTest test
```

Expected: compilation fails because `CourseTestSupportController`, its DTOs, and `CourseTestCleanupService` do not exist.

- [ ] **Step 3: Create request/response DTOs and the controller**

`CourseCleanupRequest.java`:

```java
package com.ticket.app.controller.testsupport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CourseCleanupRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9]{8,32}")
        String runId
) {}
```

`CourseCleanupResponse.java`:

```java
package com.ticket.app.controller.testsupport.dto;

import java.util.Map;

public record CourseCleanupResponse(String runId, Map<String, Integer> deleted) {}
```

`CourseTestSupportController.java`:

```java
package com.ticket.app.controller.testsupport;

import com.ticket.app.common.Result;
import com.ticket.app.controller.testsupport.dto.CourseCleanupRequest;
import com.ticket.app.controller.testsupport.dto.CourseCleanupResponse;
import com.ticket.app.service.testsupport.CourseTestCleanupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Profile("dev")
@RestController
@RequestMapping("/test-support")
@ConditionalOnProperty(prefix = "course.test-support", name = "enabled", havingValue = "true")
public class CourseTestSupportController {
    private final CourseTestCleanupService cleanupService;
    private final String expectedKey;

    public CourseTestSupportController(
            CourseTestCleanupService cleanupService,
            @Value("${course.test-support.key}") String expectedKey) {
        this.cleanupService = cleanupService;
        this.expectedKey = expectedKey;
    }

    @PostMapping("/cleanup")
    public Result<CourseCleanupResponse> cleanup(
            @RequestHeader("X-Course-Test-Key") String suppliedKey,
            @Valid @RequestBody CourseCleanupRequest request) {
        if (!MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid course test key");
        }
        return Result.success(cleanupService.cleanup(request.runId()));
    }
}
```

Create this compilable service shell; Task 2 replaces the exception with SQL behavior after its service test fails:

```java
package com.ticket.app.service.testsupport;

import com.ticket.app.controller.testsupport.dto.CourseCleanupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Profile("dev")
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "course.test-support", name = "enabled", havingValue = "true")
public class CourseTestCleanupService {
    private final JdbcTemplate jdbcTemplate;

    public CourseCleanupResponse cleanup(String runId) {
        throw new UnsupportedOperationException("cleanup is implemented in Task 2");
    }
}
```

- [ ] **Step 4: Run the controller test and verify GREEN**

Run the same Maven command. Expected: 2 tests pass.

- [ ] **Step 5: Commit only Task 1 files**

```powershell
git add booking-api/src/main/java/com/ticket/app/controller/testsupport booking-api/src/main/java/com/ticket/app/service/testsupport/CourseTestCleanupService.java booking-api/src/test/java/com/ticket/app/controller/testsupport/CourseTestSupportControllerTest.java
git commit -m "test: define course cleanup controller contract"
```

---

### Task 2: Implement prefix-protected cleanup

**Files:**
- Create: `booking-api/src/test/java/com/ticket/app/service/testsupport/CourseTestCleanupServiceTest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/testsupport/CourseTestCleanupService.java`
- Modify: `booking-api/src/main/java/com/ticket/app/config/WebMvcConfig.java`
- Modify: `booking-api/src/main/resources/application-dev.yml`
- Test: `booking-api/src/test/java/com/ticket/app/service/testsupport/CourseTestCleanupServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Use a mocked `JdbcTemplate`. Assert that `cleanup("../unsafe")` throws `IllegalArgumentException`; assert that `cleanup("a1b2c3d4")` passes `pw-course-a1b2c3d4%` to every update; assert the first user-owned deletion targets `refund` and the `user` deletion occurs after its dependent rows; call cleanup twice with all updates returning zero and assert both responses contain zero counts.

```java
@Test
void rejectsUnsafeRunIdBeforeSql() {
    assertThrows(IllegalArgumentException.class, () -> service.cleanup("../unsafe"));
    verifyNoInteractions(jdbcTemplate);
}

@Test
void usesRunScopedPrefixAndIsIdempotent() {
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
    CourseCleanupResponse first = service.cleanup("a1b2c3d4");
    CourseCleanupResponse second = service.cleanup("a1b2c3d4");
    assertTrue(first.deleted().values().stream().allMatch(count -> count == 0));
    assertEquals(first.deleted(), second.deleted());
    verify(jdbcTemplate, atLeastOnce()).update(anyString(), aryEq(new Object[]{"pw-course-a1b2c3d4%"}));
}
```

- [ ] **Step 2: Run service tests and verify RED**

```powershell
mvn.cmd -pl booking-api -Dtest=CourseTestCleanupServiceTest test
```

Expected: tests fail because the shell throws `UnsupportedOperationException`.

- [ ] **Step 3: Implement the transactional cleanup service**

Use a fixed `LinkedHashMap<String, String>` of parameterized MySQL deletes in this dependency order:

```java
private static final Map<String, String> DELETE_SQL = Map.ofEntries(
    Map.entry("refund", "DELETE r FROM refund r JOIN user u ON r.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("payment", "DELETE p FROM payment p JOIN user u ON p.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("order_item", "DELETE oi FROM order_item oi JOIN `order` o ON oi.order_no=o.order_no JOIN user u ON o.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("user_coupon", "DELETE uc FROM user_coupon uc JOIN user u ON uc.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("user_identity", "DELETE ui FROM user_identity ui JOIN user u ON ui.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("cart", "DELETE c FROM cart c JOIN user u ON c.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("user_role_relation", "DELETE urr FROM user_role_relation urr JOIN user u ON urr.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("order", "DELETE o FROM `order` o JOIN user u ON o.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("user_auth", "DELETE ua FROM user_auth ua JOIN user u ON ua.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("user_address", "DELETE a FROM user_address a JOIN user u ON a.user_id=u.id WHERE u.username LIKE ?"),
    Map.entry("ticket_comment", "DELETE tc FROM ticket_comment tc JOIN ticket t ON tc.ticket_id=t.id WHERE t.name LIKE ?"),
    Map.entry("ticket_reminder", "DELETE tr FROM ticket_reminder tr JOIN ticket t ON tr.ticket_id=t.id WHERE t.name LIKE ?"),
    Map.entry("ticket_flash_sale", "DELETE tf FROM ticket_flash_sale tf JOIN ticket t ON tf.ticket_id=t.id WHERE t.name LIKE ?"),
    Map.entry("inventory_log", "DELETE il FROM inventory_log il JOIN ticket t ON il.ticket_id=t.id WHERE t.name LIKE ?"),
    Map.entry("ticket_price_level", "DELETE pl FROM ticket_price_level pl JOIN ticket t ON pl.ticket_id=t.id WHERE t.name LIKE ?"),
    Map.entry("ticket_session", "DELETE ts FROM ticket_session ts JOIN ticket t ON ts.ticket_id=t.id WHERE t.name LIKE ?"),
    Map.entry("user_coupon_by_coupon", "DELETE uc FROM user_coupon uc JOIN coupon c ON uc.coupon_id=c.id WHERE c.name LIKE ?"),
    Map.entry("user", "DELETE FROM user WHERE username LIKE ?"),
    Map.entry("ticket", "DELETE FROM ticket WHERE name LIKE ?"),
    Map.entry("coupon", "DELETE FROM coupon WHERE name LIKE ?")
);
```

Because `Map.ofEntries` does not guarantee iteration order, construct a `LinkedHashMap` in a static factory in exactly the displayed order. Validate `runId` with `[a-z0-9]{8,32}`, build `pw-course-{runId}%`, execute every statement with that single parameter, and return an unmodifiable copy of the counts. Do not accept table names or SQL from the request.

- [ ] **Step 4: Enable the endpoint only in local dev**

Append to `application-dev.yml`:

```yaml
course:
  test-support:
    enabled: true
    key: ${COURSE_TEST_KEY:playwright-course-local}
```

Add `/api/test-support/**` to `WebMvcConfig.jwtExcludePathPatterns()`. The controller's `@Profile`, conditional property, and course key remain the authorization boundary.

- [ ] **Step 5: Run focused and controller regression tests**

```powershell
mvn.cmd -pl booking-api -Dtest=CourseTestCleanupServiceTest,CourseTestSupportControllerTest,AdminAuthControllerTest test
```

Expected: all focused tests pass.

- [ ] **Step 6: Commit only cleanup implementation files**

```powershell
git add booking-api/src/main/java/com/ticket/app/service/testsupport/CourseTestCleanupService.java booking-api/src/main/java/com/ticket/app/config/WebMvcConfig.java booking-api/src/main/resources/application-dev.yml booking-api/src/test/java/com/ticket/app/service/testsupport/CourseTestCleanupServiceTest.java
git commit -m "feat: add run-scoped course data cleanup"
```

---

### Task 3: Define and add the frontend Playwright contract

**Files:**
- Create: `frontend/tests/coursePlaywrightContractSource.test.mjs`
- Modify: `frontend/src/pages/auth/LoginPage.tsx`
- Modify: `frontend/src/pages/shop/ShopLoginPage.tsx`
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Modify: `frontend/src/pages/user/UserListPage.tsx`
- Modify: `frontend/src/pages/user/UserCreatePage.tsx`
- Modify: `frontend/src/pages/ticket/TicketListPage.tsx`
- Modify: `frontend/src/pages/order/OrderListPage.tsx`
- Modify: `frontend/src/pages/coupon/AdminCouponListPage.tsx`

- [ ] **Step 1: Write a failing source-contract test**

Read each page with `readFileSync` and assert these exact IDs are present:

```javascript
const contracts = {
  'frontend/src/pages/auth/LoginPage.tsx': [
    'admin-login-form', 'admin-username', 'admin-password', 'admin-login-submit'
  ],
  'frontend/src/pages/shop/ShopLoginPage.tsx': [
    'shop-login-form', 'shop-username', 'shop-password', 'shop-login-submit'
  ],
  'frontend/src/pages/shop/ShopPage.tsx': [
    'shop-search-input', 'shop-search-submit', 'shop-ticket-grid', 'shop-empty-result'
  ],
  'frontend/src/pages/user/UserListPage.tsx': [
    'admin-user-page', 'admin-user-search', 'admin-user-table', 'admin-user-create'
  ],
  'frontend/src/pages/user/UserCreatePage.tsx': [
    'admin-user-create-form', 'admin-user-create-submit'
  ],
  'frontend/src/pages/ticket/TicketListPage.tsx': ['admin-ticket-table'],
  'frontend/src/pages/order/OrderListPage.tsx': ['admin-order-table'],
  'frontend/src/pages/coupon/AdminCouponListPage.tsx': ['admin-coupon-table']
};
```

- [ ] **Step 2: Run and verify RED**

```powershell
node frontend/tests/coursePlaywrightContractSource.test.mjs
```

Expected: assertion failure naming the first missing test ID.

- [ ] **Step 3: Add semantic `data-testid` attributes**

Attach each ID to the real form, input, submit button, table wrapper, ticket grid, or empty-state element named by the contract. For Ant Design inputs, put `data-testid` directly on `Input`/`Input.Password`; for tables, wrap `Table` in a `<div data-testid="...">`. Do not add hidden duplicate elements and do not change labels, navigation, API calls, or CSS.

- [ ] **Step 4: Run source contracts and TypeScript build**

```powershell
node frontend/tests/coursePlaywrightContractSource.test.mjs
npm.cmd --prefix frontend run build
```

Expected: the contract prints `course Playwright contract source tests passed`; Vite build exits 0.

- [ ] **Step 5: Commit only locator contract files**

```powershell
git add frontend/tests/coursePlaywrightContractSource.test.mjs frontend/src/pages/auth/LoginPage.tsx frontend/src/pages/shop/ShopLoginPage.tsx frontend/src/pages/shop/ShopPage.tsx frontend/src/pages/user/UserListPage.tsx frontend/src/pages/user/UserCreatePage.tsx frontend/src/pages/ticket/TicketListPage.tsx frontend/src/pages/order/OrderListPage.tsx frontend/src/pages/coupon/AdminCouponListPage.tsx
git commit -m "test: expose stable Playwright UI contract"
```

---

### Task 4: Specify the course configuration, data, and API client

**Files:**
- Create: `D:\Education\playwright课程\tests\unit\test_course_config.py`
- Create: `D:\Education\playwright课程\tests\unit\test_course_data.py`
- Create: `D:\Education\playwright课程\tests\unit\test_booking_api.py`
- Modify: `D:\Education\playwright课程\requirements.txt`

- [ ] **Step 1: Write failing configuration and data tests**

```python
def test_course_config_uses_booking_defaults(monkeypatch):
    for key in ("BOOKING_WEB_URL", "BOOKING_API_URL", "BOOKING_ADMIN_USERNAME"):
        monkeypatch.delenv(key, raising=False)
    config = CourseConfig.from_env()
    assert config.web_url == "http://127.0.0.1:3000"
    assert config.api_url == "http://127.0.0.1:8088/api"
    assert config.admin_username == "admin"

def test_course_data_is_run_scoped():
    data = CourseData("a1b2c3d4")
    assert data.username.startswith("pw-course-a1b2c3d4-")
    assert data.user_remark.startswith("pw-course-a1b2c3d4-")
```

For `BookingApiClient`, use a fake `requests.Session` response and assert `admin_login()` returns the nested `data`, a non-zero business `code` raises `BookingApiError`, and `cleanup("a1b2c3d4")` sends `X-Course-Test-Key` plus `{"runId":"a1b2c3d4"}`.

- [ ] **Step 2: Run and verify RED**

```powershell
python -m pytest tests/unit/test_course_config.py tests/unit/test_course_data.py tests/unit/test_booking_api.py -q
```

Expected: collection errors because the three implementation modules do not exist.

- [ ] **Step 3: Add required dependencies**

Append exact compatible floors to `requirements.txt`:

```text
requests>=2.32.0
python-dotenv>=1.0.0
```

- [ ] **Step 4: Implement the three shared modules**

`CourseConfig` is a frozen dataclass with `from_env()`, strips trailing slashes, and contains `web_url`, `api_url`, `admin_username`, `admin_password`, `course_test_key`, and `request_timeout`.

```python
from dataclasses import dataclass
import os

@dataclass(frozen=True)
class CourseConfig:
    web_url: str
    api_url: str
    admin_username: str
    admin_password: str
    course_test_key: str
    request_timeout: float

    @classmethod
    def from_env(cls) -> "CourseConfig":
        return cls(
            web_url=os.getenv("BOOKING_WEB_URL", "http://127.0.0.1:3000").rstrip("/"),
            api_url=os.getenv("BOOKING_API_URL", "http://127.0.0.1:8088/api").rstrip("/"),
            admin_username=os.getenv("BOOKING_ADMIN_USERNAME", "admin"),
            admin_password=os.getenv("BOOKING_ADMIN_PASSWORD", "Admin@123456"),
            course_test_key=os.getenv("COURSE_TEST_KEY", "playwright-course-local"),
            request_timeout=float(os.getenv("BOOKING_REQUEST_TIMEOUT", "10")),
        )
```

`CourseData` validates the run ID, derives a numeric suffix for a unique phone number, and exposes deterministic username, nickname, email, phone, password, search keyword, and user remark fields prefixed with `pw-course-{runId}`.

```python
from dataclasses import dataclass, field
import re

@dataclass(frozen=True)
class CourseData:
    run_id: str
    prefix: str = field(init=False)
    username: str = field(init=False)
    nickname: str = field(init=False)
    email: str = field(init=False)
    phone: str = field(init=False)
    password: str = "Course@123456"
    search_keyword: str = "演唱会"
    user_remark: str = field(init=False)

    def __post_init__(self) -> None:
        if not re.fullmatch(r"[a-z0-9]{8,32}", self.run_id):
            raise ValueError("run_id must contain 8-32 lowercase letters or digits")
        prefix = f"pw-course-{self.run_id}"
        phone_suffix = int.from_bytes(self.run_id.encode("utf-8"), "big") % 1_000_000_000
        object.__setattr__(self, "prefix", prefix)
        object.__setattr__(self, "username", f"{prefix}-user")
        object.__setattr__(self, "nickname", f"{prefix}-学员")
        object.__setattr__(self, "email", f"{prefix}@example.test")
        object.__setattr__(self, "phone", f"13{phone_suffix:09d}")
        object.__setattr__(self, "user_remark", f"{prefix}-order")
```

`BookingApiClient` owns one `requests.Session`, unwraps the BookingApi `Result` envelope, stores the admin bearer token after login, exposes `health()`, `admin_login()`, `register_user()`, `list_tickets()`, `list_admin_users()`, `create_admin_user()`, and `cleanup()`, and never logs passwords or tokens.

```python
from typing import Any
import requests

from utils.course_config import CourseConfig
from utils.course_data import CourseData

class BookingApiError(RuntimeError):
    pass

class BookingApiClient:
    def __init__(self, config: CourseConfig, session: requests.Session | None = None):
        self.config = config
        self.session = session or requests.Session()
        self.admin_token: str | None = None

    def health(self) -> dict[str, Any]:
        response = self.session.get(
            f"{self.config.api_url}/actuator/health",
            timeout=self.config.request_timeout,
        )
        response.raise_for_status()
        return response.json()

    def _request(self, method: str, path: str, *, admin: bool = False, **kwargs) -> Any:
        headers = dict(kwargs.pop("headers", {}))
        if admin:
            if not self.admin_token:
                raise BookingApiError("admin_login() must be called first")
            headers["Authorization"] = f"Bearer {self.admin_token}"
        response = self.session.request(
            method,
            f"{self.config.api_url}{path}",
            headers=headers,
            timeout=self.config.request_timeout,
            **kwargs,
        )
        response.raise_for_status()
        payload = response.json()
        if payload.get("code") != 0:
            raise BookingApiError(f"{method} {path}: {payload.get('code')} {payload.get('message')}")
        return payload.get("data")

    def admin_login(self) -> dict[str, Any]:
        data = self._request("POST", "/admin/login", json={
            "username": self.config.admin_username,
            "password": self.config.admin_password,
        })
        self.admin_token = data["token"]
        return data

    def register_user(self, data: CourseData) -> int:
        return self._request("POST", "/user/register", json={
            "username": data.username,
            "password": data.password,
            "phone": data.phone,
            "nickname": data.nickname,
            "source": "h5",
        })

    def list_tickets(self, page_size: int = 10) -> dict[str, Any]:
        return self._request("GET", "/tickets", params={"pageNum": 1, "pageSize": page_size})

    def list_admin_users(self, keyword: str) -> dict[str, Any]:
        return self._request("GET", "/admin/users", admin=True,
                             params={"page": 1, "pageSize": 20, "keyword": keyword})

    def create_admin_user(self, data: CourseData) -> int:
        return self._request("POST", "/admin/users", admin=True, json={
            "username": data.username,
            "password": data.password,
            "phone": data.phone,
            "email": data.email,
            "nickname": data.nickname,
        })

    def cleanup(self, run_id: str) -> dict[str, Any]:
        return self._request("POST", "/test-support/cleanup", headers={
            "X-Course-Test-Key": self.config.course_test_key,
        }, json={"runId": run_id})
```

- [ ] **Step 5: Run and verify GREEN**

Run the same three-test command. Expected: all tests pass.

---

### Task 5: Add pytest lifecycle fixtures and content contracts

**Files:**
- Create: `D:\Education\playwright课程\tests\unit\test_course_content_contract.py`
- Modify: `D:\Education\playwright课程\conftest.py`
- Modify: `D:\Education\playwright课程\pytest.ini`
- Modify: `D:\Education\playwright课程\config\base.json`
- Modify: `D:\Education\playwright课程\config\test.json`

- [ ] **Step 1: Write a failing content contract**

The test recursively scans Python/Markdown/JSON/YAML/text files under `pages`, `tests`, `lessons`, and the 36-unit companion. Fail on fake business targets `api.example.com`, `jsonplaceholder.typicode.com`, `reqres.in`, `saucedemo.com`, `https://example.com/login`, `https://example.com/products`, and `http://localhost:3000/login`. Do not reject `playwright.dev`, `demo.playwright.dev`, `the-internet.herokuapp.com`, `httpbin.org`, or plain `https://example.com` used for navigation/performance demonstrations.

Add positive assertions that root config contains `127.0.0.1:3000` and `127.0.0.1:8088/api`, and the companion final-project test contains `/shop` or `/login` plus `/api`.

```python
from pathlib import Path
import json
import pytest

COURSE_ROOT = Path(__file__).resolve().parents[2]
COMPANION_ROOT = (
    COURSE_ROOT / "图文课程" / "Playwright_Python_36张图配套代码"
    / "playwright_python_course_code_01_36"
)
TEXT_SUFFIXES = {".py", ".md", ".json", ".yml", ".yaml", ".txt"}
FORBIDDEN_BUSINESS_TARGETS = (
    "api.example.com",
    "jsonplaceholder.typicode.com",
    "reqres.in",
    "saucedemo.com",
    "https://example.com/login",
    "https://example.com/products",
    "http://localhost:3000/login",
)

def course_text_files():
    for root in (COURSE_ROOT / "pages", COURSE_ROOT / "tests",
                 COURSE_ROOT / "lessons", COMPANION_ROOT):
        for path in root.rglob("*"):
            if path.is_file() and path.suffix.lower() in TEXT_SUFFIXES:
                yield path

@pytest.mark.parametrize("forbidden", FORBIDDEN_BUSINESS_TARGETS)
def test_fake_business_targets_are_removed(forbidden):
    hits = []
    for path in course_text_files():
        if path == Path(__file__).resolve():
            continue
        if forbidden in path.read_text(encoding="utf-8", errors="ignore"):
            hits.append(str(path.relative_to(COURSE_ROOT)))
    assert hits == [], f"{forbidden} remains in: {hits}"

def test_root_config_points_to_booking_api():
    config = json.loads((COURSE_ROOT / "config" / "base.json").read_text(encoding="utf-8"))
    assert config["web"]["base_url"] == "http://127.0.0.1:3000"
    assert config["api"]["base_url"] == "http://127.0.0.1:8088/api"

def test_companion_capstone_uses_real_ui_and_api():
    source = (COMPANION_ROOT / "36_课程总结与毕业项目" / "test_graduation.py").read_text(encoding="utf-8")
    assert "/shop" in source or "/login" in source
    assert "/api" in source or "API_URL" in source
```

- [ ] **Step 2: Run and verify RED**

```powershell
python -m pytest tests/unit/test_course_content_contract.py -q
```

Expected: failure listing current fake business references.

- [ ] **Step 3: Implement session and function fixtures**

In `conftest.py`, add:

- session `course_config` fixture;
- session autouse `booking_services_ready` fixture for tests marked `booking`, probing backend health and frontend root without starting services;
- function `course_run_id` fixture using the first 12 lowercase hex characters of `uuid4().hex`;
- function `course_data` fixture;
- function `booking_api` fixture that logs in the admin only when requested;
- function autouse cleanup only for tests marked `writes_data`, calling cleanup from a `yield` finalizer.

Use `pytest_runtest_setup` or marker inspection so unit tests and `--collect-only` never contact the running project. Register `booking`, `writes_data`, and `external_demo` markers in `pytest.ini`.

- [ ] **Step 4: Update JSON defaults**

Set UI `base_url` to `http://127.0.0.1:3000`, API `base_url` to `http://127.0.0.1:8088/api`, and remove PostgreSQL credentials that are unrelated to BookingApi. Keep browser/report settings unchanged.

- [ ] **Step 5: Run fixture unit tests and verify collect-only**

```powershell
python -m pytest tests/unit -q
python -m pytest --collect-only -q
```

Expected: unit tests pass; collection completes without a network request.

---

### Task 6: Replace root Page Objects with BookingApi pages

**Files:**
- Modify: `D:\Education\playwright课程\pages\login_page.py`
- Modify: `D:\Education\playwright课程\pages\search_page.py`
- Modify: `D:\Education\playwright课程\pages\user_management_page.py`
- Modify: `D:\Education\playwright课程\tests\test_login.py`
- Modify: `D:\Education\playwright课程\tests\test_search.py`
- Modify: `D:\Education\playwright课程\tests\test_user_management.py`

- [ ] **Step 1: Replace oversized placeholder tests with failing behavior tests**

Write these five focused tests first (the Page Object APIs shown here are the required APIs for Step 3):

```python
import re
import pytest
from playwright.sync_api import expect

from pages.login_page import LoginPage
from pages.search_page import SearchPage
from pages.user_management_page import UserManagementPage

@pytest.mark.booking
def test_admin_can_login(page, course_config):
    login = LoginPage(page, course_config.web_url)
    login.open()
    login.login(course_config.admin_username, course_config.admin_password)
    expected_home = re.escape(course_config.web_url.rstrip("/")) + r"/?$"
    expect(page).to_have_url(re.compile(expected_home))

@pytest.mark.booking
def test_wrong_admin_password_shows_error(page, course_config):
    login = LoginPage(page, course_config.web_url)
    login.open()
    login.login(course_config.admin_username, "Wrong@123456")
    expect(login.error_message).to_be_visible()

@pytest.mark.booking
def test_shop_search_matches_ticket_name(page, booking_api, course_config):
    tickets = booking_api.list_tickets(page_size=10)["list"]
    assert tickets, "BookingApi 演示库需要至少一条在售票品"
    target_name = tickets[0]["name"]
    shop = SearchPage(page, course_config.web_url)
    shop.open()
    shop.search(target_name)
    expect(shop.ticket_cards.filter(has_text=target_name).first).to_be_visible()

@pytest.mark.booking
def test_shop_search_has_empty_state(page, course_config):
    shop = SearchPage(page, course_config.web_url)
    shop.open()
    shop.search("pw-course-no-such-ticket")
    expect(shop.empty_result).to_be_visible()

@pytest.mark.booking
@pytest.mark.writes_data
def test_admin_can_create_and_find_run_scoped_user(
        page, course_config, course_data, booking_api):
    login = LoginPage(page, course_config.web_url)
    login.open()
    login.login(course_config.admin_username, course_config.admin_password)
    users = UserManagementPage(page, course_config.web_url)
    users.open()
    users.create_user(course_data)
    users.search(course_data.username)
    expect(users.rows.filter(has_text=course_data.username).first).to_be_visible()
    booking_api.admin_login()
    api_users = booking_api.list_admin_users(course_data.username)["list"]
    assert any(user["username"] == course_data.username for user in api_users)
```

Assert URL transitions, visible feedback, returned ticket names, row content, and the API-visible user record. Do not retain placeholder delete, bulk edit, social login, forgot-password, or role permutations that BookingApi does not expose in the current UI.

- [ ] **Step 2: Run and verify RED**

```powershell
python -m pytest tests/test_login.py tests/test_search.py tests/test_user_management.py -q
```

Expected: failures point to old selectors and placeholder routes.

- [ ] **Step 3: Implement concise Page Objects**

`LoginPage` navigates to `{web_url}/login`, fills `admin-username` and `admin-password`, submits `admin-login-submit`, and waits for `/` plus `admin-user-page` only when navigating to user management.

`SearchPage` navigates to `{web_url}/shop`, fills `shop-search-input`, clicks `shop-search-submit`, reads ticket cards from `shop-ticket-grid`, and exposes the `shop-empty-result` assertion.

`UserManagementPage` assumes an authenticated admin, navigates to `{web_url}/users`, uses `admin-user-search`, `admin-user-table`, and `admin-user-create`, and submits the real create-user form fields.

- [ ] **Step 4: Run core UI tests and verify GREEN**

Run the same command with the already-running BookingApi. Expected: five tests pass and the generated user is absent after cleanup.

---

### Task 7: Align root datasets and teacher documentation

**Files:**
- Modify: `D:\Education\playwright课程\data\json\login_data.json`
- Modify: `D:\Education\playwright课程\data\json\search_data.json`
- Modify: `D:\Education\playwright课程\data\json\users.json`
- Modify: `D:\Education\playwright课程\README.md`
- Test: `D:\Education\playwright课程\tests\unit\test_course_content_contract.py`

- [ ] **Step 1: Add dataset assertions to the content contract**

Assert login data names the `admin` teaching account, search data uses ticket-domain keywords, and user templates use `pw-course-` prefixes without embedding the cleanup key or a real token.

- [ ] **Step 2: Run and verify RED**

Expected: current generic datasets fail the new assertions.

- [ ] **Step 3: Rewrite datasets and README**

Document these commands exactly:

```powershell
python -m pytest -m booking tests/test_login.py -v
python -m pytest -m booking tests/test_search.py -v
python -m pytest -m "booking and writes_data" tests/test_user_management.py -v
```

State that BookingApi must already be available on ports 3000 and 8088, tests do not start it, and write tests clean only `pw-course-{runId}` data. Remove references to nonexistent `utils/api_client.py`, PostgreSQL test databases, social login, and placeholder pages.

- [ ] **Step 4: Run content and root smoke tests**

Expected: content contract passes; root smoke tests pass.

---

### Task 8: Map the 32-lesson business examples to BookingApi

**Files:**
- Create: `D:\Education\playwright课程\lessons\course_targets.py`
- Modify: `D:\Education\playwright课程\lessons\README.md`
- Modify: `lessons/lesson01/README.md`
- Modify: `lessons/lesson02/README.md`
- Modify: `lessons/lesson03/README.md`
- Modify: `lessons/lesson04/README.md`
- Modify: `lessons/lesson05/README.md`
- Modify: `lessons/lesson06/README.md`
- Modify: `lessons/lesson07/README.md`
- Modify: `lessons/lesson08/README.md`
- Modify: `lessons/lesson10/README.md`
- Modify: `lessons/lesson11/README.md`
- Modify: `lessons/lesson13/README.md`
- Modify: `lessons/lesson14/README.md`
- Modify: `lessons/lesson15/README.md`
- Modify: `lessons/lesson17/README.md`
- Modify: `lessons/lesson18/README.md`
- Modify: `lessons/lesson20/README.md`
- Modify: `lessons/lesson21/README.md`
- Modify: `lessons/lesson23/README.md`
- Modify: `lessons/lesson25/README.md`
- Modify: `lessons/lesson26/README.md`
- Modify: `lessons/lesson27/README.md`
- Modify: `lessons/lesson29/README.md`
- Modify: `lessons/lesson30/README.md`
- Modify: `lessons/lesson31/README.md`
- Modify: `lessons/lesson03/exercises.py`
- Modify: `lessons/lesson03/solutions.py`
- Modify: `lessons/lesson09/README.md`
- Modify: `lessons/lesson09/solutions.py`
- Modify: `lessons/lesson10/solutions.py`
- Modify: `lessons/lesson11/code_examples.py`
- Modify: `lessons/lesson11/exercises.py`
- Modify: `lessons/lesson11/solutions.py`
- Modify: `lessons/lesson12/README.md`
- Modify: `lessons/lesson12/code_examples.py`
- Modify: `lessons/lesson12/exercises.py`
- Modify: `lessons/lesson12/solutions.py`
- Modify: `lessons/lesson13/exercises.py`
- Modify: `lessons/lesson13/solutions.py`
- Modify: `lessons/lesson16/README.md`
- Modify: `lessons/lesson16/code_examples.py`
- Modify: `lessons/lesson17/code_examples.py`
- Modify: `lessons/lesson19/README.md`
- Modify: `lessons/lesson19/code_examples.py`
- Modify: `lessons/lesson19/exercises.py`
- Modify: `lessons/lesson19/solutions.py`
- Modify: `lessons/lesson22/README.md`
- Modify: `lessons/lesson24/solutions.py`
- Modify: `lessons/lesson26/code_examples.py`
- Modify: `lessons/lesson27/code_examples.py`
- Modify: `lessons/lesson28/README.md`

- [ ] **Step 1: Run the content contract and save the exact RED list**

```powershell
python -m pytest tests/unit/test_course_content_contract.py -q
```

Expected: the files above appear in the failure report.

- [ ] **Step 2: Add shared lesson targets**

```python
import os

WEB_URL = os.getenv("BOOKING_WEB_URL", "http://127.0.0.1:3000").rstrip("/")
API_URL = os.getenv("BOOKING_API_URL", "http://127.0.0.1:8088/api").rstrip("/")
ADMIN_USERNAME = os.getenv("BOOKING_ADMIN_USERNAME", "admin")
ADMIN_PASSWORD = os.getenv("BOOKING_ADMIN_PASSWORD", "Admin@123456")

ADMIN_LOGIN_URL = f"{WEB_URL}/login"
SHOP_URL = f"{WEB_URL}/shop"
ADMIN_USERS_URL = f"{WEB_URL}/users"
```

- [ ] **Step 3: Replace fake business examples by chapter purpose**

- Navigation and POM login examples use `ADMIN_LOGIN_URL` and the four admin login test IDs.
- API examples use `API_URL + "/tickets"`, `API_URL + "/categories"`, and `API_URL + "/admin/login"`; validate BookingApi `code`, `message`, and nested `data`.
- Network mock examples intercept `**/api/tickets*` while the page is on `/shop` and fulfill a BookingApi-shaped page result.
- Visual, multi-browser, reporting, and CI examples target `/shop` or `/login` instead of fake storefront pages.
- `example.com`, `httpbin.org`, and `the-internet.herokuapp.com` remain only where the lesson is specifically about generic navigation, delay, form security, iframe, upload, or browser mechanics.

- [ ] **Step 4: Update the corresponding README explanations**

Every modified README states the real route/API, whether the example is read-only or writes data, and why any retained external site is necessary.

- [ ] **Step 5: Run syntax and content verification**

```powershell
python -m compileall -q lessons
python -m pytest tests/unit/test_course_content_contract.py -q
```

Expected: no syntax errors and no forbidden fake business targets.

---

### Task 9: Turn lesson 32 into a BookingApi capstone

**Files:**
- Modify: `D:\Education\playwright课程\lessons\lesson32\README.md`
- Modify: `D:\Education\playwright课程\lessons\lesson32\code_examples.py`
- Modify: `D:\Education\playwright课程\lessons\lesson32\exercises.py`
- Modify: `D:\Education\playwright课程\lessons\lesson32\solutions.py`
- Modify: `D:\Education\playwright课程\lessons\lesson32\ai_testing_components.py`

- [ ] **Step 1: Add capstone contract assertions**

Assert these files mention `BookingApi`, `/shop`, `/login`, `/api/tickets`, `runId`, and automatic cleanup; reject generic “测试商品/无线耳机” storefront terminology.

- [ ] **Step 2: Run and verify RED**

Expected: capstone contract fails on the current generic project material.

- [ ] **Step 3: Rewrite the executable capstone path**

The capstone demonstrates: health check, admin login, API ticket discovery, UI shop search, API-created run-scoped user, UI user search, response/business assertions, evidence capture, and cleanup. Preserve conceptual strategy/AI sections, but change their examples and review prompts to the ticketing domain.

- [ ] **Step 4: Verify capstone syntax and focused examples**

```powershell
python -m compileall -q lessons\lesson32
python -m pytest tests/unit/test_course_content_contract.py -q
```

Expected: both commands exit 0.

---

### Task 10: Add shared support to the 36-unit companion

**Files:**
- Create: companion `course_support.py`
- Create: companion `conftest.py`
- Create: companion `.env.example`
- Modify: companion `requirements.txt`
- Modify: companion `README.md`
- Modify: companion `01_课程总览/README.md`
- Modify: companion `02_环境搭建与初始化/README.md`
- Modify: companion `03_第一个测试与运行方式/README.md`
- Modify: companion `04_Locator定位器/README.md`
- Modify: companion `05_自动等待与WebFirst断言/README.md`
- Modify: companion `06_调试录制Trace/README.md`
- Modify: companion `07_POM_Fixtures_Hooks/README.md`
- Modify: companion `08_认证数据API网络控制/README.md`
- Modify: companion `09_质量与交付/README.md`
- Modify: companion `10_AI融合实战/README.md`
- Modify: companion `11_项目目录与职责/README.md`
- Modify: companion `12_conftest与Fixtures/README.md`
- Modify: companion `13_PageObject实战/README.md`
- Modify: companion `14_pytest参数化与数据驱动/README.md`
- Modify: companion `15_复用登录态storage_state/README.md`
- Modify: companion `16_API加UI联合测试/README.md`
- Modify: companion `17_Route_Mock网络拦截/README.md`
- Modify: companion `18_多窗口_iframe_上传下载/README.md`
- Modify: companion `03_第一个测试与运行方式/test_login.py`
- Modify: companion `07_POM_Fixtures_Hooks/conftest.py`
- Modify: companion `07_POM_Fixtures_Hooks/pages/login_page.py`
- Modify: companion `07_POM_Fixtures_Hooks/test_login.py`
- Modify: companion `08_认证数据API网络控制/test_data_driven.py`
- Modify: companion `08_认证数据API网络控制/test_mock.py`
- Modify: companion `13_PageObject实战/pages/login_page.py`
- Modify: companion `13_PageObject实战/pages/product_page.py`
- Modify: companion `13_PageObject实战/test_pom.py`
- Modify: companion `14_pytest参数化与数据驱动/data/search_cases.json`
- Modify: companion `14_pytest参数化与数据驱动/test_parametrize.py`
- Modify: companion `15_复用登录态storage_state/save_state.py`
- Modify: companion `15_复用登录态storage_state/test_reuse_state.py`
- Modify: companion `16_API加UI联合测试/test_api_ui.py`
- Modify: companion `17_Route_Mock网络拦截/test_route.py`

- [ ] **Step 1: Extend the content contract for companion real-project units**

Assert units 03, 07, 13, 15, and 16 import `course_support`, unit 16 calls `/tickets`, and units 08/17 mock `**/api/tickets*` with BookingApi-shaped responses.

- [ ] **Step 2: Run and verify RED**

Expected: companion contract assertions fail.

- [ ] **Step 3: Implement companion support**

`course_support.py` mirrors the root environment defaults, provides `unique_run_id()`, `admin_login(page)`, `api_headers(token)`, `assert_booking_result(response)`, and `cleanup_run(run_id)`. Root companion `conftest.py` provides configuration, run ID, and cleanup fixtures without starting services.

- [ ] **Step 4: Replace generic storefront teaching flows**

Use real admin login and shop search for the listed units. Keep small `page.set_content()` examples only when the unit teaches a browser mechanism whose behavior BookingApi does not expose. Unit 16 must use Playwright `APIRequestContext` to query a real ticket then verify that ticket through `/shop` UI.

- [ ] **Step 5: Align unit 01-18 README files**

Each README names one of three target types: BookingApi UI/API, local `page.set_content()` teaching fixture, or approved external browser demo. Include the exact command for that unit and do not claim a local fixture is a real backend flow.

- [ ] **Step 6: Run focused companion tests**

```powershell
python -m pytest "03_第一个测试与运行方式" "07_POM_Fixtures_Hooks" "13_PageObject实战" "16_API加UI联合测试" -q
```

Expected: focused tests pass against the running project.

---

### Task 11: Convert the 36-unit enterprise path and delivery docs

**Files:**
- Modify: companion `19_报告截图视频Trace/README.md`
- Modify: companion `20_CI_Jenkins_GitHubActions/README.md`
- Modify: companion `21_配置中心与命令规范/README.md`
- Modify: companion `22_浏览器启动与执行模式/README.md`
- Modify: companion `23_并发执行与分层执行/README.md`
- Modify: companion `24_标签过滤与精准运行/README.md`
- Modify: companion `25_超时重试与失败隔离/README.md`
- Modify: companion `26_日志与可观测性/README.md`
- Modify: companion `27_Allure报告/README.md`
- Modify: companion `28_多环境配置/README.md`
- Modify: companion `29_BasePage与公共组件/README.md`
- Modify: companion `30_电商实战主流程/README.md`
- Modify: companion `31_订单查询取消与清理/README.md`
- Modify: companion `32_API_UI联合测试进阶/README.md`
- Modify: companion `33_持续集成落地/README.md`
- Modify: companion `34_最终企业级项目结构/README.md`
- Modify: companion `35_官方AI提效工作流/README.md`
- Modify: companion `36_课程总结与毕业项目/README.md`
- Modify: companion `20_CI_Jenkins_GitHubActions/.github/workflows/ci.yml`
- Modify: companion `20_CI_Jenkins_GitHubActions/Jenkinsfile`
- Modify: companion `21_配置中心与命令规范/commands.txt`
- Modify: companion `21_配置中心与命令规范/tests/test_config.py`
- Modify: companion `23_并发执行与分层执行/test_layers.py`
- Modify: companion `24_标签过滤与精准运行/test_filter.py`
- Modify: companion `25_超时重试与失败隔离/test_stability.py`
- Modify: companion `27_Allure报告/test_allure.py`
- Modify: companion `28_多环境配置/.env.example`
- Modify: companion `28_多环境配置/config.py`
- Modify: companion `28_多环境配置/test_config.py`
- Modify: companion `29_BasePage与公共组件/base_page.py`
- Modify: companion `29_BasePage与公共组件/components.py`
- Modify: companion `30_电商实战主流程/test_order_flow.py`
- Modify: companion `31_订单查询取消与清理/order_api.py`
- Modify: companion `31_订单查询取消与清理/test_order_cleanup.py`
- Modify: companion `32_API_UI联合测试进阶/test_api_ui_advanced.py`
- Modify: companion `33_持续集成落地/Jenkinsfile`
- Modify: companion `33_持续集成落地/commands.txt`
- Modify: companion `34_最终企业级项目结构/.env.example`
- Modify: companion `34_最终企业级项目结构/conftest.py`
- Modify: companion `34_最终企业级项目结构/tests/test_smoke.py`
- Modify: companion `35_官方AI提效工作流/AI_REVIEW_CHECKLIST.md`
- Modify: companion `35_官方AI提效工作流/commands.txt`
- Modify: companion `36_课程总结与毕业项目/PROJECT_CHECKLIST.md`
- Modify: companion `36_课程总结与毕业项目/test_graduation.py`

- [ ] **Step 1: Add enterprise-path contract assertions and verify RED**

Assert that units 28, 30, 31, 32, 34, and 36 use BookingApi URLs, `booking`/`writes_data` markers, and cleanup; assert CI examples clearly state that a reachable BookingApi test environment is required and do not pretend to start it.

- [ ] **Step 2: Convert the final business flow**

Unit 30 becomes admin login → query real tickets → shop search → select ticket detail. Unit 31 creates a run-scoped customer/order through API, verifies/cancels it, and cleans the run. Unit 32 prepares data by API and verifies state by UI and API. Units 34/36 expose a minimal smoke suite using the same fixtures.

- [ ] **Step 3: Align configuration, CI, reporting, and AI examples**

Use `BOOKING_WEB_URL`, `BOOKING_API_URL`, and `COURSE_TEST_KEY`; use `/shop` in codegen examples; label external demos; keep all tokens/passwords in environment variables except documented local teaching defaults.

- [ ] **Step 4: Align unit 19-36 README files**

Document the exact BookingApi prerequisite, marker command, read/write behavior, evidence output, and cleanup behavior for every enterprise-path unit. The CI units must say that CI needs a separately reachable BookingApi test environment because this course does not start the application.

- [ ] **Step 5: Run companion syntax, collection, and focused smoke**

```powershell
python -m compileall -q .
python -m pytest --collect-only -q
python -m pytest "30_电商实战主流程" "31_订单查询取消与清理" "32_API_UI联合测试进阶" "34_最终企业级项目结构" "36_课程总结与毕业项目" -q
```

Expected: syntax and collection pass; focused smoke passes and test-owned users/orders are absent afterward.

---

### Task 12: Regenerate manifest and delivery archive

**Files:**
- Modify: companion `MANIFEST.json`
- Modify: `D:\Education\playwright课程\图文课程\Playwright_Python_36张图配套代码.zip`
- Create: `D:\Education\playwright课程\tests\unit\test_companion_delivery.py`

- [ ] **Step 1: Write failing manifest/archive tests**

Load `MANIFEST.json` and assert every declared file exists; assert `course_support.py`, `conftest.py`, `.env.example`, and `requirements.txt` are declared; open the ZIP and assert its normalized file list equals the source delivery file list excluding `__pycache__`, reports, `.pytest_cache`, screenshots, videos, traces, and `.env`.

- [ ] **Step 2: Run and verify RED**

```powershell
python -m pytest tests/unit/test_companion_delivery.py -q
```

Expected: new support files are missing from the manifest/archive.

- [ ] **Step 3: Update manifest and rebuild the ZIP**

Update the manifest with exact relative paths. Build the ZIP from the companion parent so its top-level directory remains `playwright_python_course_code_01_36`; exclude generated artifacts and secrets.

- [ ] **Step 4: Run delivery verification and inspect the archive listing**

Expected: delivery test passes and no `.env`, logs, reports, caches, screenshots, videos, or traces appear in the archive.

---

### Task 13: Full verification and handoff

**Files:**
- Modify: `D:\Education\playwright课程\README.md` only if verification reveals a command mismatch.
- Modify: design/plan documents only if the implemented contract intentionally differs.

- [ ] **Step 1: Run BookingApi focused regression**

```powershell
mvn.cmd -pl booking-api -Dtest=CourseTestCleanupServiceTest,CourseTestSupportControllerTest,AdminAuthControllerTest,UserControllerTest test
node frontend/tests/coursePlaywrightContractSource.test.mjs
npm.cmd --prefix frontend run build
```

Expected: all commands exit 0.

- [ ] **Step 2: Run course static and unit verification**

```powershell
python -m compileall -q pages utils tests lessons
python -m pytest tests/unit -q
python -m pytest --collect-only -q
```

Expected: all commands exit 0 without contacting services during collection.

- [ ] **Step 3: Run real-project smoke**

```powershell
python -m pytest -m booking tests/test_login.py tests/test_search.py tests/test_user_management.py -q
```

Expected: all core tests pass against the user-started BookingApi.

- [ ] **Step 4: Prove cleanup**

Capture the test run ID from pytest output, query `/api/admin/users` with the admin token for `pw-course-{runId}`, and assert the returned list is empty. If cleanup fails, preserve the data and report the exact run ID; do not issue broad SQL or database reset commands.

- [ ] **Step 5: Review the final diff without touching unrelated work**

In BookingApi, inspect `git diff --name-only` and `git status --short`; confirm only planned BookingApi files were changed by this implementation in addition to pre-existing user changes. In the course, list changed timestamps and test outputs because it has no Git history.

- [ ] **Step 6: Request code review**

Use the requesting-code-review skill on the implemented BookingApi commit range and the course file list. Resolve all Critical and Important findings, rerun the affected verification, then provide the teacher-facing commands and note which external demo sites remain.
