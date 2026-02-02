from fastapi import FastAPI, Depends, status
from sqlalchemy.orm import Session
from typing import List, Optional

from .models import get_db, create_tables
from .schemas import TaskCreate, TaskUpdate, TaskResponse
from .service import TaskService

app = FastAPI(title="Task API")


@app.on_event("startup")
def startup():
    create_tables()


def get_task_service(db: Session = Depends(get_db)) -> TaskService:
    return TaskService(db)


@app.post("/tasks", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
def create_task(
    task_data: TaskCreate,
    service: TaskService = Depends(get_task_service)
):
    return service.create(task_data)


@app.get("/tasks", response_model=List[TaskResponse])
def get_tasks(
    completed: Optional[bool] = None,
    service: TaskService = Depends(get_task_service)
):
    return service.get_all(completed)


@app.get("/tasks/{task_id}", response_model=TaskResponse)
def get_task(
    task_id: int,
    service: TaskService = Depends(get_task_service)
):
    return service.get_by_id(task_id)


@app.put("/tasks/{task_id}", response_model=TaskResponse)
def update_task(
    task_id: int,
    task_data: TaskUpdate,
    service: TaskService = Depends(get_task_service)
):
    return service.update(task_id, task_data)


@app.delete("/tasks/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_task(
    task_id: int,
    service: TaskService = Depends(get_task_service)
):
    service.delete(task_id)
