"""Task CRUD endpoints."""

from fastapi import APIRouter, status, Query
from typing import Optional
import math

from app.schemas import (
    TaskCreate,
    TaskUpdate,
    TaskResponse,
    TaskListResponse,
    TaskStatus,
    TaskPriority,
)
from app.dependencies import TaskRepo
from app.exceptions import TaskNotFoundError

router = APIRouter(prefix="/tasks", tags=["tasks"])


@router.get("", response_model=TaskListResponse, summary="Get all tasks")
def get_tasks(
    repo: TaskRepo,
    page: int = Query(1, ge=1, description="Page number"),
    page_size: int = Query(10, ge=1, le=100, description="Items per page"),
    status: Optional[TaskStatus] = Query(None, description="Filter by status"),
    priority: Optional[TaskPriority] = Query(None, description="Filter by priority"),
) -> TaskListResponse:
    """
    Retrieve all tasks with pagination and optional filtering.

    - **page**: Page number (starting from 1)
    - **page_size**: Number of items per page (max 100)
    - **status**: Optional filter by task status
    - **priority**: Optional filter by task priority
    """
    skip = (page - 1) * page_size
    tasks = repo.get_all(skip=skip, limit=page_size, status=status, priority=priority)
    total = repo.count(status=status, priority=priority)
    pages = math.ceil(total / page_size) if total > 0 else 1

    return TaskListResponse(
        items=tasks,
        total=total,
        page=page,
        page_size=page_size,
        pages=pages,
    )


@router.get("/{task_id}", response_model=TaskResponse, summary="Get a task by ID")
def get_task(task_id: int, repo: TaskRepo) -> TaskResponse:
    """
    Retrieve a specific task by its ID.

    - **task_id**: The ID of the task to retrieve
    """
    task = repo.get_by_id(task_id)
    if not task:
        raise TaskNotFoundError(task_id)
    return task


@router.post(
    "",
    response_model=TaskResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new task",
)
def create_task(task_data: TaskCreate, repo: TaskRepo) -> TaskResponse:
    """
    Create a new task.

    - **title**: Task title (required)
    - **description**: Task description (optional)
    - **status**: Task status (default: pending)
    - **priority**: Task priority (default: medium)
    - **completed**: Whether the task is completed (default: false)
    """
    return repo.create(task_data)


@router.put("/{task_id}", response_model=TaskResponse, summary="Update a task")
def update_task(
    task_id: int,
    task_data: TaskUpdate,
    repo: TaskRepo,
) -> TaskResponse:
    """
    Update an existing task.

    - **task_id**: The ID of the task to update
    - Only provided fields will be updated
    """
    task = repo.update(task_id, task_data)
    if not task:
        raise TaskNotFoundError(task_id)
    return task


@router.delete(
    "/{task_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a task",
)
def delete_task(task_id: int, repo: TaskRepo) -> None:
    """
    Delete a task by its ID.

    - **task_id**: The ID of the task to delete
    """
    deleted = repo.delete(task_id)
    if not deleted:
        raise TaskNotFoundError(task_id)
