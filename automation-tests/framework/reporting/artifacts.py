"""Evidence helpers and pytest hooks. Never serialize browser authentication state."""

from __future__ import annotations

import hashlib
import json
import re
import shutil
import uuid
import warnings
import tempfile
import zipfile
from collections.abc import Mapping
from datetime import datetime, timezone
from pathlib import Path

import pytest


REDACTED = "[REDACTED]"
STATUSES = ("passed", "failed", "error", "skipped", "xfail", "xpass")
_PRIVATE = re.compile(r"password|passwd|secret|token|authorization|cookie|coursekey|coursetestkey|"
                      r"phone|mobile|email|idcard|identitynumber|realname|contactname|"
                      r"storage|credential|nickname|username|address", re.I)
_STATES = {"NONE", "PROCESSING", "SUCCESS", "FAILED", "PENDING", "CLOSED", "CANCELLED"}


def redact(value):
    """Keep structural/numeric evidence; discard arbitrary text and private fields.

    Opaque response messages can contain unlabelled credentials. A string allowlist
    is deliberately stricter than replacing known passwords after serialization.
    """
    if isinstance(value, Mapping):
        result = {}
        for key, item in value.items():
            key = str(key)
            if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_-]{0,63}", key):
                continue
            normalized = re.sub(r"[^a-z0-9]", "", key.lower())
            if key == "runId" and isinstance(item, str) and re.fullmatch(r"[a-z0-9]{8,32}", item):
                result[key] = item
            else:
                result[key] = REDACTED if _PRIVATE.search(normalized) else redact(item)
        return result
    if isinstance(value, (list, tuple)):
        return [redact(item) for item in value]
    if value is None or type(value) in (int, float, bool):
        return value
    if isinstance(value, str) and value in _STATES:
        return value
    return REDACTED


def create_run_dir(root: Path, environment: str) -> Path:
    if environment not in {"local", "public"}:
        raise ValueError("unsupported report environment")
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    directory = Path(root) / f"{stamp}-{environment}-{uuid.uuid4().hex[:12]}"
    directory.mkdir(parents=True, exist_ok=False)
    return directory


def artifact_name(nodeid: str, browser: str) -> str:
    digest = hashlib.sha256(nodeid.encode("utf-8")).hexdigest()[:20]
    browser = browser if browser in {"chromium", "firefox", "webkit"} else "none"
    return f"test-{digest}-{browser}"


def attach_json(name: str, value) -> None:
    """Only call with code-owned attachment names; payload is always redacted."""
    try:
        import allure
        allure.attach(json.dumps(redact(value), ensure_ascii=False, allow_nan=False),
                      name=name, attachment_type=allure.attachment_type.JSON)
    except Exception:
        # Reporting transport must not turn an existing assertion into a new error.
        warnings.warn("Could not attach redacted JSON evidence", pytest.PytestWarning)


def report_status(report) -> str:
    if getattr(report, "wasxfail", None):
        return "xfail" if report.outcome == "skipped" else "xpass"
    if report.outcome == "failed" and str(getattr(report, "longrepr", "")).startswith("[XPASS(strict)]"):
        return "xpass"
    if report.outcome == "failed" and report.when != "call":
        return "error"
    return report.outcome


def sanitize_action_trace(raw: Path, destination: Path) -> None:
    """Export a limited JSON action timeline, NOT a playable Playwright trace.

    Keep only protocol identifiers/timings. Drop params, errors, result, options,
    logs, stacks, snapshots, network, sources and resources wholesale.
    """
    numeric = {"version", "startTime", "endTime", "wallTime", "monotonicTime"}
    identifiers = {"type", "callId", "parentId", "apiName", "method", "class", "pageId", "browserName"}
    events = []
    with zipfile.ZipFile(raw) as archive:
        for name in archive.namelist():
            if not name.endswith(".trace"):
                continue
            for line in archive.read(name).decode("utf-8").splitlines():
                event = json.loads(line)
                safe = {key: value for key, value in event.items()
                        if (key in numeric and type(value) in (int, float))
                        or (key in identifiers and isinstance(value, str)
                            and re.fullmatch(r"[A-Za-z0-9_.@:-]{1,100}", value))}
                events.append(safe)
    destination.write_text(json.dumps({"format": "sanitized-action-timeline-v1", "events": events}, indent=2),
                           encoding="utf-8")


