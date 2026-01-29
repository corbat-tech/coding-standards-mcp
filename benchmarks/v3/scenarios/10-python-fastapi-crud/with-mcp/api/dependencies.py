"""
API Dependencies for Dependency Injection.

Provides factory functions for service layer dependencies.
"""
from typing import Generator

from fastapi import Depends
from sqlalchemy.orm import Session

from core.database import get_db
from repositories.task_repository import TaskRepository
from repositories.task_repository_impl import TaskRepositoryImpl
from services.task_service import TaskService
from services.task_service_impl import TaskServiceImpl


def get_task_repository(
    db: Session = Depends(get_db)
) -> TaskRepository:
    """Get TaskRepository instance."""
    return TaskRepositoryImpl(db)


def get_task_service(
    repository: TaskRepository = Depends(get_task_repository)
) -> TaskService:
    """Get TaskService instance with repository dependency."""
    return TaskServiceImpl(repository)
