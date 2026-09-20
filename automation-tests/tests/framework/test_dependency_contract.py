from pathlib import Path


def test_requirements_use_supported_playwright_stack():
    text = Path("requirements.txt").read_text(encoding="utf-8")
    assert "pytest-playwright" in text
    assert "pytest-xdist" in text
    assert "allure-pytest" in text
    assert "pytest-allure-adaptor" not in text
