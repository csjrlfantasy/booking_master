from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class Settings:
    name: str
    web_url: str
    api_url: str
    timeout_seconds: int
