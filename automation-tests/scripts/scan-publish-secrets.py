"""Bounded tracked-source credential gate; outputs locations/rules, never values.

Scans the working copies of tracked text files, not Git history, ignored files,
untracked files, binary assets, or arbitrary high-entropy strings. Review history
and the exact publication manifest separately before publishing.
"""
from pathlib import Path
import re
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]
RULES = {
    "private-key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "access-token": re.compile(r"gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|AKIA[0-9A-Z]{16}|eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"),
    "sensitive-fallback": re.compile(r"\$\{(?:DB_PASSWORD|JWT_SECRET|PAYMENT_CALLBACK_SECRET_KEY|COURSE_TEST_KEY|BOOKING_ADMIN_PASSWORD):[^}]+\}"),
    "python-sensitive-fallback": re.compile(r'''os\.(?:getenv|environ\.get)\(["'](?:BOOKING_ADMIN_PASSWORD|COURSE_TEST_KEY|JWT_SECRET)["']\s*,\s*["'][^"']+["']'''),
}
SENSITIVE_DEFAULT = re.compile(r'''(?:DEFAULT_ADMIN_PASSWORD|TEST_USER_PASSWORD)\s*=\s*["']([^"']*)["']''')
SMOKE_PASSWORD = re.compile(r'''(?i)\bpassword\s*=\s*["']([^"']+)["']''')
ARCHIVE_PLACEHOLDER = "<ARCHIVED_PASSWORD_UNSET>"


def findings(path, content):
    for number, line in enumerate(content.splitlines(), 1):
        for rule, pattern in RULES.items():
            if pattern.search(line):
                yield f"{path}:{number}:{rule}"
        if path.endswith("AdminAccountInitializer.java") or path.endswith("legacy/api-tests-v1/config.py"):
            for match in SENSITIVE_DEFAULT.finditer(line):
                if match.group(1) != ARCHIVE_PLACEHOLDER:
                    yield f"{path}:{number}:credential-default"
        if path == "scripts/full-smoke-test.ps1" and SMOKE_PASSWORD.search(line):
            yield f"{path}:{number}:smoke-credential-literal"


def main():
    result = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, capture_output=True, check=True)
    scanned = 0
    violations = []
    for raw in result.stdout.split(b"\0"):
        if not raw:
            continue
        path = raw.decode("utf-8")
        file = ROOT / path
        if not file.is_file():
            continue
        content = file.read_bytes()
        if b"\0" in content:
            continue
        try:
            text = content.decode("utf-8-sig")
        except UnicodeDecodeError:
            continue
        scanned += 1
        violations.extend(findings(path, text))
    for violation in violations:
        print(violation)
    print(f"Tracked UTF-8 text files scanned: {scanned}; findings: {len(violations)}")
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())
