"""Task repository for database operations."""

from sqlalchemy.orm import Session
from sqlalchemy import func
from typing import Optional

from app.models import Task, TaskStatus, TaskPriority
from app.schemas import TaskCreate, TaskUpdate


class TaskRepository:
    """Repository class for Task database operations."""

    def __init__(self, db: Session):
        """Initialize repository with database session."""
        self.db = db

    def get_all(
        self,
        skip: int = 0,
        limit: int = 100,
        status: Optional[TaskStatus] = None,
        priority: Optional[TaskPriority] = None
    ) -> list[Task]:
        """
        Get all tasks with optional filtering and pagination.

        Args:
            skip: Number of records to skip
            limit: Maximum number of records to return
            status: Filter by task status
            priority: Filter by task priority

        Returns:
            List of Task objects
        """
        query = self.db.query(Task)

        if status:
            query = query.filter(Task.status == status)
        if priority:
            query = query.filter(Task.priority == priority)

        return query.offset(skip).limit(limit).all()

    def count(
        self,
        status: Optional[TaskStatus] = None,
        priority: Optional[TaskPriority] = None
    ) -> int:
        """
        Count total tasks with optional filtering.

        Args:
            status: Filter by task status
            priority: Filter by task priority

        Returns:
            Total count of tasks
        """
        query = self.db.query(func.count(Task.id))

        if status:
            query = query.filter(Task.status == status)
        if priority:
            query = query.filter(Task.priority == priority)

        return query.scalar()

    def get_by_id(self, task_id: int) -> Optional[Task]:
        """
        Get a task by its ID.

        Args:
            task_id: The task ID

        Returns:
            Task object or None if not found
        """
        return self.db.query(Task).filter(Task.id == task_id).first()

    def create(self, task_data: TaskCreate) -> Task:
        """
        Create a new task.

        Args:
            task_data: Task creation data

        Returns:
            Created Task object
        """
        db_task = Task(**task_data.model_dump())
        self.db.add(db_task)
        self.db.commit()
        self.db.refresh(db_task)
        return db_task

    def update(self, task_id: int, task_data: TaskUpdate) -> Optional[Task]:
        """
        Update an existing task.

        Args:
            task_id: The task ID to update
            task_data: Task update data

        Returns:
            Updated Task object or None if not found
        """
        db_task = self.get_by_id(task_id)
        if not db_task:
            return None

        update_data = task_data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(db_task, field, value)

        self.db.commit()
        self.db.refresh(db_task)
        return db_task

    def delete(self, task_id: int) -> bool:
        """
        Delete a task by its ID.

        Args:
            task_id: The task ID to delete

        Returns:
            True if deleted, False if not found
        """
        db_task = self.get_by_id(task_id)
        if not db_task:
            return False

        self.db.delete(db_task)
        self.db.commit()
        return True
