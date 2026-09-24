# Playwright Foundation and Safe Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the secure multi-environment pytest-playwright foundation and guarantee that every temporary API/UI user can be deleted with its related test data.

**Architecture:** Extend the existing dev-only test-support controller with an idempotent single-user endpoint, then build focused Python configuration, API, data, cleanup, and authentication units around it. Pytest fixtures own the complete create-login-inject-yield-cleanup lifecycle and refuse public write tests when cleanup is unavailable.

**Tech Stack:** Java 17, Spring Boot 3.2, JUnit 5, Mockito, JdbcTemplate, Python 3.11+, pytest, pytest-playwright, requests, PyYAML, jsonschema, Allure.

---

## File structure

- Modify `booking-api/src/main/resources/application-dev.yml`: remove committed secret defaults and make teaching-test support opt-in.
- Modify `booking-api/src/main/java/com/ticket/app/controller/testsupport/CourseTestSupportController.java`: add authenticated single-user deletion.
- Modify `booking-api/src/main/java/com/ticket/app/service/testsupport/CourseTestCleanupService.java`: expose one-user cleanup through the existing ownership and dependency rules.
- Modify the matching controller and service tests under `booking-api/src/test/java/.../testsupport/`.
- Replace `automation-tests/requirements.txt` and `automation-tests/pytest.ini`: supported dependency and marker baseline.
- Create `automation-tests/config/{local,public,schema}.yaml`: non-secret environment defaults and schema.
- Create `automation-tests/framework/config/{models,loader,pytest_plugin}.py`: immutable validated configuration.
- Create `automation-tests/framework/{api,data,cleanup,auth}/`: focused runtime units.
- Create `automation-tests/tests/framework/`: unit tests for the framework itself.
- Create `automation-tests/conftest.py`: lifecycle composition only.

### Task 1: Remove committed secret defaults

**Files:**
- Create: `booking-api/src/test/java/com/ticket/app/config/ApplicationDevSecretSourceTest.java`
- Modify: `booking-api/src/main/resources/application-dev.yml`

- [ ] **Step 1: Write the failing source contract test**

```java
package com.ticket.app.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationDevSecretSourceTest {
    @Test
    void devConfigurationRequiresSecretsFromEnvironment() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application-dev.yml"));
        assertTrue(yaml.contains("${DB_PASSWORD}"));
        assertTrue(yaml.contains("${JWT_SECRET}"));
        assertTrue(yaml.contains("${PAYMENT_CALLBACK_SECRET_KEY}"));
        assertTrue(yaml.contains("${COURSE_TEST_KEY}"));
        assertFalse(yaml.contains("COURSE_TEST_KEY:<SECRET_FROM_ENV>"));
        assertFalse(yaml.matches("(?s).*password:\\s+[^$\\s].*"));
    }
}
```

- [ ] **Step 2: Verify RED**

Run: `mvn.cmd -pl booking-api -Dtest=ApplicationDevSecretSourceTest test`

Expected: FAIL because `application-dev.yml` currently contains literal development defaults.

- [ ] **Step 3: Replace secret-bearing values**

Use this exact shape in `application-dev.yml`:

```yaml
spring:
  datasource:
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
  header: Authorization
  prefix: Bearer

payment:
  callback:
    secret-key: ${PAYMENT_CALLBACK_SECRET_KEY}

course:
  test-support:
    enabled: ${COURSE_TEST_SUPPORT_ENABLED:false}
    key: ${COURSE_TEST_KEY}
```

- [ ] **Step 4: Verify GREEN and commit**

Run: `mvn.cmd -pl booking-api -Dtest=ApplicationDevSecretSourceTest test`

Expected: PASS.

Commit only the two files with message `security: require runtime secrets for dev profile`.

### Task 2: Add the protected single-user cleanup endpoint

**Files:**
- Modify: `booking-api/src/test/java/com/ticket/app/controller/testsupport/CourseTestSupportControllerTest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/testsupport/CourseTestSupportController.java`

- [ ] **Step 1: Add failing controller tests**

```java
@Test
void deleteUserRejectsWrongCourseKey() {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> controller.deleteUser("wrong-key", 7L, "a1b2c3d4"));
    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
}

@Test
void deleteUserDelegatesForMatchingKey() {
    CourseCleanupResponse response = new CourseCleanupResponse(
            "a1b2c3d4", Map.of("user", 1), Map.of(), Map.of());
    when(service.cleanupUser("a1b2c3d4", 7L)).thenReturn(response);

    Result<CourseCleanupResponse> result =
            controller.deleteUser("<SECRET_FROM_ENV>", 7L, "a1b2c3d4");

    assertEquals(response, result.getData());
    verify(service).cleanupUser("a1b2c3d4", 7L);
}
```

- [ ] **Step 2: Verify RED**

