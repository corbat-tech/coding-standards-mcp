"""
Task SQLAlchemy Model.

Defines the database schema for tasks.
"""
from datetime import datetime

from sqlalchemy import Column, Integer, String, DateTime, Enum as SQLEnum
from sqlalchemy.sql import func

from core.database import Base
from schemas.task_schema import TaskStatus, TaskPriority


class Task(Base):
    """Task database model."""

    __tablename__ = "tasks"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(200), nullable=False, index=True)
    description = Column(String(2000), nullable=True)
    status = Column(
        SQLEnum(TaskStatus),
        default=TaskStatus.PENDING,
        nullable=False,
        index=True
    )
    priority = Column(
        SQLEnum(TaskPriority),
        default=TaskPriority.MEDIUM,
        nullable=False
    )
    due_date = Column(DateTime, nullable=True)
    created_at = Column(
        DateTime,
        server_default=func.now(),
        nullable=False
    )
    updated_at = Column(
        DateTime,
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False
    )

    def __repr__(self) -> str:
        return f"<Task(id={self.id}, title='{self.title}')>"
