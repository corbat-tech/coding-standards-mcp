from typing import List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from ..domain.exceptions import TaskNotFoundError, InvalidTaskInputError
from ..application.task_service import TaskService
from ..infrastructure.database import get_session
from ..infrastructure.repository import SqlAlchemyTaskRepository
from ..infrastructure.adapters import UuidGenerator, SystemClock
from .schemas import CreateTaskRequest, UpdateTaskRequest, TaskResponse

router = APIRouter(prefix="/tasks", tags=["tasks"])


def get_task_service(session: AsyncSession = Depends(get_session)) -> TaskService:
    repository = SqlAlchemyTaskRepository(session)
    return TaskService(repository, UuidGenerator(), SystemClock())


@router.post("", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
async def create_task(
    request: CreateTaskRequest,
    service: TaskService = Depends(get_task_service)
):
    try:
        task = await service.create_task(request.title, request.description)
        return TaskResponse.model_validate(task)
    except InvalidTaskInputError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("", response_model=List[TaskResponse])
async def list_tasks(service: TaskService = Depends(get_task_service)):
    tasks = await service.list_tasks()
    return [TaskResponse.model_validate(t) for t in tasks]


@router.get("/{task_id}", response_model=TaskResponse)
async def get_task(task_id: str, service: TaskService = Depends(get_task_service)):
    try:
        task = await service.get_task(task_id)
        return TaskResponse.model_validate(task)
    except TaskNotFoundError:
        raise HTTPException(status_code=404, detail=f"Task not found: {task_id}")


@router.put("/{task_id}", response_model=TaskResponse)
async def update_task(
    task_id: str,
    request: UpdateTaskRequest,
    service: TaskService = Depends(get_task_service)
):
    try:
        task = await service.update_task(
            task_id,
            title=request.title,
            description=request.description,
            status=request.status
        )
        return TaskResponse.model_validate(task)
    except TaskNotFoundError:
        raise HTTPException(status_code=404, detail=f"Task not found: {task_id}")
    except InvalidTaskInputError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_task(task_id: str, service: TaskService = Depends(get_task_service)):
    try:
        await service.delete_task(task_id)
    except TaskNotFoundError:
        raise HTTPException(status_code=404, detail=f"Task not found: {task_id}")
