"""Data Transfer Objects for the application layer."""
from dataclasses import dataclass
from datetime import datetime
from typing import Sequence
from uuid import UUID

from domain.entities import User


@dataclass(frozen=True)
class CreateUserDTO:
    """DTO for creating a user."""

    name: str
    email: str


@dataclass(frozen=True)
class UpdateUserDTO:
    """DTO for updating a user."""

    name: str | None = None
    email: str | None = None


@dataclass(frozen=True)
class UserResponseDTO:
    """DTO for user response."""

    id: UUID
    name: str
    email: str
    is_active: bool
    created_at: datetime
    updated_at: datetime | None

    @classmethod
    def from_entity(cls, user: User) -> "UserResponseDTO":
        """Create DTO from domain entity."""
        return cls(
            id=user.id,
            name=user.name,
            email=user.email,
            is_active=user.is_active,
            created_at=user.created_at,
            updated_at=user.updated_at,
        )


@dataclass(frozen=True)
class PaginatedUsersDTO:
    """DTO for paginated user list response."""

    items: Sequence[UserResponseDTO]
    total: int
    skip: int
    limit: int

    @property
    def has_more(self) -> bool:
        """Check if there are more items."""
        return self.skip + len(self.items) < self.total
