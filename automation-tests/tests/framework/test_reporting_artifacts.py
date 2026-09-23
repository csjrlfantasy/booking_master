"""Offline reporting contracts; no service or browser is started."""

import importlib
import json
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock

import pytest


ROOT = Path(__file__).resolve().parents[2]


def reporting():
    assert (ROOT / "framework/reporting/artifacts.py").exists(), "reporting implementation missing"
    return importlib.import_module("framework.reporting.artifacts")


def test_recursive_redaction_removes_credentials_identity_and_storage(monkeypatch):
    module = reporting()
    monkeypatch.setenv("COURSE_TEST_KEY", "synthetic-course-value")
    payload = {"request": {"Authorization": "Bearer abc", "Cookie": "abc", "PASSWORD": "abc"},
               "response": [{"accessToken": "abc", "idCardNo": "000000200001010001",
                             "contactPhone": "19000000000", "email": "teacher@example.test",
                             "orderId": 7}], "storage_state": {"origins": []},
               "note": "synthetic-course-value", "nested": ({"refresh_token": "abc"},)}
    safe = module.redact(payload)
    dumped = json.dumps(safe)
    for secret in ("Bearer abc", "synthetic-course-value", "000000200001010001", "19000000000", "teacher@example.test"):
        assert secret not in dumped
    assert safe["response"][0]["orderId"] == 7
    assert safe["storage_state"] == module.REDACTED
    assert payload["request"]["PASSWORD"] == "abc"


def test_untrusted_text_is_omitted_and_safe_business_evidence_is_preserved():
    module = reporting()
    safe = module.redact({"message": "Bearer unknown-dynamic-token", "orderStatus": 2,
                          "localStorage": [{"name": "auth-storage", "value": "secret"}],
                          "custom": object()})
    assert "unknown-dynamic-token" not in json.dumps(safe)
    assert safe["orderStatus"] == 2
    assert safe["localStorage"] == module.REDACTED


def test_output_paths_are_isolated_and_names_cannot_escape(tmp_path):
    module = reporting()
    a, b = module.create_run_dir(tmp_path, "local"), module.create_run_dir(tmp_path, "local")
    assert a != b and a.is_dir() and b.is_dir()
    name = module.artifact_name("../../tests/test.py::test[secret/../x]", "chromium")
    assert "/" not in name and "\\" not in name and ".." not in name
    assert name != module.artifact_name("other::test", "chromium")
    with pytest.raises(ValueError):
        module.create_run_dir(tmp_path, "../public")


@pytest.mark.parametrize("when,outcome,xfail,expected", [
    ("call", "passed", None, "passed"), ("call", "failed", None, "failed"),
    ("setup", "failed", None, "error"), ("teardown", "failed", None, "error"),
    ("setup", "skipped", None, "skipped"), ("call", "skipped", "reason", "xfail"),
    ("call", "passed", "reason", "xpass"),
])
def test_status_vocabulary(when, outcome, xfail, expected):
    report = SimpleNamespace(when=when, outcome=outcome)
    if xfail:
        report.wasxfail = xfail
    assert reporting().report_status(report) == expected


def test_capture_failure_does_not_hide_test_failure(tmp_path):
    module = reporting()
    context = MagicMock()
    context.pages = [MagicMock()]
    context.pages[0].screenshot.side_effect = RuntimeError("secret screenshot failure")
    context.tracing.stop.side_effect = RuntimeError("secret trace failure")
    capture = module.BrowserArtifacts(tmp_path, "case", "chromium")
    capture.before_close(context)
    assert {x["operation"] for x in capture.diagnostics} >= {"screenshot", "trace"}
    assert "secret" not in json.dumps(capture.diagnostics)


def test_success_artifacts_removed_but_failed_artifacts_retained(tmp_path):
    module = reporting()
    success = module.BrowserArtifacts(tmp_path, "passed", "chromium")
    success.directory.mkdir(parents=True)
    (success.directory / "owned.zip").write_bytes(b"trace")
    unrelated = tmp_path / "unrelated.zip"
    unrelated.write_bytes(b"keep")
    success.finish(failed=False)
    assert not success.directory.exists() and unrelated.exists()
    failed = module.BrowserArtifacts(tmp_path, "failed", "chromium")
    failed.directory.mkdir(parents=True)
    (failed.directory / "trace.zip").write_bytes(b"trace")
    failed.finish(failed=True)
    assert (failed.directory / "trace.zip").exists()


