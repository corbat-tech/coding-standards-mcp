from typing import Optional, List

from sqlalchemy import select, delete
from sqlalchemy.ext.asyncio import AsyncSession

from ..domain.task import Task
from ..domain.ports import TaskRepository
from .models import TaskModel


class SqlAlchemyTaskRepository(TaskRepository):
    def __init__(self, session: AsyncSession):
        self._session = session

    async def find_by_id(self, task_id: str) -> Optional[Task]:
        result = await self._session.execute(
            select(TaskModel).where(TaskModel.id == task_id)
        )
        model = result.scalar_one_or_none()
        return self._to_domain(model) if model else None

    async def find_all(self) -> List[Task]:
        result = await self._session.execute(select(TaskModel))
        models = result.scalars().all()
        return [self._to_domain(m) for m in models]

    async def save(self, task: Task) -> None:
        model = await self._session.get(TaskModel, task.id)
        if model:
            model.title = task.title
            model.description = task.description
            model.status = task.status
            model.updated_at = task.updated_at
        else:
            model = TaskModel(
                id=task.id,
                title=task.title,
                description=task.description,
                status=task.status,
                created_at=task.created_at,
                updated_at=task.updated_at
            )
            self._session.add(model)
        await self._session.commit()

    async def delete(self, task_id: str) -> bool:
        result = await self._session.execute(
            delete(TaskModel).where(TaskModel.id == task_id)
        )
        await self._session.commit()
        return result.rowcount > 0

    def _to_domain(self, model: TaskModel) -> Task:
        return Task(
            id=model.id,
            title=model.title,
            description=model.description,
            status=model.status,
            created_at=model.created_at,
            updated_at=model.updated_at
        )
