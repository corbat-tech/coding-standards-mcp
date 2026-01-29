"""Unit of Work implementation using SQLAlchemy."""
from contextlib import asynccontextmanager
from typing import AsyncIterator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from application.protocols import UnitOfWork
from domain.protocols import UserRepository
from infrastructure.repositories import SQLAlchemyUserRepository


class SQLAlchemyUnitOfWork(UnitOfWork):
    """SQLAlchemy implementation of the Unit of Work pattern.

    Manages database transactions and provides access to repositories.
    """

    def __init__(
        self, session_factory: async_sessionmaker[AsyncSession]
    ) -> None:
        """Initialize with session factory.

        Args:
            session_factory: Factory for creating async sessions
        """
        self._session_factory = session_factory
        self._session: AsyncSession | None = None

    @property
    def users(self) -> UserRepository:
        """Get the user repository."""
        if not self._session:
            raise RuntimeError("Unit of Work not initialized. Use transaction context.")
        return SQLAlchemyUserRepository(self._session)

    async def commit(self) -> None:
        """Commit the current transaction."""
        if self._session:
            await self._session.commit()

    async def rollback(self) -> None:
        """Rollback the current transaction."""
        if self._session:
            await self._session.rollback()

    @asynccontextmanager
    async def transaction(self) -> AsyncIterator[None]:
        """Context manager for transaction handling."""
        self._session = self._session_factory()
        try:
            yield
            await self.commit()
        except Exception:
            await self.rollback()
            raise
        finally:
            await self._session.close()
            self._session = None