def test_pytest_records_failure_and_teardown_error_without_false_pass(tmp_path):
    module = reporting()
    suite = tmp_path / "test_outcomes.py"
    suite.write_text('''import pytest
@pytest.fixture
def cleanup():
    yield
    raise RuntimeError("cleanup failed")
def test_both(cleanup):
    assert False, "business failed"
def test_ok(): pass
@pytest.mark.skip(reason="pending")
def test_skip(): pass
@pytest.mark.xfail(reason="known")
def test_xfail(): assert False
@pytest.mark.xfail(reason="known")
def test_xpass(): pass
''', encoding="utf-8")
    output = tmp_path / "evidence"
    result = subprocess.run([sys.executable, "-m", "pytest", str(suite), "-p", module.__name__,
                             "--confcutdir", str(tmp_path), "-o", "addopts=", "--evidence-dir", str(output)],
                            cwd=ROOT, text=True, capture_output=True, timeout=30)
    assert result.returncode == 1, result.stdout + result.stderr
    summary = json.loads((output / "summary.json").read_text(encoding="utf-8"))
    assert summary["planned"] is None
    assert summary["collected"] == 5 and summary["attempted"] == 5 and summary["executed"] == 4
    assert summary["statuses"] == {"passed": 1, "failed": 1, "error": 1, "skipped": 1, "xfail": 1, "xpass": 1}
    both = [r for r in summary["reports"] if "test_both" in r["nodeid"]]
    assert {r["status"] for r in both} >= {"failed", "error"}


def test_script_help_and_dry_run_do_not_execute_tests_or_require_secrets(tmp_path):
    reporting()
    script = ROOT / "scripts/run-suite.ps1"
    assert script.exists()
    for suite in ("framework", "api-read", "api-write", "customer-ui", "admin-ui", "smoke", "e2e", "full"):
        result = subprocess.run(["pwsh", "-NoProfile", "-File", str(script), "-Suite", suite,
                                 "-Environment", "public", "-DryRun", "-OutputRoot", str(tmp_path)],
                                capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30)
        assert result.returncode == 0, result.stdout + result.stderr
        assert "--collect-only" not in result.stdout
        if suite in {"e2e", "full"}:
            assert '"-n","0"' in result.stdout.replace(" ", "")
        if suite == "smoke":
            assert all(browser in result.stdout for browser in ("chromium", "firefox", "webkit"))
    assert not list(tmp_path.iterdir())


def test_scripts_fail_closed_and_have_no_dynamic_command_evaluation():
    reporting()
    for name in ("run-suite.ps1", "generate-report.ps1"):
        script = ROOT / "scripts" / name
        assert script.exists()
        assert "Invoke-Expression" not in script.read_text(encoding="utf-8")
    result = subprocess.run(["pwsh", "-NoProfile", "-File", str(ROOT / "scripts/run-suite.ps1"),
                             "-Suite", "e2e"], capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30)
    assert result.returncode != 0
    assert "AllowWrite" in result.stderr


def test_trace_export_omits_auth_payloads_network_bodies_sources_and_storage(tmp_path):
    module = reporting()
    raw, clean = tmp_path / "raw.zip", tmp_path / "sanitized-action-trace.json"
    events = [
        {"type": "context-options", "version": 8, "browserName": "chromium", "options": {"storageState": "secret"}},
        {"type": "before", "callId": "call@7", "apiName": "browserContext.addInitScript", "startTime": 2,
         "params": {"script": "localStorage.setItem('token', 'dynamic-secret')"}},
        {"type": "after", "callId": "call@7", "endTime": 3, "result": {"token": "dynamic-secret"}},
        {"type": "event", "method": "console", "params": {"text": "dynamic-secret"}},
    ]
    with zipfile.ZipFile(raw, "w") as archive:
        archive.writestr("trace.trace", "\n".join(json.dumps(event) for event in events))
        archive.writestr("trace.network", "Authorization: dynamic-secret")
        archive.writestr("resources/source.txt", "dynamic-secret")
    module.sanitize_action_trace(raw, clean)
    text = clean.read_text(encoding="utf-8")
    assert "dynamic-secret" not in text and "storageState" not in text
    assert '"callId": "call@7"' in text
    assert not any(key in text for key in ('"params"', '"result"', '"network"', '"resources"'))


