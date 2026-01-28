from typing import Optional, List
from contextlib import asynccontextmanager
from fastapi import FastAPI, Depends, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from database import get_db, init_db
from models import TaskStatus, TaskPriority
from repository import TaskRepository
from service import TaskService
from schemas import TaskCreate, TaskUpdate, TaskResponse


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield


app = FastAPI(title="Task Management API", lifespan=lifespan)


def get_task_service(db: AsyncSession = Depends(get_db)) -> TaskService:
    repository = TaskRepository(db)
    return TaskService(repository)


@app.post("/tasks", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
async def create_task(
    task_data: TaskCreate,
    service: TaskService = Depends(get_task_service)
):
    """Create a new task."""
    task = await service.create_task(task_data)
    return task


@app.get("/tasks", response_model=List[TaskResponse])
async def get_tasks(
    task_status: Optional[TaskStatus] = Query(None, alias="status"),
    priority: Optional[TaskPriority] = None,
    service: TaskService = Depends(get_task_service)
):
    """Get all tasks with optional filtering by status and priority."""
    return await service.get_tasks(task_status=task_status, priority=priority)


@app.get("/tasks/{task_id}", response_model=TaskResponse)
async def get_task(
    task_id: str,
    service: TaskService = Depends(get_task_service)
):
    """Get a specific task by ID."""
    return await service.get_task(task_id)


@app.put("/tasks/{task_id}", response_model=TaskResponse)
async def update_task(
    task_id: str,
    task_data: TaskUpdate,
    service: TaskService = Depends(get_task_service)
):
    """Update a task. Cannot modify completed tasks."""
    return await service.update_task(task_id, task_data)


@app.delete("/tasks/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_task(
    task_id: str,
    service: TaskService = Depends(get_task_service)
):
    """Delete a task."""
    await service.delete_task(task_id)


@app.post("/tasks/{task_id}/complete", response_model=TaskResponse)
async def mark_task_completed(
    task_id: str,
    service: TaskService = Depends(get_task_service)
):
    """Mark a task as completed."""
    return await service.mark_as_completed(task_id)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
