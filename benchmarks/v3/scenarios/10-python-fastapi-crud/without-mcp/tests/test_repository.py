"""Tests for TaskRepository."""

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base
from app.repository import TaskRepository
from app.schemas import TaskCreate, TaskUpdate
from app.models import TaskStatus, TaskPriority


@pytest.fixture(scope="function")
def db_session():
    """Create a new database session for each test."""
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    Base.metadata.create_all(bind=engine)
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)


@pytest.fixture
def repository(db_session):
    """Create a TaskRepository instance."""
    return TaskRepository(db_session)


@pytest.fixture
def sample_task_create():
    """Sample TaskCreate schema."""
    return TaskCreate(
        title="Test Task",
        description="Test description",
        status="pending",
        priority="medium",
        completed=False,
    )


class TestTaskRepository:
    """Tests for TaskRepository class."""

    def test_create_task(self, repository, sample_task_create):
        """Test creating a task through repository."""
        task = repository.create(sample_task_create)

        assert task.id is not None
        assert task.title == sample_task_create.title
        assert task.description == sample_task_create.description
        assert task.status == TaskStatus.PENDING
        assert task.priority == TaskPriority.MEDIUM
        assert task.completed is False

    def test_get_by_id(self, repository, sample_task_create):
        """Test getting task by ID."""
        created = repository.create(sample_task_create)
        task = repository.get_by_id(created.id)

        assert task is not None
        assert task.id == created.id
        assert task.title == created.title

    def test_get_by_id_not_found(self, repository):
        """Test getting non-existent task."""
        task = repository.get_by_id(9999)

        assert task is None

    def test_get_all_empty(self, repository):
        """Test getting all tasks when empty."""
        tasks = repository.get_all()

        assert tasks == []

    def test_get_all_with_tasks(self, repository, sample_task_create):
        """Test getting all tasks."""
        repository.create(sample_task_create)
        repository.create(TaskCreate(title="Task 2"))

        tasks = repository.get_all()

        assert len(tasks) == 2

    def test_get_all_with_pagination(self, repository, sample_task_create):
        """Test getting tasks with pagination."""
        for i in range(10):
            repository.create(TaskCreate(title=f"Task {i}"))

        tasks = repository.get_all(skip=2, limit=3)

        assert len(tasks) == 3

    def test_get_all_filter_by_status(self, repository):
        """Test filtering tasks by status."""
        repository.create(TaskCreate(title="Task 1", status="pending"))
        repository.create(TaskCreate(title="Task 2", status="completed"))
        repository.create(TaskCreate(title="Task 3", status="completed"))

        tasks = repository.get_all(status=TaskStatus.COMPLETED)

        assert len(tasks) == 2
        assert all(t.status == TaskStatus.COMPLETED for t in tasks)

    def test_get_all_filter_by_priority(self, repository):
        """Test filtering tasks by priority."""
        repository.create(TaskCreate(title="Task 1", priority="low"))
        repository.create(TaskCreate(title="Task 2", priority="high"))
        repository.create(TaskCreate(title="Task 3", priority="high"))

        tasks = repository.get_all(priority=TaskPriority.HIGH)

        assert len(tasks) == 2
        assert all(t.priority == TaskPriority.HIGH for t in tasks)

    def test_count_empty(self, repository):
        """Test counting tasks when empty."""
        count = repository.count()

        assert count == 0

    def test_count_with_tasks(self, repository, sample_task_create):
        """Test counting tasks."""
        repository.create(sample_task_create)
        repository.create(TaskCreate(title="Task 2"))

        count = repository.count()

        assert count == 2

    def test_count_with_filter(self, repository):
        """Test counting tasks with filter."""
        repository.create(TaskCreate(title="Task 1", status="pending"))
        repository.create(TaskCreate(title="Task 2", status="completed"))
        repository.create(TaskCreate(title="Task 3", status="completed"))

        count = repository.count(status=TaskStatus.COMPLETED)

        assert count == 2

    def test_update_task(self, repository, sample_task_create):
        """Test updating a task."""
        created = repository.create(sample_task_create)
        update_data = TaskUpdate(title="Updated Title", status="completed")

        updated = repository.update(created.id, update_data)

        assert updated is not None
        assert updated.title == "Updated Title"
        assert updated.status == TaskStatus.COMPLETED
        assert updated.description == sample_task_create.description  # Unchanged

    def test_update_task_partial(self, repository, sample_task_create):
        """Test partial update."""
        created = repository.create(sample_task_create)
        update_data = TaskUpdate(priority="high")

        updated = repository.update(created.id, update_data)

        assert updated.priority == TaskPriority.HIGH
        assert updated.title == sample_task_create.title

    def test_update_task_not_found(self, repository):
        """Test updating non-existent task."""
        update_data = TaskUpdate(title="Updated")
        result = repository.update(9999, update_data)

        assert result is None

    def test_delete_task(self, repository, sample_task_create):
        """Test deleting a task."""
        created = repository.create(sample_task_create)
        result = repository.delete(created.id)

        assert result is True
        assert repository.get_by_id(created.id) is None

    def test_delete_task_not_found(self, repository):
        """Test deleting non-existent task."""
        result = repository.delete(9999)

        assert result is False
