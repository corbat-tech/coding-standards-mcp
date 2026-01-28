from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from models import Task, TaskPriority, TaskStatus


class TaskRepository:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def create(self, task: Task) -> Task:
        self._session.add(task)
        await self._session.commit()
        await self._session.refresh(task)
        return task

    async def get_by_id(self, task_id: int) -> Task | None:
        result = await self._session.execute(select(Task).where(Task.id == task_id))
        return result.scalar_one_or_none()

    async def get_all(
        self,
        status: TaskStatus | None = None,
        priority: TaskPriority | None = None,
        skip: int = 0,
        limit: int = 100,
    ) -> list[Task]:
        query = select(Task)

        if status is not None:
            query = query.where(Task.status == status)
        if priority is not None:
            query = query.where(Task.priority == priority)

        query = query.offset(skip).limit(limit).order_by(Task.created_at.desc())
        result = await self._session.execute(query)
        return list(result.scalars().all())

    async def count(
        self,
        status: TaskStatus | None = None,
        priority: TaskPriority | None = None,
    ) -> int:
        query = select(Task)

        if status is not None:
            query = query.where(Task.status == status)
        if priority is not None:
            query = query.where(Task.priority == priority)

        result = await self._session.execute(query)
        return len(list(result.scalars().all()))

    async def update(self, task: Task) -> Task:
        await self._session.commit()
        await self._session.refresh(task)
        return task

    async def delete(self, task: Task) -> None:
        await self._session.delete(task)
        await self._session.commit()
