from pathlib import Path

import pytest
import yaml
from jsonschema import ValidationError

from framework.config import loader
from framework.config.loader import load_settings


def test_local_defaults(monkeypatch):
    monkeypatch.delenv("BOOKING_WEB_URL", raising=False)
    monkeypatch.delenv("BOOKING_API_URL", raising=False)

    settings = load_settings("local")

    assert settings.web_url == "http://127.0.0.1:3000"
    assert settings.api_url == "http://127.0.0.1:8088/api"


def test_environment_overrides_yaml(monkeypatch):
    monkeypatch.setenv("BOOKING_API_URL", "http://example.test/api/")

    settings = load_settings("public")

    assert settings.api_url == "http://example.test/api"


def test_unknown_environment_is_rejected():
    with pytest.raises(ValueError, match="unknown environment"):
        load_settings("production")


def test_missing_api_url_fails_schema_validation(monkeypatch, tmp_path):
    schema_path = Path(__file__).resolve().parents[2] / "config" / "schema.yaml"
    schema = yaml.safe_load(schema_path.read_text(encoding="utf-8"))
    (tmp_path / "schema.yaml").write_text(yaml.safe_dump(schema), encoding="utf-8")
    (tmp_path / "local.yaml").write_text(
        "name: local\nweb_url: http://127.0.0.1:3000\ntimeout_seconds: 30\n",
        encoding="utf-8",
    )

    monkeypatch.setattr(loader, "_CONFIG_DIR", tmp_path)

    with pytest.raises(ValidationError, match="api_url"):
        load_settings("local")


def test_trailing_slashes_are_normalized(monkeypatch):
    monkeypatch.setenv("BOOKING_WEB_URL", "http://example.test/")
    monkeypatch.setenv("BOOKING_API_URL", "http://example.test/api///")

    settings = load_settings("public")

    assert settings.web_url == "http://example.test"
    assert settings.api_url == "http://example.test/api"