class BrowserArtifacts:
    """Capture before fixture context.close, decide retention after all teardown.

    Screenshots/videos are restricted artifacts; trace export is a JSON timeline.
    Raw trace uses a temporary directory outside archived evidence and is deleted.
    """

    def __init__(self, root: Path, nodeid: str, browser: str):
        self.browser_name = browser
        self.directory = Path(root) / "browser" / f"{artifact_name(nodeid, browser)}-{uuid.uuid4().hex[:8]}"
        self.diagnostics = []
        self._context_count = 0

    def diagnostic(self, operation: str, error: Exception) -> None:
        self.diagnostics.append({"operation": operation, "errorType": type(error).__name__})

    def install(self, browser, monkeypatch) -> None:
        original_new = browser.new_context

        def new_context(*args, **kwargs):
            self.directory.mkdir(parents=True, exist_ok=True)
            kwargs.setdefault("record_video_dir", str(self.directory / "video"))
            context = original_new(*args, **kwargs)
            try:
                context.tracing.start(screenshots=False, snapshots=False, sources=False)
            except Exception as error:
                self.diagnostic("trace-start", error)
            original_close = context.close
            closed = False

            def close(*close_args, **close_kwargs):
                nonlocal closed
                if not closed:
                    closed = True
                    self.before_close(context)
                return original_close(*close_args, **close_kwargs)

            monkeypatch.setattr(context, "close", close)
            return context

        monkeypatch.setattr(browser, "new_context", new_context)

    def before_close(self, context) -> None:
        self._context_count += 1
        index = self._context_count
        try:
            self.directory.mkdir(parents=True, exist_ok=True)
            pages = context.pages
        except Exception as error:
            self.diagnostic("context-pages", error)
            pages = []
        for page_index, page in enumerate(pages):
            try:
                # Mask form values; surrounding UI may still contain teaching PII.
                page.screenshot(path=str(self.directory / f"context-{index}-page-{page_index}.png"),
                                full_page=True, mask=[page.locator("input, textarea")], timeout=5000)
            except Exception as error:
                self.diagnostic("screenshot", error)
        try:
            with tempfile.TemporaryDirectory(prefix="booking-trace-") as temporary:
                raw = Path(temporary) / "raw.zip"
                context.tracing.stop(path=str(raw))
                sanitize_action_trace(raw, self.directory / f"context-{index}-sanitized-action-trace.json")
        except Exception as error:
            self.diagnostic("trace", error)

    def finish(self, *, failed: bool) -> None:
        try:
            if not failed and self.directory.is_dir():
                shutil.rmtree(self.directory)
            elif failed:
                self.directory.mkdir(parents=True, exist_ok=True)
                (self.directory / "capture-diagnostics.json").write_text(
                    json.dumps(self.diagnostics, indent=2), encoding="utf-8")
        except Exception as error:
            self.diagnostic("retention", error)
            warnings.warn("Browser artifact retention failed", pytest.PytestWarning)


def pytest_addoption(parser):
    parser.getgroup("booking evidence").addoption(
        "--evidence-dir", default=None, help="empty directory for this run's isolated evidence")


