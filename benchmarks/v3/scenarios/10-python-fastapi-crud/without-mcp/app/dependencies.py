"""Dependency injection for FastAPI."""

from typing import Annotated
from fastapi import Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.repository import TaskRepository
from app.config import Settings, get_settings


# Type aliases for dependency injection
DbSession = Annotated[Session, Depends(get_db)]
AppSettings = Annotated[Settings, Depends(get_settings)]


def get_task_repository(db: DbSession) -> TaskRepository:
    """
    Dependency that provides a TaskRepository instance.

    Args:
        db: Database session from dependency injection

    Returns:
        TaskRepository instance
    """
    return TaskRepository(db)


# Type alias for TaskRepository dependency
TaskRepo = Annotated[TaskRepository, Depends(get_task_repository)]