Run: `mvn.cmd -pl booking-api -Dtest=CourseTestSupportControllerTest test`

Expected: compilation failure because `deleteUser` and `cleanupUser` do not exist.

- [ ] **Step 3: Extract key validation and add DELETE mapping**

Add `DeleteMapping`, `PathVariable`, and `RequestParam` imports, then implement:

```java
@DeleteMapping("/users/{userId}")
public Result<CourseCleanupResponse> deleteUser(
        @RequestHeader("X-Course-Test-Key") String suppliedKey,
        @PathVariable Long userId,
        @RequestParam String runId) {
    requireValidKey(suppliedKey);
    return Result.success(cleanupService.cleanupUser(runId, userId));
}

private void requireValidKey(String suppliedKey) {
    if (!MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8),
            suppliedKey.getBytes(StandardCharsets.UTF_8))) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid course test key");
    }
}
```

Change the existing batch `cleanup` method to call `requireValidKey(suppliedKey)`.

- [ ] **Step 4: Verify GREEN and commit**

Run: `mvn.cmd -pl booking-api -Dtest=CourseTestSupportControllerTest test`

Expected: PASS.

Commit the controller and controller test with message `feat: expose protected test user cleanup`.

### Task 3: Reuse ownership validation for one-user cleanup

**Files:**
- Modify: `booking-api/src/test/java/com/ticket/app/service/testsupport/CourseTestCleanupServiceTest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/testsupport/CourseTestCleanupService.java`

- [ ] **Step 1: Add failing service tests**

```java
@Test
void cleanupUserDeletesOnlyRunOwnedUser() {
    when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(7L)))
            .thenReturn("pw-course-a1b2c3d4-user");
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

    CourseCleanupResponse response =
            new CourseTestCleanupService(jdbcTemplate).cleanupUser("a1b2c3d4", 7L);

    assertEquals(1, response.deleted().get("user"));
    assertEquals(0, response.rejected().getOrDefault("user", 0));
}

@Test
void cleanupUserRejectsUserOwnedByAnotherRun() {
    when(jdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(7L)))
            .thenReturn("pw-course-deadbeef-user");

    CourseCleanupResponse response =
            new CourseTestCleanupService(jdbcTemplate).cleanupUser("a1b2c3d4", 7L);

    assertEquals(1, response.rejected().get("user"));
    verify(jdbcTemplate, times(0)).update(anyString(), any(Object[].class));
}
```

- [ ] **Step 2: Verify RED**

Run: `mvn.cmd -pl booking-api -Dtest=CourseTestCleanupServiceTest test`

Expected: compilation failure for missing `cleanupUser`.

- [ ] **Step 3: Implement through the batch contract**

```java
@Transactional
public CourseCleanupResponse cleanupUser(String runId, Long userId) {
    if (userId == null || userId <= 0) {
        throw new IllegalArgumentException("userId must be positive");
    }
    return cleanup(new CourseCleanupRequest(
            runId, List.of(userId), List.of(), List.of(), List.of()));
}
```

- [ ] **Step 4: Run focused and package tests**

Run: `mvn.cmd -pl booking-api -Dtest=CourseTestSupportControllerTest,CourseTestCleanupServiceTest,ApplicationDevSecretSourceTest test`

Expected: all tests PASS.

Run: `mvn.cmd -pl booking-api -am test`

Expected: reactor tests PASS; if an unrelated pre-existing failure appears, record its exact test name and preserve the focused green evidence.

Commit with message `test: guarantee run scoped user teardown`.

### Task 4: Establish supported Python dependencies and markers

**Files:**
- Create: `automation-tests/tests/framework/test_dependency_contract.py`
- Replace: `automation-tests/requirements.txt`
- Replace: `automation-tests/pytest.ini`

- [ ] **Step 1: Add dependency contract test**

```python
from pathlib import Path


def test_requirements_use_supported_playwright_stack():
    text = Path("requirements.txt").read_text(encoding="utf-8")
    assert "pytest-playwright" in text
    assert "pytest-xdist" in text
    assert "allure-pytest" in text
    assert "pytest-allure-adaptor" not in text
```

- [ ] **Step 2: Verify RED**

Run from `automation-tests`: `python -m pytest tests/framework/test_dependency_contract.py -q`

Expected: FAIL because Playwright and xdist are absent and the obsolete adaptor is present.

- [ ] **Step 3: Replace dependencies and pytest defaults**

Use:

```text
pytest>=8.3,<9
pytest-playwright>=0.7,<1
playwright>=1.55,<2
pytest-xdist>=3.6,<4
requests>=2.32,<3
PyYAML>=6.0,<7
jsonschema>=4.23,<5
allure-pytest>=2.13,<3
pytest-html>=4.1,<5
```

