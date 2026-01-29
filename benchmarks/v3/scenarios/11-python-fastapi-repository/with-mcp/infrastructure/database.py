"""Database configuration and SQLAlchemy models."""
from datetime import datetime
from typing import AsyncIterator
from uuid import UUID

from sqlalchemy import Boolean, DateTime, String
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    """SQLAlchemy declarative base."""

    pass


class UserModel(Base):
    """SQLAlchemy user model for persistence."""

    __tablename__ = "users"

    id: Mapped[UUID] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True)
    name: Mapped[str] = mapped_column(String(255))
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


def get_async_engine(database_url: str) -> AsyncEngine:
    """Create async SQLAlchemy engine.

    Args:
        database_url: Database connection URL

    Returns:
        Async SQLAlchemy engine
    """
    return create_async_engine(
        database_url,
        echo=False,
        future=True,
    )


def get_async_session_factory(
    engine: AsyncEngine,
) -> async_sessionmaker[AsyncSession]:
    """Create async session factory.

    Args:
        engine: Async SQLAlchemy engine

    Returns:
        Session factory for creating async sessions
    """
    return async_sessionmaker(
        engine,
        class_=AsyncSession,
        expire_on_commit=False,
        autoflush=False,
    )
