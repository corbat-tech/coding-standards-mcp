"""Application services implementation."""
from domain.entities import User, UserId
from domain.exceptions import UserAlreadyExistsException, UserNotFoundException
from application.protocols import UnitOfWork
from application.dtos import UserResponseDTO, PaginatedUsersDTO


class UserServiceImpl:
    """User service implementation with unit of work pattern.

    This service orchestrates business operations and manages
    transactions through the unit of work.
    """

    def __init__(self, uow: UnitOfWork) -> None:
        """Initialize service with unit of work dependency.

        Args:
            uow: Unit of work for transaction management
        """
        self._uow = uow

    async def create_user(self, name: str, email: str) -> UserResponseDTO:
        """Create a new user."""
        async with self._uow.transaction():
            existing = await self._uow.users.get_by_email(email)
            if existing:
                raise UserAlreadyExistsException(email)

            user = User(name=name, email=email)
            created = await self._uow.users.add(user)
            return UserResponseDTO.from_entity(created)

    async def get_user(self, user_id: UserId) -> UserResponseDTO:
        """Get a user by ID."""
        user = await self._uow.users.get_by_id(user_id)
        if not user:
            raise UserNotFoundException(str(user_id))
        return UserResponseDTO.from_entity(user)

    async def update_user(
        self,
        user_id: UserId,
        name: str | None = None,
        email: str | None = None,
    ) -> UserResponseDTO:
        """Update a user's information."""
        async with self._uow.transaction():
            user = await self._uow.users.get_by_id(user_id)
            if not user:
                raise UserNotFoundException(str(user_id))

            if email and email != user.email:
                existing = await self._uow.users.get_by_email(email)
                if existing:
                    raise UserAlreadyExistsException(email)

            updated_user = user.update(name=name, email=email)
            saved = await self._uow.users.update(updated_user)
            return UserResponseDTO.from_entity(saved)

    async def delete_user(self, user_id: UserId) -> None:
        """Delete a user."""
        async with self._uow.transaction():
            user = await self._uow.users.get_by_id(user_id)
            if not user:
                raise UserNotFoundException(str(user_id))
            await self._uow.users.delete(user_id)

    async def list_users(
        self, skip: int = 0, limit: int = 100
    ) -> PaginatedUsersDTO:
        """List users with pagination."""
        users = await self._uow.users.list_all(skip=skip, limit=limit)
        total = await self._uow.users.count()

        return PaginatedUsersDTO(
            items=[UserResponseDTO.from_entity(u) for u in users],
            total=total,
            skip=skip,
            limit=limit,
        )
