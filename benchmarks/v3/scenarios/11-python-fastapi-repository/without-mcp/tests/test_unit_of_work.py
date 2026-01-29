"""Tests for Unit of Work pattern."""
import pytest
import pytest_asyncio

from domain.entities import User
from domain.exceptions import InvalidUserDataException
from infrastructure.unit_of_work import SQLAlchemyUnitOfWork


class TestSQLAlchemyUnitOfWork:
    """Test cases for SQLAlchemy Unit of Work."""

    @pytest.mark.asyncio
    async def test_should_commit_on_success(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Unit of work should commit on successful transaction."""
        user = User(name="Commit User", email="commit@example.com")

        async with unit_of_work.transaction():
            await unit_of_work.users.add(user)

        # Verify user was committed
        async with unit_of_work.transaction():
            result = await unit_of_work.users.get_by_id(user.id)

        assert result is not None
        assert result.email == "commit@example.com"

    @pytest.mark.asyncio
    async def test_should_rollback_on_error(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Unit of work should rollback on exception."""
        user1 = User(name="Rollback User 1", email="rollback1@example.com")
        user2_invalid_email = "invalid-email"  # This will cause validation error

        async with unit_of_work.transaction():
            await unit_of_work.users.add(user1)

        # Attempt to create user with invalid data should not affect user1
        with pytest.raises(InvalidUserDataException):
            async with unit_of_work.transaction():
                await unit_of_work.users.add(user1)  # re-add ok
                # Force validation error
                User(name="Invalid", email=user2_invalid_email)

        # Verify first user still exists
        async with unit_of_work.transaction():
            result = await unit_of_work.users.get_by_id(user1.id)

        assert result is not None

    @pytest.mark.asyncio
    async def test_should_raise_when_accessing_users_outside_transaction(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Unit of work should raise when accessing users outside transaction."""
        with pytest.raises(RuntimeError) as exc_info:
            _ = unit_of_work.users

        assert "Unit of Work not initialized" in str(exc_info.value)

    @pytest.mark.asyncio
    async def test_should_provide_repository_within_transaction(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Unit of work should provide repository within transaction context."""
        async with unit_of_work.transaction():
            repo = unit_of_work.users
            assert repo is not None
            count = await repo.count()
            assert count >= 0
