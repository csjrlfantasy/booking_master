"""Exercise bounded publication rules without storing real credential examples."""
import importlib.util
from pathlib import Path
import subprocess
import sys

import pytest


SCRIPT = Path(__file__).resolve().parents[2] / "scripts" / "scan-publish-secrets.py"
spec = importlib.util.spec_from_file_location("publish_secret_scan", SCRIPT)
scanner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(scanner)


@pytest.mark.parametrize("path,content,rule", [
    ("doc.md", "-----BEGIN " + "PRIVATE KEY-----", "private-key"),
    ("doc.md", "ghp_" + "A" * 30, "access-token"),
    ("doc.md", "eyJ" + "A" * 12 + "." + "B" * 12 + "." + "C" * 12, "access-token"),
    ("config.yml", "${" + "COURSE_TEST_KEY:sample-unsafe-value}", "sensitive-fallback"),
    ("doc.md", 'os.getenv(' + '"BOOKING_ADMIN_PASSWORD", "unsafe-sample")', "python-sensitive-fallback"),
    ("config/AdminAccountInitializer.java", 'DEFAULT_ADMIN_PASSWORD' + ' = "synthetic-input";', "credential-default"),
    ("automation-tests/legacy/api-tests-v1/config.py", 'TEST_USER_PASSWORD' + ' = "synthetic-input"', "credential-default"),
    ("scripts/full-smoke-test.ps1", 'password' + ' = "synthetic-input"', "smoke-credential-literal"),
])
def test_rules_report_only_location_and_rule(path, content, rule):
    matches = list(scanner.findings(path, content))
    assert matches == [f"{path}:1:{rule}"]


def test_environment_and_archival_placeholders_are_allowed():
    for path, content in [
        ("config/AdminAccountInitializer.java", '@Value("${BOOKING_ADMIN_PASSWORD:}")'),
        ("config.yml", "key: ${COURSE_TEST_KEY}"),
        ("scripts/full-smoke-test.ps1", "password = $env:BOOKING_ADMIN_PASSWORD"),
        ("automation-tests/legacy/api-tests-v1/config.py", 'TEST_USER_PASSWORD = "<ARCHIVED_PASSWORD_UNSET>"'),
    ]:
        assert list(scanner.findings(path, content)) == []


def test_tracked_working_sources_pass_bounded_scan():
    result = subprocess.run([sys.executable, str(SCRIPT)], capture_output=True, text=True)
    assert result.returncode == 0, result.stdout
    assert "findings: 0" in result.stdout
