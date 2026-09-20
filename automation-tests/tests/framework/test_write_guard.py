import os
from pathlib import Path


pytest_plugins = ("pytester",)


def _enable_plugin(pytester, monkeypatch):
    source_root = str(Path(__file__).resolve().parents[2])
    existing_path = os.environ.get("PYTHONPATH", "")
    monkeypatch.setenv("PYTHONPATH", os.pathsep.join(filter(None, (source_root, existing_path))))
    pytester.makeconftest(
        'pytest_plugins = ("framework.config.pytest_plugin",)\n'
    )


def _make_write_test(pytester):
    pytester.makepyfile(
        """
        import pytest

        @pytest.mark.write
        def test_mutates_data():
            assert True
        """
    )


def test_read_only_collection_succeeds_without_flags(pytester, monkeypatch):
    _enable_plugin(pytester, monkeypatch)
    pytester.makepyfile("def test_read_only(): assert True")

    result = pytester.runpytest_subprocess("--collect-only", "-q")

    assert result.ret == 0
    result.stdout.fnmatch_lines(["*test_read_only*"])


def test_selected_write_test_requires_explicit_allow_write(pytester, monkeypatch):
    monkeypatch.delenv("COURSE_TEST_KEY", raising=False)
    _enable_plugin(pytester, monkeypatch)
    _make_write_test(pytester)

    result = pytester.runpytest_subprocess("-q")

    assert result.ret != 0
    assert "--allow-write" in result.stderr.str()


def test_selected_write_test_requires_non_empty_course_key(pytester, monkeypatch):
    monkeypatch.setenv("COURSE_TEST_KEY", "   ")
    _enable_plugin(pytester, monkeypatch)
    _make_write_test(pytester)

    result = pytester.runpytest_subprocess("--allow-write", "-q")

    assert result.ret != 0
    assert "COURSE_TEST_KEY" in result.stderr.str()
    assert "   " not in result.stderr.str()


def test_selected_write_test_executes_with_flag_and_synthetic_key(pytester, monkeypatch):
    monkeypatch.setenv("COURSE_TEST_KEY", "synthetic-course-key")
    _enable_plugin(pytester, monkeypatch)
    _make_write_test(pytester)

    result = pytester.runpytest_subprocess("--allow-write", "-q")

    result.assert_outcomes(passed=1)


def test_public_environment_loads_public_settings(pytester, monkeypatch):
    _enable_plugin(pytester, monkeypatch)
    pytester.makepyfile(
        """
        def test_public_settings(pytestconfig):
            assert pytestconfig._booking_settings.name == "public"
        """
    )

    result = pytester.runpytest_subprocess("--env=public", "-q")

    result.assert_outcomes(passed=1)
