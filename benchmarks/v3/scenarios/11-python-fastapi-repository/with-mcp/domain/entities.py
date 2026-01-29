"""Domain entities for the user management system."""
from dataclasses import dataclass, field
from datetime import datetime
from typing import NewType
from uuid import UUID, uuid4

UserId = NewType("UserId", UUID)


@dataclass
class User:
    """User domain entity with validation."""

    email: str
    name: str
    id: UserId = field(default_factory=lambda: UserId(uuid4()))
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime | None = None
    is_active: bool = True

    def __post_init__(self) -> None:
        """Validate entity invariants."""
        self._validate_email()
        self._validate_name()

    def _validate_email(self) -> None:
        """Validate email format."""
        if not self.email or "@" not in self.email:
            from domain.exceptions import InvalidUserDataException

            raise InvalidUserDataException("Invalid email format")

    def _validate_name(self) -> None:
        """Validate name is not empty."""
        if not self.name or len(self.name.strip()) == 0:
            from domain.exceptions import InvalidUserDataException

            raise InvalidUserDataException("Name cannot be empty")

    def update(self, name: str | None = None, email: str | None = None) -> "User":
        """Create updated user with new values."""
        return User(
            id=self.id,
            email=email or self.email,
            name=name or self.name,
            created_at=self.created_at,
            updated_at=datetime.utcnow(),
            is_active=self.is_active,
        )

    def deactivate(self) -> "User":
        """Create deactivated user."""
        return User(
            id=self.id,
            email=self.email,
            name=self.name,
            created_at=self.created_at,
            updated_at=datetime.utcnow(),
            is_active=False,
        )
