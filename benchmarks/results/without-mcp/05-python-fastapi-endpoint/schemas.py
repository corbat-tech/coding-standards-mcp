from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, field_validator
from models import TaskStatus, TaskPriority


class TaskBase(BaseModel):
    title: str = Field(..., min_length=1, max_length=200)
    description: Optional[str] = None
    priority: TaskPriority = TaskPriority.MEDIUM
    due_date: Optional[datetime] = None


class TaskCreate(TaskBase):
    @field_validator('due_date')
    @classmethod
    def due_date_must_be_in_future(cls, v: Optional[datetime]) -> Optional[datetime]:
        if v is not None and v <= datetime.utcnow():
            raise ValueError('due_date must be in the future')
        return v


class TaskUpdate(BaseModel):
    title: Optional[str] = Field(None, min_length=1, max_length=200)
    description: Optional[str] = None
    status: Optional[TaskStatus] = None
    priority: Optional[TaskPriority] = None
    due_date: Optional[datetime] = None

    @field_validator('due_date')
    @classmethod
    def due_date_must_be_in_future(cls, v: Optional[datetime]) -> Optional[datetime]:
        if v is not None and v <= datetime.utcnow():
            raise ValueError('due_date must be in the future')
        return v


class TaskResponse(BaseModel):
    id: str
    title: str
    description: Optional[str]
    status: TaskStatus
    priority: TaskPriority
    due_date: Optional[datetime]
    created_at: datetime
    updated_at: Optional[datetime]

    class Config:
        from_attributes = True
