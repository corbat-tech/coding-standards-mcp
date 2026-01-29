"""Tests for application services."""
import pytest
import pytest_asyncio
from uuid import uuid4

from domain.entities import UserId
from domain.exceptions import (
    UserNotFoundException,
    UserAlreadyExistsException,
)
from application.services import UserServiceImpl
from infrastructure.unit_of_work import SQLAlchemyUnitOfWork


class TestUserService:
    """Test cases for user service."""

    @pytest.mark.asyncio
    async def test_should_create_user_when_valid_data(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should create user with valid data."""
        service = UserServiceImpl(unit_of_work)

        result = await service.create_user(
            name="John Doe",
            email="service1@example.com",
        )

        assert result.name == "John Doe"
        assert result.email == "service1@example.com"
        assert result.is_active is True

    @pytest.mark.asyncio
    async def test_should_raise_user_already_exists_when_email_duplicate(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should raise exception for duplicate email."""
        service = UserServiceImpl(unit_of_work)

        await service.create_user(
            name="John Doe",
            email="duplicate@example.com",
        )

        with pytest.raises(UserAlreadyExistsException) as exc_info:
            await service.create_user(
                name="Jane Doe",
                email="duplicate@example.com",
            )

        assert exc_info.value.code == "USER_ALREADY_EXISTS"

    @pytest.mark.asyncio
    async def test_should_get_user_by_id_when_exists(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should return user by ID."""
        service = UserServiceImpl(unit_of_work)

        created = await service.create_user(
            name="John Doe",
            email="getbyid@example.com",
        )

        result = await service.get_user(UserId(created.id))

        assert result.id == created.id
        assert result.name == "John Doe"

    @pytest.mark.asyncio
    async def test_should_raise_user_not_found_when_id_invalid(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should raise exception for non-existent user."""
        service = UserServiceImpl(unit_of_work)

        with pytest.raises(UserNotFoundException) as exc_info:
            await service.get_user(UserId(uuid4()))

        assert exc_info.value.code == "USER_NOT_FOUND"

    @pytest.mark.asyncio
    async def test_should_update_user_when_exists(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should update existing user."""
        service = UserServiceImpl(unit_of_work)

        created = await service.create_user(
            name="John Doe",
            email="update@example.com",
        )

        result = await service.update_user(
            user_id=UserId(created.id),
            name="Jane Doe",
        )

        assert result.name == "Jane Doe"
        assert result.email == "update@example.com"

    @pytest.mark.asyncio
    async def test_should_raise_when_update_with_duplicate_email(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should raise exception when updating to existing email."""
        service = UserServiceImpl(unit_of_work)

        await service.create_user(
            name="John Doe",
            email="existing@example.com",
        )

        created = await service.create_user(
            name="Jane Doe",
            email="toupdate@example.com",
        )

        with pytest.raises(UserAlreadyExistsException):
            await service.update_user(
                user_id=UserId(created.id),
                email="existing@example.com",
            )

    @pytest.mark.asyncio
    async def test_should_delete_user_when_exists(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should delete existing user."""
        service = UserServiceImpl(unit_of_work)

        created = await service.create_user(
            name="John Doe",
            email="delete@example.com",
        )

        await service.delete_user(UserId(created.id))

        with pytest.raises(UserNotFoundException):
            await service.get_user(UserId(created.id))

    @pytest.mark.asyncio
    async def test_should_raise_when_delete_nonexistent_user(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should raise exception when deleting non-existent user."""
        service = UserServiceImpl(unit_of_work)

        with pytest.raises(UserNotFoundException):
            await service.delete_user(UserId(uuid4()))

    @pytest.mark.asyncio
    async def test_should_list_users_with_pagination(
        self, unit_of_work: SQLAlchemyUnitOfWork
    ) -> None:
        """Service should list users with pagination."""
        service = UserServiceImpl(unit_of_work)

        for i in range(5):
            await service.create_user(
                name=f"List User {i}",
                email=f"list{i}@example.com",
            )

        result = await service.list_users(skip=0, limit=3)

        assert len(result.items) == 3
        assert result.total >= 5
        assert result.has_more is True
