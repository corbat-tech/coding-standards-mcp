"""
Task Repository Implementation.

Implements data access using SQLAlchemy ORM.
"""
from typing import Optional

from sqlalchemy.orm import Session

from models.task import Task
from repositories.task_repository import TaskRepository


class TaskRepositoryImpl(TaskRepository):
    """SQLAlchemy implementation of TaskRepository."""

    def __init__(self, db: Session):
        """Initialize with database session."""
        self._db = db

    def create(self, task: Task) -> Task:
        """Create a new task in the database."""
        self._db.add(task)
        self._db.commit()
        self._db.refresh(task)
        return task

    def get_by_id(self, task_id: int) -> Optional[Task]:
        """Get a task by its ID."""
        return self._db.query(Task).filter(Task.id == task_id).first()

    def get_all(
        self,
        skip: int = 0,
        limit: int = 100,
        status: Optional[str] = None
    ) -> list[Task]:
        """Get all tasks with optional filtering and pagination."""
        query = self._db.query(Task)

        if status:
            query = query.filter(Task.status == status)

        return query.offset(skip).limit(limit).all()

    def count(self, status: Optional[str] = None) -> int:
        """Count total tasks with optional status filter."""
        query = self._db.query(Task)

        if status:
            query = query.filter(Task.status == status)

        return query.count()

    def update(self, task: Task) -> Task:
        """Update an existing task."""
        self._db.commit()
        self._db.refresh(task)
        return task

    def delete(self, task_id: int) -> bool:
        """Delete a task by its ID."""
        task = self.get_by_id(task_id)
        if not task:
            return False

        self._db.delete(task)
        self._db.commit()
        return True