def test_capture_exports_only_action_summary_and_deletes_raw_trace(tmp_path):
    module = reporting()
    capture = module.BrowserArtifacts(tmp_path, "case", "chromium")
    context = MagicMock()
    context.pages = []
    def stop(*, path):
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr("trace.trace", json.dumps({"type": "before", "callId": "call@1", "params": {"token": "secret"}}))
    context.tracing.stop.side_effect = stop
    capture.before_close(context)
    capture.finish(failed=True)
    assert not list(capture.directory.rglob("*.zip"))
    summaries = list(capture.directory.glob("*-sanitized-action-trace.json"))
    assert len(summaries) == 1 and "secret" not in summaries[0].read_text(encoding="utf-8")


def test_browser_context_hook_captures_before_close_and_preserves_close_error(tmp_path, monkeypatch):
    module = reporting()
    events = []
    context = MagicMock()
    context.pages = []
    context.close.side_effect = lambda: events.append("close")
    browser = SimpleNamespace(new_context=lambda **kwargs: context)
    capture = module.BrowserArtifacts(tmp_path, "case", "chromium")
    monkeypatch.setattr(capture, "before_close", lambda ctx: events.append("capture"))
    capture.install(browser, monkeypatch)
    created = browser.new_context(base_url="https://example.test")
    created.close()
    assert events == ["capture", "close"]
    context.tracing.start.assert_called_once_with(screenshots=False, snapshots=False, sources=False)


def test_collection_evidence_has_zero_execution(tmp_path):
    module = reporting()
    suite = tmp_path / "test_pending.py"
    suite.write_text("def test_pending(): raise AssertionError('must not execute')", encoding="utf-8")
    output = tmp_path / "collected"
    result = subprocess.run([sys.executable, "-m", "pytest", str(suite), "-p", module.__name__,
                             "--confcutdir", str(tmp_path), "-o", "addopts=", "--collect-only",
                             "--evidence-dir", str(output)], cwd=ROOT, capture_output=True, timeout=30)
    assert result.returncode == 0
    summary = json.loads((output / "summary.json").read_text(encoding="utf-8"))
    assert summary["collectionOnly"] and summary["collected"] == 1
    assert summary["attempted"] == summary["executed"] == sum(summary["statuses"].values()) == 0


def test_run_suite_propagates_exit_code_with_literal_path_arguments(tmp_path):
    fake_python = tmp_path / "fake python.ps1"
    fake_python.write_text("exit 7\n", encoding="utf-8")
    result = subprocess.run(["pwsh", "-NoProfile", "-File", str(ROOT / "scripts/run-suite.ps1"),
                             "-Python", str(fake_python), "-Suite", "framework",
                             "-OutputRoot", str(tmp_path / "space's;literal$(value)")],
                            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30)
    assert result.returncode == 7, result.stdout + result.stderr
    assert "literal$(value)" in result.stdout


def test_generate_report_reports_missing_cli_and_does_not_claim_html(tmp_path):
    results = tmp_path / "allure-results"
    results.mkdir()
    (results / "synthetic-result.json").write_text("{}", encoding="utf-8")
    pwsh = shutil.which("pwsh")
    result = subprocess.run([pwsh, "-NoProfile", "-File", str(ROOT / "scripts/generate-report.ps1"),
                             "-ResultsDirectory", str(results)],
                            env={**os.environ, "PATH": ""}, capture_output=True, text=True,
                            encoding="utf-8", errors="replace", timeout=30)
    assert result.returncode != 0
    assert "Allure CLI is missing" in result.stderr
    assert "Generated Allure report:" not in result.stdout


