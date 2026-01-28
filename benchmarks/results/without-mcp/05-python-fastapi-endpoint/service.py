from typing import Optional, List
from datetime import datetime
from fastapi import HTTPException, status
from models import Task, TaskStatus, TaskPriority
from repository import TaskRepository
from schemas import TaskCreate, TaskUpdate


class TaskService:
    def __init__(self, repository: TaskRepository):
        self.repository = repository

    async def create_task(self, task_data: TaskCreate) -> Task:
        return await self.repository.create(
            title=task_data.title,
            description=task_data.description,
            priority=task_data.priority,
            due_date=task_data.due_date
        )

    async def get_task(self, task_id: str) -> Task:
        task = await self.repository.get_by_id(task_id)
        if not task:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Task with id {task_id} not found"
            )
        return task

    async def get_tasks(self, task_status: Optional[TaskStatus] = None,
                        priority: Optional[TaskPriority] = None) -> List[Task]:
        return await self.repository.get_all(status=task_status, priority=priority)

    async def update_task(self, task_id: str, task_data: TaskUpdate) -> Task:
        task = await self.get_task(task_id)

        # Check if task is completed - no changes allowed
        if task.status == TaskStatus.COMPLETED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Cannot modify a completed task"
            )

        update_data = task_data.model_dump(exclude_unset=True)
        return await self.repository.update(task, **update_data)

    async def delete_task(self, task_id: str) -> None:
        task = await self.get_task(task_id)
        await self.repository.delete(task)

    async def mark_as_completed(self, task_id: str) -> Task:
        task = await self.get_task(task_id)

        if task.status == TaskStatus.COMPLETED:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Task is already completed"
            )

        return await self.repository.update(task, status=TaskStatus.COMPLETED)
