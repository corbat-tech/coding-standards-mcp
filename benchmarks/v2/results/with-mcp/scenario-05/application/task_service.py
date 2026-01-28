from typing import List, Optional

from ..domain.task import Task, TaskStatus
from ..domain.ports import TaskRepository, IdGenerator, Clock
from ..domain.exceptions import TaskNotFoundError, InvalidTaskInputError


class TaskService:
    def __init__(
        self,
        repository: TaskRepository,
        id_generator: IdGenerator,
        clock: Clock
    ):
        self._repository = repository
        self._id_generator = id_generator
        self._clock = clock

    async def create_task(self, title: str, description: str = "") -> Task:
        self._validate_title(title)

        task = Task.create(
            id=self._id_generator.generate(),
            title=title,
            description=description,
            created_at=self._clock.now()
        )
        await self._repository.save(task)
        return task

    async def get_task(self, task_id: str) -> Task:
        task = await self._repository.find_by_id(task_id)
        if not task:
            raise TaskNotFoundError(task_id)
        return task

    async def list_tasks(self) -> List[Task]:
        return await self._repository.find_all()

    async def update_task(
        self,
        task_id: str,
        title: Optional[str] = None,
        description: Optional[str] = None,
        status: Optional[TaskStatus] = None
    ) -> Task:
        if title is not None:
            self._validate_title(title)

        task = await self.get_task(task_id)
        updated_task = task.update(
            title=title,
            description=description,
            status=status,
            updated_at=self._clock.now()
        )
        await self._repository.save(updated_task)
        return updated_task

    async def delete_task(self, task_id: str) -> None:
        deleted = await self._repository.delete(task_id)
        if not deleted:
            raise TaskNotFoundError(task_id)

    def _validate_title(self, title: str) -> None:
        if not title or not title.strip():
            raise InvalidTaskInputError("title", "cannot be empty")
        if len(title.strip()) > 200:
            raise InvalidTaskInputError("title", "cannot exceed 200 characters")
