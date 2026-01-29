"""Application configuration using pydantic-settings."""

from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings with environment variable support."""

    app_name: str = "Task Management API"
    database_url: str = "sqlite:///./tasks.db"
    debug: bool = False

    class Config:
        env_file = ".env"


@lru_cache
def get_settings() -> Settings:
    """Get cached application settings (singleton pattern)."""
    return Settings()
