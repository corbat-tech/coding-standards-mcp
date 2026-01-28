import pytest
from datetime import datetime
from typing import Optional, List, Dict

from ..domain.task import Task, TaskStatus
from ..domain.ports import TaskRepository, IdGenerator, Clock
from ..domain.exceptions import TaskNotFoundError, InvalidTaskInputError
from ..application.task_service import TaskService


class InMemoryTaskRepository(TaskRepository):
    def __init__(self):
        self._tasks: Dict[str, Task] = {}

    async def find_by_id(self, task_id: str) -> Optional[Task]:
        return self._tasks.get(task_id)

    async def find_all(self) -> List[Task]:
        return list(self._tasks.values())

    async def save(self, task: Task) -> None:
        self._tasks[task.id] = task

    async def delete(self, task_id: str) -> bool:
        if task_id in self._tasks:
            del self._tasks[task_id]
            return True
        return False

    def clear(self):
        self._tasks.clear()


class StubIdGenerator(IdGenerator):
    def __init__(self):
        self._counter = 0

    def generate(self) -> str:
        self._counter += 1
        return f"task-{self._counter}"


class StubClock(Clock):
    def __init__(self, fixed_time: datetime):
        self._fixed_time = fixed_time

    def now(self) -> datetime:
        return self._fixed_time


@pytest.fixture
def repository():
    return InMemoryTaskRepository()


@pytest.fixture
def id_generator():
    return StubIdGenerator()


@pytest.fixture
def clock():
    return StubClock(datetime(2024, 1, 15, 10, 0, 0))


@pytest.fixture
def service(repository, id_generator, clock):
    return TaskService(repository, id_generator, clock)


class TestCreateTask:
    @pytest.mark.asyncio
    async def test_should_create_task_when_valid_input(self, service):
        # Arrange
        title = "Test task"
        description = "Test description"

        # Act
        task = await service.create_task(title, description)

        # Assert
        assert task.id == "task-1"
        assert task.title == "Test task"
        assert task.status == TaskStatus.PENDING

    @pytest.mark.asyncio
    async def test_should_trim_title_when_has_whitespace(self, service):
        # Arrange & Act
        task = await service.create_task("  Test task  ", "")

        # Assert
        assert task.title == "Test task"

    @pytest.mark.asyncio
    async def test_should_raise_error_when_title_empty(self, service):
        # Act & Assert
        with pytest.raises(InvalidTaskInputError) as exc_info:
            await service.create_task("", "description")
        assert exc_info.value.field == "title"

    @pytest.mark.asyncio
    async def test_should_raise_error_when_title_too_long(self, service):
        # Arrange
        long_title = "a" * 201

        # Act & Assert
        with pytest.raises(InvalidTaskInputError) as exc_info:
            await service.create_task(long_title, "")
        assert "200 characters" in exc_info.value.reason


class TestGetTask:
    @pytest.mark.asyncio
    async def test_should_return_task_when_exists(self, service):
        # Arrange
        created = await service.create_task("Test", "")

        # Act
        task = await service.get_task(created.id)

        # Assert
        assert task == created

    @pytest.mark.asyncio
    async def test_should_raise_error_when_not_found(self, service):
        # Act & Assert
        with pytest.raises(TaskNotFoundError) as exc_info:
            await service.get_task("non-existent")
        assert exc_info.value.task_id == "non-existent"


class TestListTasks:
    @pytest.mark.asyncio
    async def test_should_return_empty_when_no_tasks(self, service):
        # Act
        tasks = await service.list_tasks()

        # Assert
        assert tasks == []

    @pytest.mark.asyncio
    async def test_should_return_all_tasks(self, service):
        # Arrange
        await service.create_task("Task 1", "")
        await service.create_task("Task 2", "")

        # Act
        tasks = await service.list_tasks()

        # Assert
        assert len(tasks) == 2


class TestUpdateTask:
    @pytest.mark.asyncio
    async def test_should_update_title_when_provided(self, service):
        # Arrange
        created = await service.create_task("Original", "")

        # Act
        updated = await service.update_task(created.id, title="Updated")

        # Assert
        assert updated.title == "Updated"

    @pytest.mark.asyncio
    async def test_should_update_status_when_provided(self, service):
        # Arrange
        created = await service.create_task("Test", "")

        # Act
        updated = await service.update_task(created.id, status=TaskStatus.COMPLETED)

        # Assert
        assert updated.status == TaskStatus.COMPLETED


class TestDeleteTask:
    @pytest.mark.asyncio
    async def test_should_delete_task_when_exists(self, service):
        # Arrange
        created = await service.create_task("Test", "")

        # Act
        await service.delete_task(created.id)

        # Assert
        with pytest.raises(TaskNotFoundError):
            await service.get_task(created.id)

    @pytest.mark.asyncio
    async def test_should_raise_error_when_not_found(self, service):
        # Act & Assert
        with pytest.raises(TaskNotFoundError):
            await service.delete_task("non-existent")
