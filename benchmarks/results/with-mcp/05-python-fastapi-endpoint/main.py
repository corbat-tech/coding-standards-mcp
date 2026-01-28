from contextlib import asynccontextmanager
from fastapi import Depends, FastAPI, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from database import get_session, init_db
from models import TaskPriority, TaskStatus
from repository import TaskRepository
from schemas import (
    ErrorResponse,
    TaskCreate,
    TaskListResponse,
    TaskResponse,
    TaskUpdate,
)
from service import TaskService


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield


app = FastAPI(
    title="Task Management API",
    version="1.0.0",
    lifespan=lifespan,
)


def get_task_service(session: AsyncSession = Depends(get_session)) -> TaskService:
    repository = TaskRepository(session)
    return TaskService(repository)


@app.post(
    "/tasks",
    response_model=TaskResponse,
    status_code=status.HTTP_201_CREATED,
    responses={400: {"model": ErrorResponse}},
)
async def create_task(
    data: TaskCreate,
    service: TaskService = Depends(get_task_service),
) -> TaskResponse:
    task = await service.create_task(data)
    return TaskResponse.model_validate(task)


@app.get(
    "/tasks/{task_id}",
    response_model=TaskResponse,
    responses={404: {"model": ErrorResponse}},
)
async def get_task(
    task_id: int,
    service: TaskService = Depends(get_task_service),
) -> TaskResponse:
    task = await service.get_task(task_id)
    return TaskResponse.model_validate(task)


@app.get("/tasks", response_model=TaskListResponse)
async def list_tasks(
    task_status: TaskStatus | None = Query(None, alias="status"),
    priority: TaskPriority | None = None,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    service: TaskService = Depends(get_task_service),
) -> TaskListResponse:
    tasks, total = await service.list_tasks(task_status, priority, skip, limit)
    return TaskListResponse(
        items=[TaskResponse.model_validate(t) for t in tasks],
        total=total,
    )


@app.put(
    "/tasks/{task_id}",
    response_model=TaskResponse,
    responses={
        400: {"model": ErrorResponse},
        404: {"model": ErrorResponse},
    },
)
async def update_task(
    task_id: int,
    data: TaskUpdate,
    service: TaskService = Depends(get_task_service),
) -> TaskResponse:
    task = await service.update_task(task_id, data)
    return TaskResponse.model_validate(task)


@app.delete(
    "/tasks/{task_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    responses={404: {"model": ErrorResponse}},
)
async def delete_task(
    task_id: int,
    service: TaskService = Depends(get_task_service),
) -> None:
    await service.delete_task(task_id)


@app.post(
    "/tasks/{task_id}/complete",
    response_model=TaskResponse,
    responses={
        400: {"model": ErrorResponse},
        404: {"model": ErrorResponse},
    },
)
async def mark_task_completed(
    task_id: int,
    service: TaskService = Depends(get_task_service),
) -> TaskResponse:
    task = await service.mark_as_completed(task_id)
    return TaskResponse.model_validate(task)


@app.get("/health")
async def health_check() -> dict[str, str]:
    return {"status": "healthy"}
