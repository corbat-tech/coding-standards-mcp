from abc import ABC, abstractmethod
from datetime import datetime
from typing import Optional, List

from .task import Task


class TaskRepository(ABC):
    @abstractmethod
    async def find_by_id(self, task_id: str) -> Optional[Task]:
        pass

    @abstractmethod
    async def find_all(self) -> List[Task]:
        pass

    @abstractmethod
    async def save(self, task: Task) -> None:
        pass

    @abstractmethod
    async def delete(self, task_id: str) -> bool:
        pass


class IdGenerator(ABC):
    @abstractmethod
    def generate(self) -> str:
        pass


class Clock(ABC):
    @abstractmethod
    def now(self) -> datetime:
        pass
