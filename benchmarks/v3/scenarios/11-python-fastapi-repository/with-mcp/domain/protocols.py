"""Repository protocols (interfaces) for the domain layer."""
from abc import abstractmethod
from typing import Protocol, Sequence

from domain.entities import User, UserId


class UserRepository(Protocol):
    """Protocol defining the user repository interface.

    This is the domain's port for persistence operations.
    Infrastructure layer provides the implementation.
    """

    @abstractmethod
    async def add(self, user: User) -> User:
        """Add a new user to the repository.

        Args:
            user: The user entity to persist

        Returns:
            The persisted user with generated ID

        Raises:
            UserAlreadyExistsException: If email already exists
        """
        ...

    @abstractmethod
    async def get_by_id(self, user_id: UserId) -> User | None:
        """Retrieve a user by their ID.

        Args:
            user_id: The unique user identifier

        Returns:
            The user if found, None otherwise
        """
        ...

    @abstractmethod
    async def get_by_email(self, email: str) -> User | None:
        """Retrieve a user by their email.

        Args:
            email: The user's email address

        Returns:
            The user if found, None otherwise
        """
        ...

    @abstractmethod
    async def update(self, user: User) -> User:
        """Update an existing user.

        Args:
            user: The user entity with updated values

        Returns:
            The updated user

        Raises:
            UserNotFoundException: If user does not exist
        """
        ...

    @abstractmethod
    async def delete(self, user_id: UserId) -> None:
        """Delete a user by ID.

        Args:
            user_id: The unique user identifier

        Raises:
            UserNotFoundException: If user does not exist
        """
        ...

    @abstractmethod
    async def list_all(
        self, skip: int = 0, limit: int = 100
    ) -> Sequence[User]:
        """List all users with pagination.

        Args:
            skip: Number of records to skip
            limit: Maximum number of records to return

        Returns:
            Sequence of users
        """
        ...

    @abstractmethod
    async def count(self) -> int:
        """Count total number of users.

        Returns:
            Total user count
        """
        ...
