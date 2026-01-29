"""
Task Service Interface - Defines business logic contract.

Following the Service Layer Pattern for encapsulating business rules.
"""
from abc import ABC, abstractmethod
from typing import Optional

from schemas.task_schema import (
    TaskCreate,
    TaskUpdate,
    TaskResponse,
    TaskListResponse
)


class TaskService(ABC):
    """Abstract service interface for task business logic."""

    @abstractmethod
    def create_task(self, task_data: TaskCreate) -> TaskResponse:
        """Create a new task."""
        pass

    @abstractmethod
    def get_task(self, task_id: int) -> TaskResponse:
        """Get a task by ID. Raises TaskNotFoundException if not found."""
        pass

    @abstractmethod
    def get_tasks(
        self,
        page: int = 1,
        page_size: int = 10,
        status: Optional[str] = None
    ) -> TaskListResponse:
        """Get paginated list of tasks."""
        pass

    @abstractmethod
    def update_task(
        self,
        task_id: int,
        task_data: TaskUpdate
    ) -> TaskResponse:
        """Update a task. Raises TaskNotFoundException if not found."""
        pass

    @abstractmethod
    def delete_task(self, task_id: int) -> bool:
        """Delete a task. Raises TaskNotFoundException if not found."""
        pass
