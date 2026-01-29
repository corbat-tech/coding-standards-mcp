"""
Task Repository Interface - Defines data access contract.

Following the Repository Pattern for clean separation of concerns.
"""
from abc import ABC, abstractmethod
from typing import Optional

from models.task import Task


class TaskRepository(ABC):
    """Abstract repository interface for task data access."""

    @abstractmethod
    def create(self, task: Task) -> Task:
        """Create a new task."""
        pass

    @abstractmethod
    def get_by_id(self, task_id: int) -> Optional[Task]:
        """Get a task by its ID."""
        pass

    @abstractmethod
    def get_all(
        self,
        skip: int = 0,
        limit: int = 100,
        status: Optional[str] = None
    ) -> list[Task]:
        """Get all tasks with optional filtering."""
        pass

    @abstractmethod
    def count(self, status: Optional[str] = None) -> int:
        """Count total tasks with optional filtering."""
        pass

    @abstractmethod
    def update(self, task: Task) -> Task:
        """Update an existing task."""
        pass

    @abstractmethod
    def delete(self, task_id: int) -> bool:
        """Delete a task by its ID. Returns True if deleted."""
        pass
