"""FastAPI dependency injection."""
from functools import lru_cache
from typing import AsyncIterator

from sqlalchemy.ext.asyncio import AsyncEngine, async_sessionmaker, AsyncSession

from application.services import UserServiceImpl
from infrastructure.database import get_async_engine, get_async_session_factory
from infrastructure.unit_of_work import SQLAlchemyUnitOfWork
from api.config import get_settings


@lru_cache
def get_engine() -> AsyncEngine:
    """Get cached database engine."""
    settings = get_settings()
    return get_async_engine(settings.database_url)


@lru_cache
def get_session_factory() -> async_sessionmaker[AsyncSession]:
    """Get cached session factory."""
    return get_async_session_factory(get_engine())


def get_unit_of_work() -> SQLAlchemyUnitOfWork:
    """Get unit of work instance for dependency injection."""
    return SQLAlchemyUnitOfWork(get_session_factory())


def get_user_service() -> UserServiceImpl:
    """Get user service instance for dependency injection."""
    uow = get_unit_of_work()
    return UserServiceImpl(uow)