Configure `pytest.ini` with `testpaths = tests`, `--strict-markers`, and the exact markers `smoke`, `regression`, `api`, `ui`, `e2e`, `write`, `public`, `chromium`, and `cross_browser`. Do not enable retries globally.

- [ ] **Step 4: Install, verify, and commit**

Run: `python -m pip install -r requirements.txt`

Run: `python -m playwright install chromium`

Run: `python -m pytest tests/framework/test_dependency_contract.py -q`

Expected: PASS.

Commit with message `build: add supported pytest playwright stack`.

### Task 5: Implement validated multi-environment configuration

**Files:**
- Create: `automation-tests/config/local.yaml`
- Create: `automation-tests/config/public.yaml`
- Create: `automation-tests/config/schema.yaml`
- Create: `automation-tests/framework/config/models.py`
- Create: `automation-tests/framework/config/loader.py`
- Create: `automation-tests/tests/framework/test_config_loader.py`

- [ ] **Step 1: Write failing loader tests**

```python
import pytest

from framework.config.loader import load_settings


def test_local_defaults(monkeypatch):
    monkeypatch.delenv("BOOKING_WEB_URL", raising=False)
    settings = load_settings("local")
    assert settings.web_url == "http://127.0.0.1:3000"
    assert settings.api_url == "http://127.0.0.1:8088/api"


def test_environment_overrides_yaml(monkeypatch):
    monkeypatch.setenv("BOOKING_API_URL", "http://example.test/api")
    assert load_settings("public").api_url == "http://example.test/api"


def test_unknown_environment_is_rejected():
    with pytest.raises(ValueError, match="unknown environment"):
        load_settings("production")
```

- [ ] **Step 2: Verify RED**

Run: `python -m pytest tests/framework/test_config_loader.py -q`

Expected: import failure because the configuration package does not exist.

- [ ] **Step 3: Add immutable model, YAML files, and loader**

`models.py` defines:

```python
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class Settings:
    name: str
    web_url: str
    api_url: str
    timeout_seconds: int
```

`loader.py` must resolve `config/<name>.yaml` relative to the automation root, validate it with `jsonschema`, strip trailing slashes, and apply `BOOKING_WEB_URL` and `BOOKING_API_URL` overrides. The YAML files contain only `name`, `web_url`, `api_url`, and `timeout_seconds`; they contain no credentials.

- [ ] **Step 4: Verify and commit**

Run: `python -m pytest tests/framework/test_config_loader.py -q`

Expected: 3 tests PASS.

Commit with message `feat: add validated local and public environments`.

### Task 6: Implement API client, data factory, and cleanup registry

**Files:**
- Create: `automation-tests/framework/api/client.py`
- Create: `automation-tests/framework/api/booking.py`
- Create: `automation-tests/framework/data/factory.py`
- Create: `automation-tests/framework/cleanup/registry.py`
- Create: `automation-tests/tests/framework/test_api_client.py`
- Create: `automation-tests/tests/framework/test_data_factory.py`
- Create: `automation-tests/tests/framework/test_cleanup_registry.py`

- [ ] **Step 1: Write focused failing tests**

```python
def test_factory_prefixes_every_created_user():
    user = TestDataFactory("a1b2c3d4").user()
    assert user["username"].startswith("pw-course-a1b2c3d4-")
    assert len(user["phone"]) == 11


def test_registry_sends_exact_registered_ids(fake_booking_api):
    registry = CleanupRegistry("a1b2c3d4", fake_booking_api)
    registry.add_user(7)
    registry.add_user(7)
    registry.cleanup()
    assert fake_booking_api.cleanup_payload["userIds"] == [7]


def test_client_rejects_non_result_envelope(requests_mock):
    requests_mock.get("http://example.test/api/tickets", json={"items": []})
    with pytest.raises(ApiContractError, match="Result envelope"):
        ApiClient("http://example.test/api").get("/tickets")
```

- [ ] **Step 2: Verify RED**

Run: `python -m pytest tests/framework/test_api_client.py tests/framework/test_data_factory.py tests/framework/test_cleanup_registry.py -q`

Expected: import failures for the new framework units.

- [ ] **Step 3: Implement minimal focused units**

`ApiClient` owns a `requests.Session`, timeout, Bearer header, status handling, and the `{code,message,data}` envelope. `BookingApi` provides `register_user`, `login_user`, `get_user`, `delete_test_user`, and `cleanup`. `TestDataFactory` generates prefix-owned user records. `CleanupRegistry` stores de-duplicated positive IDs and always sends all four ID lists.

The client must mask `Authorization`, `X-Course-Test-Key`, `password`, and `token` values before logging.

- [ ] **Step 4: Verify and commit**

Run: `python -m pytest tests/framework/test_api_client.py tests/framework/test_data_factory.py tests/framework/test_cleanup_registry.py -q`

Expected: PASS.

Commit with message `feat: add API data and cleanup foundation`.

