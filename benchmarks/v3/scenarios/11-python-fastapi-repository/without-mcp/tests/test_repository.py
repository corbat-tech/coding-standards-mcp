"""Tests for repository implementations."""
import pytest
import pytest_asyncio
from uuid import uuid4

from domain.entities import User, UserId
from infrastructure.repositories import SQLAlchemyUserRepository
from infrastructure.unit_of_work import SQLAlchemyUnitOfWork


class TestSQLAlchemyUserRepository:
    """Test cases for SQLAlchemy user repository."""

    @pytest.mark.asyncio
    async def test_should_add_user_when_valid(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should persist user."""
        user = User(name="John Doe", email="john@example.com")

        async with unit_of_work.transaction():
            result = await unit_of_work.users.add(user)

        assert result.id == user.id
        assert result.name == "John Doe"
        assert result.email == "john@example.com"

    @pytest.mark.asyncio
    async def test_should_get_user_by_id_when_exists(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should retrieve user by ID."""
        user = User(name="John Doe", email="john2@example.com")

        async with unit_of_work.transaction():
            await unit_of_work.users.add(user)

        async with unit_of_work.transaction():
            result = await unit_of_work.users.get_by_id(user.id)

        assert result is not None
        assert result.id == user.id

    @pytest.mark.asyncio
    async def test_should_return_none_when_user_not_found(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should return None for non-existent user."""
        async with unit_of_work.transaction():
            result = await unit_of_work.users.get_by_id(UserId(uuid4()))

        assert result is None

    @pytest.mark.asyncio
    async def test_should_get_user_by_email_when_exists(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should retrieve user by email."""
        user = User(name="John Doe", email="john3@example.com")

        async with unit_of_work.transaction():
            await unit_of_work.users.add(user)

        async with unit_of_work.transaction():
            result = await unit_of_work.users.get_by_email("john3@example.com")

        assert result is not None
        assert result.email == "john3@example.com"

    @pytest.mark.asyncio
    async def test_should_update_user_when_exists(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should update existing user."""
        user = User(name="John Doe", email="john4@example.com")

        async with unit_of_work.transaction():
            await unit_of_work.users.add(user)

        updated = user.update(name="Jane Doe")

        async with unit_of_work.transaction():
            result = await unit_of_work.users.update(updated)

        assert result.name == "Jane Doe"
        assert result.updated_at is not None

    @pytest.mark.asyncio
    async def test_should_delete_user_when_exists(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should delete existing user."""
        user = User(name="John Doe", email="john5@example.com")

        async with unit_of_work.transaction():
            await unit_of_work.users.add(user)

        async with unit_of_work.transaction():
            await unit_of_work.users.delete(user.id)

        async with unit_of_work.transaction():
            result = await unit_of_work.users.get_by_id(user.id)

        assert result is None

    @pytest.mark.asyncio
    async def test_should_list_users_with_pagination(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should list users with pagination."""
        for i in range(5):
            user = User(name=f"User {i}", email=f"user{i}@example.com")
            async with unit_of_work.transaction():
                await unit_of_work.users.add(user)

        async with unit_of_work.transaction():
            result = await unit_of_work.users.list_all(skip=0, limit=3)

        assert len(result) == 3

    @pytest.mark.asyncio
    async def test_should_count_users(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Repository should count total users."""
        for i in range(3):
            user = User(name=f"Count User {i}", email=f"count{i}@example.com")
            async with unit_of_work.transaction():
                await unit_of_work.users.add(user)

        async with unit_of_work.transaction():
            count = await unit_of_work.users.count()

        assert count >= 3
