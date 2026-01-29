"""Pytest fixtures for async tests."""
import asyncio
from typing import AsyncIterator
from uuid import uuid4

import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from domain.entities import User, UserId
from infrastructure.database import Base
from infrastructure.unit_of_work import SQLAlchemyUnitOfWork


@pytest.fixture(scope="session")
def event_loop():
    """Create event loop for async tests."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest_asyncio.fixture
async def async_engine() -> AsyncIterator[AsyncEngine]:
    """Create async engine with in-memory SQLite."""
    engine = create_async_engine(
        "sqlite+aiosqlite:///:memory:",
        echo=False,
        future=True,
    )
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    await engine.dispose()


@pytest_asyncio.fixture
async def session_factory(
    async_engine: AsyncEngine,
) -> async_sessionmaker[AsyncSession]:
    """Create session factory."""
    return async_sessionmaker(
        async_engine,
        class_=AsyncSession,
        expire_on_commit=False,
        autoflush=False,
    )


@pytest_asyncio.fixture
async def unit_of_work(
    session_factory: async_sessionmaker[AsyncSession],
) -> SQLAlchemyUnitOfWork:
    """Create unit of work for testing."""
    return SQLAlchemyUnitOfWork(session_factory)


@pytest.fixture
def sample_user() -> User:
    """Create a sample user for testing."""
    return User(
        id=UserId(uuid4()),
        name="John Doe",
        email="john@example.com",
    )


@pytest.fixture
def sample_user_data() -> dict:
    """Create sample user data for testing."""
    return {
        "name": "Jane Doe",
        "email": "jane@example.com",
    }