@pytest.hookimpl(tryfirst=True)
def pytest_configure(config):
    environment = config.getoption("env", default="local")
    requested = config.getoption("evidence_dir")
    worker = getattr(config, "workerinput", {}).get("workerid")
    if requested:
        root = Path(requested).resolve()
        if worker:
            root = root / worker
        if root.exists() and any(root.iterdir()):
            raise pytest.UsageError("--evidence-dir must be empty; use a new run directory")
        root.mkdir(parents=True, exist_ok=True)
    else:
        root = create_run_dir(Path(config.rootpath) / "report", environment)
    config._booking_artifact_root = root
    if not config.option.collectonly:
        # Preserve explicit caller paths. The suite script always supplies isolated paths.
        for option, target in (("xmlpath", "junit.xml"), ("htmlpath", "pytest.html"),
                               ("allure_report_dir", "allure-results")):
            if hasattr(config.option, option) and not getattr(config.option, option):
                setattr(config.option, option, str(root / target))
    plugin = EvidencePlugin(config, root, environment)
    config.pluginmanager.register(plugin, "booking-evidence-recorder")


class EvidencePlugin:
    def __init__(self, config, root, environment):
        self.config, self.root, self.environment = config, root, environment
        self.reports = []
        self.collected = 0

    def pytest_collection_finish(self, session):
        self.collected = len(session.items)

    @pytest.hookimpl(optionalhook=True)
    def pytest_xdist_node_collection_finished(self, node, ids):
        self.collected = max(self.collected, len(ids))

    def pytest_runtest_logreport(self, report):
        self.reports.append({"nodeid": report.nodeid, "phase": report.when,
                             "status": report_status(report), "duration": report.duration,
                             "worker": getattr(report, "worker_id", "main")})

    @pytest.hookimpl(hookwrapper=True)
    def pytest_runtest_makereport(self, item, call):
        outcome = yield
        report = outcome.get_result()
        if report.failed:
            item._booking_failed = True
        # Preserve errors raised by teardown alongside an earlier failed call.
        capture = getattr(item, "_booking_capture", None)
        if report.when == "teardown" and capture:
            capture.finish(failed=getattr(item, "_booking_failed", False))
        if report.when == "setup":
            browser = item.funcargs.get("browser_name", capture.browser_name if capture else "none")
            run_id = item.funcargs.get("run_id", "not-created")
            metadata = {"environment": self.environment, "runId": run_id, "browser": browser,
                        "nodeid": item.nodeid, "cleanup": "pending"}
            item.user_properties.append(("booking.metadata", json.dumps(metadata)))
            try:
                import allure
                for key, value in metadata.items():
                    allure.dynamic.label(key, str(value))
            except Exception:
                pass

    def pytest_sessionfinish(self, session, exitstatus):
        counts = {status: 0 for status in STATUSES}
        for report in self.reports:
            if report["phase"] == "call" or report["status"] != "passed":
                counts[report["status"]] += 1
        summary = {
            "runId": self.root.name, "environment": self.environment,
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "planned": None, "collected": self.collected,
            "attempted": sum(r["phase"] == "setup" for r in self.reports),
            "executed": sum(r["phase"] == "call" for r in self.reports),
            "statuses": counts, "exitCode": int(exitstatus),
            "collectionOnly": bool(self.config.option.collectonly),
            "reports": self.reports,
            "definitions": {
                "planned": "Unknown; requires an external test plan, never inferred from collection",
                "collected": "Selected parameterized nodeids before execution",
                "attempted": "Setup phase reports, including repeated attempts",
                "executed": "Call phase reports; includes xfail and xpass",
                "statuses": "Call outcomes plus non-passed setup/teardown; may exceed unique cases",
            },
        }
        (self.root / "summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
        (self.root / "environment.properties").write_text(
            f"environment={self.environment}\nrunId={self.root.name}\n", encoding="utf-8")
        allure_dir = getattr(self.config.option, "allure_report_dir", None)
        if allure_dir and not self.config.option.collectonly:
            Path(allure_dir).mkdir(parents=True, exist_ok=True)
            shutil.copyfile(self.root / "environment.properties", Path(allure_dir) / "environment.properties")
