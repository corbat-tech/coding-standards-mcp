import uuid
from datetime import datetime

from ..domain.ports import IdGenerator, Clock


class UuidGenerator(IdGenerator):
    def generate(self) -> str:
        return str(uuid.uuid4())


class SystemClock(Clock):
    def now(self) -> datetime:
        return datetime.utcnow()
