from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Optional


class TaskStatus(Enum):
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"


@dataclass(frozen=True)
class Task:
    id: str
    title: str
    description: str
    status: TaskStatus
    created_at: datetime
    updated_at: datetime

    @staticmethod
    def create(
        id: str,
        title: str,
        description: str,
        created_at: datetime
    ) -> "Task":
        return Task(
            id=id,
            title=title.strip(),
            description=description.strip(),
            status=TaskStatus.PENDING,
            created_at=created_at,
            updated_at=created_at
        )

    def update(
        self,
        title: Optional[str] = None,
        description: Optional[str] = None,
        status: Optional[TaskStatus] = None,
        updated_at: Optional[datetime] = None
    ) -> "Task":
        return Task(
            id=self.id,
            title=title.strip() if title else self.title,
            description=description.strip() if description else self.description,
            status=status if status else self.status,
            created_at=self.created_at,
            updated_at=updated_at if updated_at else self.updated_at
        )
