"""SQLAlchemy repository implementations."""
from typing import Sequence

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from domain.entities import User, UserId
from domain.protocols import UserRepository
from infrastructure.database import UserModel


class SQLAlchemyUserRepository(UserRepository):
    """SQLAlchemy implementation of the user repository."""

    def __init__(self, session: AsyncSession) -> None:
        """Initialize repository with database session.

        Args:
            session: SQLAlchemy async session
        """
        self._session = session

    async def add(self, user: User) -> User:
        """Add a new user to the database."""
        model = self._to_model(user)
        self._session.add(model)
        await self._session.flush()
        return self._to_entity(model)

    async def get_by_id(self, user_id: UserId) -> User | None:
        """Retrieve a user by their ID."""
        stmt = select(UserModel).where(UserModel.id == user_id)
        result = await self._session.execute(stmt)
        model = result.scalar_one_or_none()
        return self._to_entity(model) if model else None

    async def get_by_email(self, email: str) -> User | None:
        """Retrieve a user by their email."""
        stmt = select(UserModel).where(UserModel.email == email)
        result = await self._session.execute(stmt)
        model = result.scalar_one_or_none()
        return self._to_entity(model) if model else None

    async def update(self, user: User) -> User:
        """Update an existing user."""
        stmt = select(UserModel).where(UserModel.id == user.id)
        result = await self._session.execute(stmt)
        model = result.scalar_one_or_none()

        if model:
            model.name = user.name
            model.email = user.email
            model.is_active = user.is_active
            model.updated_at = user.updated_at
            await self._session.flush()
            return self._to_entity(model)

        return user

    async def delete(self, user_id: UserId) -> None:
        """Delete a user by ID."""
        stmt = select(UserModel).where(UserModel.id == user_id)
        result = await self._session.execute(stmt)
        model = result.scalar_one_or_none()
        if model:
            await self._session.delete(model)
            await self._session.flush()

    async def list_all(
        self, skip: int = 0, limit: int = 100
    ) -> Sequence[User]:
        """List all users with pagination."""
        stmt = (
            select(UserModel)
            .order_by(UserModel.created_at.desc())
            .offset(skip)
            .limit(limit)
        )
        result = await self._session.execute(stmt)
        models = result.scalars().all()
        return [self._to_entity(m) for m in models]

    async def count(self) -> int:
        """Count total number of users."""
        stmt = select(func.count()).select_from(UserModel)
        result = await self._session.execute(stmt)
        return result.scalar() or 0

    def _to_model(self, user: User) -> UserModel:
        """Convert domain entity to SQLAlchemy model."""
        return UserModel(
            id=user.id,
            email=user.email,
            name=user.name,
            is_active=user.is_active,
            created_at=user.created_at,
            updated_at=user.updated_at,
        )

    def _to_entity(self, model: UserModel) -> User:
        """Convert SQLAlchemy model to domain entity."""
        return User(
            id=UserId(model.id),
            email=model.email,
            name=model.name,
            is_active=model.is_active,
            created_at=model.created_at,
            updated_at=model.updated_at,
        )
