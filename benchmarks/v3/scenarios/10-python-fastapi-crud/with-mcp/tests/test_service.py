"""
Tests for TaskService Implementation.

Tests business logic layer functionality.
"""
import pytest
from unittest.mock import Mock, MagicMock
from datetime import datetime

from models.task import Task
from schemas.task_schema import (
    TaskCreate,
    TaskUpdate,
    TaskStatus,
    TaskPriority
)
from services.task_service_impl import TaskServiceImpl
from repositories.task_repository import TaskRepository
from core.exceptions import TaskNotFoundException


class TestTaskService:
    """Test suite for TaskService."""

    @pytest.fixture
    def mock_repository(self) -> Mock:
        """Create a mock repository."""
        return Mock(spec=TaskRepository)

    @pytest.fixture
    def service(self, mock_repository: Mock) -> TaskServiceImpl:
        """Create service with mock repository."""
        return TaskServiceImpl(mock_repository)

    def test_create_task_success(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should create task successfully."""
        task_data = TaskCreate(
            title="New Task",
            description="Description",
            priority=TaskPriority.HIGH
        )
        mock_task = Task(
            id=1,
            title="New Task",
            description="Description",
            status=TaskStatus.PENDING,
            priority=TaskPriority.HIGH,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        mock_repository.create.return_value = mock_task

        result = service.create_task(task_data)

        assert result.id == 1
        assert result.title == "New Task"
        mock_repository.create.assert_called_once()

    def test_get_task_success(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should get task by ID successfully."""
        mock_task = Task(
            id=1,
            title="Test Task",
            description="Description",
            status=TaskStatus.PENDING,
            priority=TaskPriority.MEDIUM,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        mock_repository.get_by_id.return_value = mock_task

        result = service.get_task(1)

        assert result.id == 1
        assert result.title == "Test Task"

    def test_get_task_not_found(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should raise TaskNotFoundException for non-existent task."""
        mock_repository.get_by_id.return_value = None

        with pytest.raises(TaskNotFoundException) as exc_info:
            service.get_task(999)

        assert exc_info.value.task_id == 999

    def test_get_tasks_paginated(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should return paginated task list."""
        mock_tasks = [
            Task(
                id=i,
                title=f"Task {i}",
                description="Desc",
                status=TaskStatus.PENDING,
                priority=TaskPriority.MEDIUM,
                created_at=datetime.utcnow(),
                updated_at=datetime.utcnow()
            )
            for i in range(1, 4)
        ]
        mock_repository.get_all.return_value = mock_tasks
        mock_repository.count.return_value = 10

        result = service.get_tasks(page=1, page_size=3)

        assert len(result.items) == 3
        assert result.total == 10
        assert result.page == 1
        assert result.page_size == 3

    def test_update_task_success(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should update task successfully."""
        mock_task = Task(
            id=1,
            title="Old Title",
            description="Description",
            status=TaskStatus.PENDING,
            priority=TaskPriority.MEDIUM,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        mock_repository.get_by_id.return_value = mock_task
        mock_repository.update.return_value = mock_task

        update_data = TaskUpdate(title="New Title")
        result = service.update_task(1, update_data)

        mock_repository.update.assert_called_once()

    def test_update_task_not_found(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should raise TaskNotFoundException on update."""
        mock_repository.get_by_id.return_value = None

        with pytest.raises(TaskNotFoundException):
            service.update_task(999, TaskUpdate(title="New"))

    def test_delete_task_success(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should delete task successfully."""
        mock_task = Task(
            id=1,
            title="Task",
            description="Desc",
            status=TaskStatus.PENDING,
            priority=TaskPriority.MEDIUM,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        mock_repository.get_by_id.return_value = mock_task
        mock_repository.delete.return_value = True

        result = service.delete_task(1)

        assert result is True

    def test_delete_task_not_found(
        self,
        service: TaskServiceImpl,
        mock_repository: Mock
    ):
        """Should raise TaskNotFoundException on delete."""
        mock_repository.get_by_id.return_value = None

        with pytest.raises(TaskNotFoundException):
            service.delete_task(999)
