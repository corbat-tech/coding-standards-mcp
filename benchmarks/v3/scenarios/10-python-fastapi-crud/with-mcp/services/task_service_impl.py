"""
Task Service Implementation.

Implements business logic for task management.
"""
import math
from datetime import datetime
from typing import Optional

from models.task import Task
from schemas.task_schema import (
    TaskCreate,
    TaskUpdate,
    TaskResponse,
    TaskListResponse
)
from services.task_service import TaskService
from repositories.task_repository import TaskRepository
from core.exceptions import TaskNotFoundException


class TaskServiceImpl(TaskService):
    """Implementation of TaskService interface."""

    def __init__(self, repository: TaskRepository):
        """Initialize with repository dependency."""
        self._repository = repository

    def create_task(self, task_data: TaskCreate) -> TaskResponse:
        """Create a new task."""
        task = Task(
            title=task_data.title,
            description=task_data.description,
            status=task_data.status,
            priority=task_data.priority,
            due_date=task_data.due_date,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        created_task = self._repository.create(task)
        return TaskResponse.model_validate(created_task)

    def get_task(self, task_id: int) -> TaskResponse:
        """Get a task by ID."""
        task = self._repository.get_by_id(task_id)
        if not task:
            raise TaskNotFoundException(task_id)
        return TaskResponse.model_validate(task)

    def get_tasks(
        self,
        page: int = 1,
        page_size: int = 10,
        status: Optional[str] = None
    ) -> TaskListResponse:
        """Get paginated list of tasks."""
        skip = (page - 1) * page_size
        tasks = self._repository.get_all(
            skip=skip,
            limit=page_size,
            status=status
        )
        total = self._repository.count(status=status)
        total_pages = math.ceil(total / page_size) if page_size > 0 else 0

        return TaskListResponse(
            items=[TaskResponse.model_validate(t) for t in tasks],
            total=total,
            page=page,
            page_size=page_size,
            total_pages=total_pages
        )

    def update_task(
        self,
        task_id: int,
        task_data: TaskUpdate
    ) -> TaskResponse:
        """Update a task."""
        task = self._repository.get_by_id(task_id)
        if not task:
            raise TaskNotFoundException(task_id)

        update_fields = task_data.model_dump(exclude_unset=True)
        for field, value in update_fields.items():
            setattr(task, field, value)
        task.updated_at = datetime.utcnow()

        updated_task = self._repository.update(task)
        return TaskResponse.model_validate(updated_task)

    def delete_task(self, task_id: int) -> bool:
        """Delete a task."""
        task = self._repository.get_by_id(task_id)
        if not task:
            raise TaskNotFoundException(task_id)
        return self._repository.delete(task_id)
