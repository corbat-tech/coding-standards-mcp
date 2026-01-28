from typing import Optional, List
from uuid import uuid4
from datetime import datetime
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from models import Task, TaskStatus, TaskPriority


class TaskRepository:
    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, title: str, description: Optional[str],
                     priority: TaskPriority, due_date: Optional[datetime]) -> Task:
        task = Task(
            id=str(uuid4()),
            title=title,
            description=description,
            status=TaskStatus.PENDING,
            priority=priority,
            due_date=due_date,
            created_at=datetime.utcnow()
        )
        self.session.add(task)
        await self.session.commit()
        await self.session.refresh(task)
        return task

    async def get_by_id(self, task_id: str) -> Optional[Task]:
        result = await self.session.execute(
            select(Task).where(Task.id == task_id)
        )
        return result.scalar_one_or_none()

    async def get_all(self, status: Optional[TaskStatus] = None,
                      priority: Optional[TaskPriority] = None) -> List[Task]:
        query = select(Task)

        if status:
            query = query.where(Task.status == status)
        if priority:
            query = query.where(Task.priority == priority)

        result = await self.session.execute(query)
        return list(result.scalars().all())

    async def update(self, task: Task, **kwargs) -> Task:
        for key, value in kwargs.items():
            if value is not None:
                setattr(task, key, value)

        task.updated_at = datetime.utcnow()
        await self.session.commit()
        await self.session.refresh(task)
        return task

    async def delete(self, task: Task) -> None:
        await self.session.delete(task)
        await self.session.commit()
