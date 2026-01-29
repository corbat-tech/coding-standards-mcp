"""FastAPI routes for user management."""
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status

from application.services import UserServiceImpl
from domain.entities import UserId
from domain.exceptions import (
    DomainException,
    UserNotFoundException,
    UserAlreadyExistsException,
)
from api.dependencies import get_user_service
from api.schemas import (
    CreateUserRequest,
    UpdateUserRequest,
    UserResponse,
    PaginatedUsersResponse,
    ErrorResponse,
)

router = APIRouter(prefix="/users", tags=["users"])


def _handle_domain_exception(exc: DomainException) -> HTTPException:
    """Convert domain exceptions to HTTP exceptions."""
    status_map = {
        UserNotFoundException: status.HTTP_404_NOT_FOUND,
        UserAlreadyExistsException: status.HTTP_409_CONFLICT,
    }
    http_status = status_map.get(type(exc), status.HTTP_400_BAD_REQUEST)
    return HTTPException(
        status_code=http_status,
        detail={"code": exc.code, "message": exc.message},
    )


@router.post(
    "",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
    responses={409: {"model": ErrorResponse}},
)
async def create_user(
    request: CreateUserRequest,
    service: Annotated[UserServiceImpl, Depends(get_user_service)],
) -> UserResponse:
    """Create a new user."""
    try:
        result = await service.create_user(
            name=request.name,
            email=request.email,
        )
        return UserResponse(**result.__dict__)
    except DomainException as exc:
        raise _handle_domain_exception(exc)


@router.get(
    "/{user_id}",
    response_model=UserResponse,
    responses={404: {"model": ErrorResponse}},
)
async def get_user(
    user_id: UUID,
    service: Annotated[UserServiceImpl, Depends(get_user_service)],
) -> UserResponse:
    """Get a user by ID."""
    try:
        result = await service.get_user(UserId(user_id))
        return UserResponse(**result.__dict__)
    except DomainException as exc:
        raise _handle_domain_exception(exc)


@router.patch(
    "/{user_id}",
    response_model=UserResponse,
    responses={404: {"model": ErrorResponse}, 409: {"model": ErrorResponse}},
)
async def update_user(
    user_id: UUID,
    request: UpdateUserRequest,
    service: Annotated[UserServiceImpl, Depends(get_user_service)],
) -> UserResponse:
    """Update a user."""
    try:
        result = await service.update_user(
            user_id=UserId(user_id),
            name=request.name,
            email=request.email,
        )
        return UserResponse(**result.__dict__)
    except DomainException as exc:
        raise _handle_domain_exception(exc)


@router.delete(
    "/{user_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    responses={404: {"model": ErrorResponse}},
)
async def delete_user(
    user_id: UUID,
    service: Annotated[UserServiceImpl, Depends(get_user_service)],
) -> None:
    """Delete a user."""
    try:
        await service.delete_user(UserId(user_id))
    except DomainException as exc:
        raise _handle_domain_exception(exc)


@router.get("", response_model=PaginatedUsersResponse)
async def list_users(
    service: Annotated[UserServiceImpl, Depends(get_user_service)],
    skip: Annotated[int, Query(ge=0)] = 0,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
) -> PaginatedUsersResponse:
    """List users with pagination."""
    result = await service.list_users(skip=skip, limit=limit)
    return PaginatedUsersResponse(
        items=[UserResponse(**u.__dict__) for u in result.items],
        total=result.total,
        skip=result.skip,
        limit=result.limit,
        has_more=result.has_more,
    )
