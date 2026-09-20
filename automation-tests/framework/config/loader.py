"""Load validated, non-secret configuration for supported test environments."""

import os
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator, FormatChecker

from framework.config.models import Settings


_AUTOMATION_ROOT = Path(__file__).resolve().parents[2]
_CONFIG_DIR = _AUTOMATION_ROOT / "config"
_ALLOWED_ENVIRONMENTS = {"local", "public"}
_URL_OVERRIDES = {
    "web_url": "BOOKING_WEB_URL",
    "api_url": "BOOKING_API_URL",
}


def load_settings(environment: str) -> Settings:
    """Load one supported environment and apply non-empty URL overrides."""
    if environment not in _ALLOWED_ENVIRONMENTS:
        raise ValueError(f"unknown environment: {environment}")

    config = _load_yaml(_CONFIG_DIR / f"{environment}.yaml")
    schema = _load_yaml(_CONFIG_DIR / "schema.yaml")
    Draft202012Validator(schema, format_checker=FormatChecker()).validate(config)

    for key, variable in _URL_OVERRIDES.items():
        override = os.getenv(variable)
        if override:
            config[key] = override

    web_url = _normalize_url(config["web_url"])
    api_url = _normalize_url(config["api_url"])
    return Settings(
        name=config["name"],
        web_url=web_url,
        api_url=api_url,
        timeout_seconds=config["timeout_seconds"],
    )


def _load_yaml(path: Path) -> object:
    with path.open(encoding="utf-8") as file:
        return yaml.safe_load(file)


def _normalize_url(url: str) -> str:
    normalized = url.rstrip("/")
    if not normalized:
        raise ValueError("URL must not be empty after normalization")
    return normalized
