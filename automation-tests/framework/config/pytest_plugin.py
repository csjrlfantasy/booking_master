"""Collection-time safety gates for course automation tests."""

from __future__ import annotations

import os
from typing import Any

import pytest

from framework.config.loader import load_settings


def pytest_addoption(parser: pytest.Parser) -> None:
    """Register environment and write-safety options."""
    group = parser.getgroup("booking automation")
    group.addoption(
        "--env",
        action="store",
        choices=("local", "public"),
        default="local",
        help="target environment (default: local)",
    )
    group.addoption(
        "--allow-write",
        action="store_true",
        default=False,
        help="allow tests marked write to execute",
    )
    group.addoption(
        "--allow-destructive-commerce",
        action="store_true",
        default=False,
        help=(
            "allow destructive_commerce tests to execute in an isolated "
            "environment after --allow-write"
        ),
    )
    group.addoption(
        "--cleanup-audit",
        action="store_true",
        default=False,
        help="enable cleanup audit diagnostics",
    )


def pytest_configure(config: pytest.Config) -> None:
    """Load non-secret settings before tests are collected."""
    config.addinivalue_line("markers", "write: test creates, mutates, or deletes data")
    config.addinivalue_line(
        "markers",
        "destructive_commerce: irreversible shared inventory or coupon consumption",
    )
    config._booking_settings = load_settings(config.getoption("--env"))
    config._booking_selected_write_tests = False
    config._booking_selected_destructive_commerce_tests = False


def pytest_collection_modifyitems(config: pytest.Config, items: list[pytest.Item]) -> None:
    """Fail closed before execution whenever selected tests can write data."""
    selected_destructive_commerce_tests = any(
        item.get_closest_marker("destructive_commerce") is not None for item in items
    )
    selected_write_tests = selected_destructive_commerce_tests or any(
        item.get_closest_marker("write") is not None for item in items
    )
    config._booking_selected_write_tests = selected_write_tests
    config._booking_selected_destructive_commerce_tests = (
        selected_destructive_commerce_tests
    )
    if not selected_write_tests:
        return
    if config.option.collectonly:
        return

    if not config.getoption("--allow-write"):
        raise pytest.UsageError("write tests require explicit --allow-write")

    if not _course_test_key_present():
        raise pytest.UsageError("write tests require a non-empty COURSE_TEST_KEY")

    if (
        selected_destructive_commerce_tests
        and not config.getoption("--allow-destructive-commerce")
    ):
        raise pytest.UsageError(
            "destructive commerce tests require explicit "
            "--allow-destructive-commerce after --allow-write"
        )


def _course_test_key_present() -> bool:
    value: Any = os.environ.get("COURSE_TEST_KEY")
    return isinstance(value, str) and bool(value.strip())
