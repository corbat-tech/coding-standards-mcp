"""
Tests for TaskRepository Implementation.

Tests data access layer functionality.
"""
import pytest
from datetime import datetime

from sqlalchemy.orm import Session

from models.task import Task
from schemas.task_schema import TaskStatus, TaskPriority
from repositories.task_repository_impl import TaskRepositoryImpl


class TestTaskRepository:
    """Test suite for TaskRepository."""

    def test_create_task_success(self, db_session: Session):
        """Should create a new task successfully."""
        repository = TaskRepositoryImpl(db_session)
        task = Task(
            title="New Task",
            description="Description",
            status=TaskStatus.PENDING,
            priority=TaskPriority.HIGH,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )

        result = repository.create(task)

        assert result.id is not None
        assert result.title == "New Task"
        assert result.status == TaskStatus.PENDING

    def test_get_task_by_id_success(
        self,
        db_session: Session,
        sample_task: Task
    ):
        """Should retrieve task by ID."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.get_by_id(sample_task.id)

        assert result is not None
        assert result.id == sample_task.id
        assert result.title == sample_task.title

    def test_get_task_by_id_not_found(self, db_session: Session):
        """Should return None for non-existent task."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.get_by_id(999)

        assert result is None

    def test_get_all_tasks(
        self,
        db_session: Session,
        sample_tasks: list[Task]
    ):
        """Should retrieve all tasks with pagination."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.get_all(skip=0, limit=10)

        assert len(result) == 5
        assert all(isinstance(t, Task) for t in result)

    def test_get_all_tasks_with_pagination(
        self,
        db_session: Session,
        sample_tasks: list[Task]
    ):
        """Should apply pagination correctly."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.get_all(skip=2, limit=2)

        assert len(result) == 2

    def test_get_all_tasks_with_status_filter(
        self,
        db_session: Session,
        sample_tasks: list[Task]
    ):
        """Should filter tasks by status."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.get_all(status=TaskStatus.PENDING.value)

        assert all(t.status == TaskStatus.PENDING for t in result)

    def test_count_all_tasks(
        self,
        db_session: Session,
        sample_tasks: list[Task]
    ):
        """Should count all tasks."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.count()

        assert result == 5

    def test_count_tasks_with_filter(
        self,
        db_session: Session,
        sample_tasks: list[Task]
    ):
        """Should count tasks with status filter."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.count(status=TaskStatus.COMPLETED.value)

        assert result == 2

    def test_update_task_success(
        self,
        db_session: Session,
        sample_task: Task
    ):
        """Should update task successfully."""
        repository = TaskRepositoryImpl(db_session)
        sample_task.title = "Updated Title"

        result = repository.update(sample_task)

        assert result.title == "Updated Title"

    def test_delete_task_success(
        self,
        db_session: Session,
        sample_task: Task
    ):
        """Should delete task successfully."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.delete(sample_task.id)

        assert result is True
        assert repository.get_by_id(sample_task.id) is None

    def test_delete_task_not_found(self, db_session: Session):
        """Should return False for non-existent task."""
        repository = TaskRepositoryImpl(db_session)

        result = repository.delete(999)

        assert result is False
