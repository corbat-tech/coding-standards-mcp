"""Application layer protocols (interfaces)."""
from abc import abstractmethod
from contextlib import asynccontextmanager
from typing import AsyncIterator, Protocol, Sequence

from domain.entities import UserId
from domain.protocols import UserRepository


class UnitOfWork(Protocol):
    """Unit of Work pattern protocol.

    Manages transactions and provides access to repositories.
    Ensures atomicity of operations across multiple repositories.
    """

    users: UserRepository

    @abstractmethod
    async def commit(self) -> None:
        """Commit the current transaction."""
        ...

    @abstractmethod
    async def rollback(self) -> None:
        """Rollback the current transaction."""
        ...

    @abstractmethod
    @asynccontextmanager
    async def transaction(self) -> AsyncIterator[None]:
        """Context manager for transaction handling.

        Usage:
            async with uow.transaction():
                await uow.users.add(user)
                # Auto-commits on success, rollbacks on exception
        """
        ...


class UserService(Protocol):
    """User service protocol defining business operations."""

    @abstractmethod
    async def create_user(
        self, name: str, email: str
    ) -> "UserResponseDTO":
        """Create a new user.

        Args:
            name: User's name
            email: User's email address

        Returns:
            Created user response DTO

        Raises:
            UserAlreadyExistsException: If email exists
            InvalidUserDataException: If data is invalid
        """
        ...

    @abstractmethod
    async def get_user(self, user_id: UserId) -> "UserResponseDTO":
        """Get a user by ID.

        Args:
            user_id: The user's unique identifier

        Returns:
            User response DTO

        Raises:
            UserNotFoundException: If user not found
        """
        ...

    @abstractmethod
    async def update_user(
        self,
        user_id: UserId,
        name: str | None = None,
        email: str | None = None,
    ) -> "UserResponseDTO":
        """Update a user's information.

        Args:
            user_id: The user's unique identifier
            name: Optional new name
            email: Optional new email

        Returns:
            Updated user response DTO

        Raises:
            UserNotFoundException: If user not found
            UserAlreadyExistsException: If new email exists
        """
        ...

    @abstractmethod
    async def delete_user(self, user_id: UserId) -> None:
        """Delete a user.

        Args:
            user_id: The user's unique identifier

        Raises:
            UserNotFoundException: If user not found
        """
        ...

    @abstractmethod
    async def list_users(
        self, skip: int = 0, limit: int = 100
    ) -> "PaginatedUsersDTO":
        """List users with pagination.

        Args:
            skip: Number of records to skip
            limit: Maximum records to return

        Returns:
            Paginated users response
        """
        ...


# Forward references for type hints
from application.dtos import UserResponseDTO, PaginatedUsersDTO  # noqa: E402, F811
