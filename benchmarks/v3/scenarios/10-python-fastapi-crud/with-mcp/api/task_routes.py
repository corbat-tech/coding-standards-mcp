"""
Task API Routes.

Defines REST endpoints for task management.
"""
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status, Query

from schemas.task_schema import (
    TaskCreate,
    TaskUpdate,
    TaskResponse,
    TaskListResponse
)
from services.task_service import TaskService
from api.dependencies import get_task_service
from core.exceptions import TaskNotFoundException

router = APIRouter(prefix="/api/v1/tasks", tags=["tasks"])


@router.post(
    "",
    response_model=TaskResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new task"
)
def create_task(
    task_data: TaskCreate,
    service: TaskService = Depends(get_task_service)
) -> TaskResponse:
    """Create a new task."""
    return service.create_task(task_data)


@router.get(
    "/{task_id}",
    response_model=TaskResponse,
    summary="Get task by ID"
)
def get_task(
    task_id: int,
    service: TaskService = Depends(get_task_service)
) -> TaskResponse:
    """Get a task by its ID."""
    try:
        return service.get_task(task_id)
    except TaskNotFoundException as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=e.message
        )


@router.get(
    "",
    response_model=TaskListResponse,
    summary="List all tasks"
)
def list_tasks(
    page: int = Query(1, ge=1, description="Page number"),
    page_size: int = Query(10, ge=1, le=100, description="Items per page"),
    status: Optional[str] = Query(None, description="Filter by status"),
    service: TaskService = Depends(get_task_service)
) -> TaskListResponse:
    """Get paginated list of tasks."""
    return service.get_tasks(page=page, page_size=page_size, status=status)


@router.put(
    "/{task_id}",
    response_model=TaskResponse,
    summary="Update a task"
)
def update_task(
    task_id: int,
    task_data: TaskUpdate,
    service: TaskService = Depends(get_task_service)
) -> TaskResponse:
    """Update an existing task."""
    try:
        return service.update_task(task_id, task_data)
    except TaskNotFoundException as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=e.message
        )


@router.delete(
    "/{task_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a task"
)
def delete_task(
    task_id: int,
    service: TaskService = Depends(get_task_service)
) -> None:
    """Delete a task by its ID."""
    try:
        service.delete_task(task_id)
    except TaskNotFoundException as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=e.message
        )
