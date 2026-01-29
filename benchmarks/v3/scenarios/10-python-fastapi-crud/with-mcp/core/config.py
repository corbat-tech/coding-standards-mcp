"""
Application Configuration.

Uses environment variables for configuration with sensible defaults.
"""
import os
from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment."""

    app_name: str = "Task Management API"
    app_version: str = "1.0.0"
    debug: bool = False

    # Database settings
    database_url: str = "sqlite:///./tasks.db"

    # Pagination defaults
    default_page_size: int = 10
    max_page_size: int = 100

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
