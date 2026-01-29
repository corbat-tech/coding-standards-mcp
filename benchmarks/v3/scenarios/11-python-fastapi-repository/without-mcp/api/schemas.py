"""Pydantic schemas for API requests and responses."""
from datetime import datetime
from typing import Sequence
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field


class CreateUserRequest(BaseModel):
    """Request schema for creating a user."""

    name: str = Field(..., min_length=1, max_length=255)
    email: EmailStr


class UpdateUserRequest(BaseModel):
    """Request schema for updating a user."""

    name: str | None = Field(None, min_length=1, max_length=255)
    email: EmailStr | None = None


class UserResponse(BaseModel):
    """Response schema for a user."""

    id: UUID
    name: str
    email: str
    is_active: bool
    created_at: datetime
    updated_at: datetime | None

    class Config:
        """Pydantic configuration."""

        from_attributes = True


class PaginatedUsersResponse(BaseModel):
    """Response schema for paginated user list."""

    items: Sequence[UserResponse]
    total: int
    skip: int
    limit: int
    has_more: bool

    class Config:
        """Pydantic configuration."""

        from_attributes = True


class ErrorResponse(BaseModel):
    """Response schema for errors."""

    code: str
    message: str
    details: dict | None = None