def test_root_cleanup_records_error_and_preserves_original_failure(monkeypatch):
    import conftest as shared
    monkeypatch.setenv("COURSE_TEST_KEY", "synthetic-key")
    evidence = MagicMock()
    monkeypatch.setattr(shared, "attach_json", evidence)
    api = MagicMock()
    api.cleanup.side_effect = RuntimeError("synthetic cleanup failure")
    fixture = shared.cleanup_registry.__wrapped__("a1b2c3d4", api, MagicMock())
    registry = next(fixture)
    registry.add_user(7)
    with pytest.raises(RuntimeError, match="synthetic cleanup failure"):
        next(fixture)
    assert evidence.call_args_list[0].args[1]["resources"]["user"] == [7]
    assert "ERROR" in evidence.call_args_list[-1].args[0]


def test_root_cleanup_rejected_resources_fail_the_teardown():
    import conftest as shared
    report = shared.cleanup_reporter.__wrapped__()
    with pytest.raises(AssertionError, match="rejected"):
        report({"deleted": {}, "notFound": {}, "rejected": {"orders": 1}})


def test_root_autouse_hook_integrates_context_retention_api_redaction_and_metadata(tmp_path):
    # Execute actual root fixtures using a fake browser; this is not live UI evidence.
    (tmp_path / "conftest.py").write_text((ROOT / "conftest.py").read_text(encoding="utf-8"), encoding="utf-8")
    (tmp_path / "test_hook.py").write_text('''import json
import zipfile
from pathlib import Path
from types import SimpleNamespace
import pytest

pytestmark = pytest.mark.ui

@pytest.fixture
def browser_name(): return "chromium"

@pytest.fixture
def browser():
    def new_context(**kwargs):
        def stop(*, path):
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("trace.trace", json.dumps({"type": "before", "callId": "call@1", "params": {"token": "dynamic-secret"}}))
        def close():
            path = Path(kwargs["record_video_dir"])
            path.mkdir(parents=True, exist_ok=True)
            (path / "fake.webm").write_bytes(b"synthetic video")
        page = SimpleNamespace(locator=lambda text: object(), screenshot=lambda **kw: Path(kw["path"]).write_bytes(b"synthetic png"))
        return SimpleNamespace(pages=[page], close=close, tracing=SimpleNamespace(start=lambda **kw: None, stop=stop))
    return SimpleNamespace(new_context=new_context)

@pytest.fixture(scope="session")
def api_client():
    return SimpleNamespace(request=lambda *args, **kwargs: {"orderId": 7, "token": "dynamic-secret", "phone": "19000000000"})

@pytest.fixture
def context(browser):
    context = browser.new_context()
    yield context
    context.close()

@pytest.fixture
def broken_cleanup():
    yield
    raise RuntimeError("cleanup failed")

def test_ok(context, api_client, run_id):
    assert api_client.request("GET", "/orders/7", json={"password": "dynamic-secret"})["orderId"] == 7

def test_fail(context, api_client, run_id):
    api_client.request("GET", "/orders/7", json={"password": "dynamic-secret"})
    assert False, "business failed"

def test_teardown(context, broken_cleanup): pass
''', encoding="utf-8")
    output = tmp_path / "output"
    result = subprocess.run([sys.executable, "-m", "pytest", str(tmp_path / "test_hook.py"),
                             "--confcutdir", str(tmp_path), "-o", "addopts=", "--evidence-dir", str(output)],
                            cwd=ROOT, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=30)
    assert result.returncode == 1, result.stdout + result.stderr
    directories = list((output / "browser").iterdir())
    assert len(directories) == 2
    assert all(list(directory.glob("*.png")) and list(directory.rglob("*.webm")) for directory in directories)
    assert not list(output.rglob("*.zip"))
    attachments = list((output / "allure-results").glob("*-attachment.json"))
    assert attachments
    for path in attachments:
        content = path.read_text(encoding="utf-8")
        assert "dynamic-secret" not in content and "19000000000" not in content
    junit = (output / "junit.xml").read_text(encoding="utf-8")
    assert "booking.metadata" in junit and "chromium" in junit
    summary = json.loads((output / "summary.json").read_text(encoding="utf-8"))
    assert summary["statuses"]["failed"] == summary["statuses"]["error"] == 1
