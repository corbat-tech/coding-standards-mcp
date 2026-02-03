from sqlalchemy.orm import Session
from typing import List, Optional
from .models import Task
from .schemas import TaskCreate, TaskUpdate
from .exceptions import TaskNotFoundException


class TaskService:
    def __init__(self, db: Session):
        self.db = db

    def create(self, task_data: TaskCreate) -> Task:
        task = Task(
            title=task_data.title,
            description=task_data.description,
            priority=task_data.priority.value
        )
        self.db.add(task)
        self.db.commit()
        self.db.refresh(task)
        return task

    def get_by_id(self, task_id: int) -> Task:
        task = self.db.query(Task).filter(Task.id == task_id).first()
        if not task:
            raise TaskNotFoundException(task_id)
        return task

    def get_all(self, completed: Optional[bool] = None) -> List[Task]:
        query = self.db.query(Task)
        if completed is not None:
            query = query.filter(Task.completed == completed)
        return query.all()

    def update(self, task_id: int, task_data: TaskUpdate) -> Task:
        task = self.get_by_id(task_id)

        update_data = task_data.model_dump(exclude_unset=True)
        if "priority" in update_data and update_data["priority"]:
            update_data["priority"] = update_data["priority"].value

        for field, value in update_data.items():
            setattr(task, field, value)

        self.db.commit()
        self.db.refresh(task)
        return task

    def delete(self, task_id: int) -> None:
        task = self.get_by_id(task_id)
        self.db.delete(task)
        self.db.commit()