### Task 7: Compose pytest lifecycle and browser token injection

**Files:**
- Create: `automation-tests/framework/auth/customer_session.py`
- Create: `automation-tests/framework/config/pytest_plugin.py`
- Create: `automation-tests/conftest.py`
- Create: `automation-tests/tests/framework/test_customer_session.py`
- Create: `automation-tests/tests/framework/test_write_guard.py`

- [ ] **Step 1: Write failing session payload test**

```python
def test_customer_session_matches_frontend_storage_contract():
    script, values = customer_init_script(
        token="secret-token", user_id=7, name="course-user", expires_in=3600
    )
    assert "booking_customer_token" in script
    assert values["token"] == "secret-token"
    assert values["userId"] == "7"
    assert values["expiresAt"] > 0
```

- [ ] **Step 2: Verify RED**

Run: `python -m pytest tests/framework/test_customer_session.py tests/framework/test_write_guard.py -q`

Expected: import failures.

- [ ] **Step 3: Implement CLI options and fixtures**

Register `--env`, `--allow-write`, and `--cleanup-audit`. Build session-scoped `settings` and `booking_api` fixtures. Build function-scoped `run_id`, `cleanup_registry`, and `temporary_customer` fixtures. `temporary_customer` must register, immediately register `userId` for cleanup, log in, yield the credentials, then call the single-user delete and verify `get_user` returns not found.

Build `authenticated_customer_context` by calling:

```python
context = browser.new_context(base_url=settings.web_url)
script, values = customer_init_script(
    token=temporary_customer.token,
    user_id=temporary_customer.user_id,
    name=temporary_customer.username,
    expires_in=temporary_customer.expires_in,
)
context.add_init_script(script=script, arg=values)
yield context
context.close()
```

Reject `@pytest.mark.write` tests unless `--allow-write` is present and `COURSE_TEST_KEY` is non-empty. Before the first public write test, send an empty batch cleanup request to verify endpoint availability and key validity.

- [ ] **Step 4: Verify framework and collection**

Run: `python -m pytest tests/framework -q`

Expected: all framework tests PASS.

Run: `python -m pytest --collect-only -q --env=local`

Expected: exit code 0 with no unknown marker or import errors.

- [ ] **Step 5: Commit**

Commit the plugin, fixtures, auth helper, and tests with message `feat: add temporary user lifecycle fixtures`.

### Task 8: Verify cleanup against the teaching environment

**Files:**
- Create: `automation-tests/tests/e2e/test_temporary_customer_lifecycle.py`
- Create: `automation-tests/.env.example`

- [ ] **Step 1: Add the lifecycle test**

```python
import pytest


@pytest.mark.e2e
@pytest.mark.api
@pytest.mark.write
@pytest.mark.public
def test_temporary_customer_is_authenticated(temporary_customer, booking_api):
    profile = booking_api.get_user(
        temporary_customer.user_id, token=temporary_customer.token
    )
    assert profile["id"] == temporary_customer.user_id
    assert profile["username"] == temporary_customer.username
```

- [ ] **Step 2: Document only variable names**

`.env.example` lists `BOOKING_ADMIN_USERNAME`, `BOOKING_ADMIN_PASSWORD`, `COURSE_TEST_KEY`, `DB_PASSWORD`, `JWT_SECRET`, and `PAYMENT_CALLBACK_SECRET_KEY` with empty values. It contains no real credential.

- [ ] **Step 3: Deploy the backend change to the teaching environment through the existing manual process for this phase**

Set `COURSE_TEST_SUPPORT_ENABLED=true` and supply a rotated `COURSE_TEST_KEY` outside the repository. Do not use a key previously disclosed in chat or source.

- [ ] **Step 4: Run and audit**

Run from `automation-tests` with secrets already present in the process environment:

`python -m pytest tests/e2e/test_temporary_customer_lifecycle.py --env=public --allow-write -v`

Expected: test PASS and teardown reports one deleted test user.

Run the same command a second time.

Expected: PASS again with a different runId and no user left from the first run.

- [ ] **Step 5: Commit**

Commit with message `test: verify public temporary user teardown`.

### Task 9: Phase verification

- [ ] Run `mvn.cmd -pl booking-api -Dtest=CourseTestSupportControllerTest,CourseTestCleanupServiceTest,ApplicationDevSecretSourceTest test` and record PASS counts.
- [ ] Run `python -m pytest tests/framework -q` and record PASS counts.
- [ ] Run `python -m pytest --collect-only -q --env=local` and verify exit code 0.
- [ ] Run the public lifecycle test with `--allow-write`, then query the exact username and verify it is absent.
- [ ] Run `git diff --check` and a credential-pattern scan over files planned for commit.
- [ ] Update `automation-tests/README.md` with exact local/public lifecycle commands and commit as `docs: explain safe Playwright test lifecycle`.
