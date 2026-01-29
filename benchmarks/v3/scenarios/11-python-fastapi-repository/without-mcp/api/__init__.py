# API layer - FastAPI routes and dependencies
from api.routes import router
from api.dependencies import get_user_service
from api.schemas import (
    CreateUserRequest,
    UpdateUserRequest,
    UserResponse,
    PaginatedUsersResponse,
    ErrorResponse,
)

__all__ = [
    "router",
    "get_user_service",
    "CreateUserRequest",
    "UpdateUserRequest",
    "UserResponse",
    "PaginatedUsersResponse",
    "ErrorResponse",
]
