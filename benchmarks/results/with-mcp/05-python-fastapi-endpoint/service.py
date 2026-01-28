from models import Task, TaskPriority, TaskStatus
from repository import TaskRepository
from schemas import TaskCreate, TaskUpdate
from exceptions import TaskAlreadyCompletedException, TaskNotFoundException


class TaskService:
    def __init__(self, repository: TaskRepository) -> None:
        self._repository = repository

    async def create_task(self, data: TaskCreate) -> Task:
        task = Task(
            title=data.title,
            description=data.description,
            priority=data.priority,
            due_date=data.due_date,
        )
        return await self._repository.create(task)

    async def get_task(self, task_id: int) -> Task:
        task = await self._repository.get_by_id(task_id)
        if task is None:
            raise TaskNotFoundException(task_id)
        return task

    async def list_tasks(
        self,
        status: TaskStatus | None = None,
        priority: TaskPriority | None = None,
        skip: int = 0,
        limit: int = 100,
    ) -> tuple[list[Task], int]:
        tasks = await self._repository.get_all(status, priority, skip, limit)
        total = await self._repository.count(status, priority)
        return tasks, total

    async def update_task(self, task_id: int, data: TaskUpdate) -> Task:
        task = await self.get_task(task_id)
        self._ensure_task_can_be_modified(task)

        if data.title is not None:
            task.title = data.title
        if data.description is not None:
            task.description = data.description
        if data.priority is not None:
            task.priority = data.priority
        if data.due_date is not None:
            task.due_date = data.due_date

        return await self._repository.update(task)

    async def delete_task(self, task_id: int) -> None:
        task = await self.get_task(task_id)
        await self._repository.delete(task)

    async def mark_as_completed(self, task_id: int) -> Task:
        task = await self.get_task(task_id)
        if task.is_completed():
            raise TaskAlreadyCompletedException(task_id)

        task.status = TaskStatus.COMPLETED
        return await self._repository.update(task)

    def _ensure_task_can_be_modified(self, task: Task) -> None:
        if not task.can_be_modified():
            raise TaskAlreadyCompletedException(task.id)
