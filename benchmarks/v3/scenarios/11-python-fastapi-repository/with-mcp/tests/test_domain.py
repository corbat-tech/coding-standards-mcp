"""Tests for domain layer entities and validation."""
import pytest
from uuid import uuid4

from domain.entities import User, UserId
from domain.exceptions import InvalidUserDataException


class TestUser:
    """Test cases for User entity."""

    def test_should_create_user_when_valid_data(self) -> None:
        """User should be created with valid data."""
        user = User(name="John Doe", email="john@example.com")

        assert user.name == "John Doe"
        assert user.email == "john@example.com"
        assert user.is_active is True
        assert user.id is not None
        assert user.created_at is not None

    def test_should_raise_when_invalid_email(self) -> None:
        """User creation should fail with invalid email."""
        with pytest.raises(InvalidUserDataException) as exc_info:
            User(name="John Doe", email="invalid-email")

        assert exc_info.value.code == "INVALID_USER_DATA"
        assert "Invalid email format" in exc_info.value.message

    def test_should_raise_when_empty_name(self) -> None:
        """User creation should fail with empty name."""
        with pytest.raises(InvalidUserDataException) as exc_info:
            User(name="", email="john@example.com")

        assert "Name cannot be empty" in exc_info.value.message

    def test_should_raise_when_whitespace_only_name(self) -> None:
        """User creation should fail with whitespace-only name."""
        with pytest.raises(InvalidUserDataException) as exc_info:
            User(name="   ", email="john@example.com")

        assert "Name cannot be empty" in exc_info.value.message

    def test_should_update_user_when_valid_data(self) -> None:
        """User update should create new instance with updated values."""
        user = User(name="John Doe", email="john@example.com")
        updated = user.update(name="Jane Doe", email="jane@example.com")

        assert updated.name == "Jane Doe"
        assert updated.email == "jane@example.com"
        assert updated.id == user.id
        assert updated.updated_at is not None

    def test_should_keep_original_value_when_update_partial(self) -> None:
        """Partial update should keep original values."""
        user = User(name="John Doe", email="john@example.com")
        updated = user.update(name="Jane Doe")

        assert updated.name == "Jane Doe"
        assert updated.email == "john@example.com"

    def test_should_deactivate_user(self) -> None:
        """User deactivation should create inactive user."""
        user = User(name="John Doe", email="john@example.com")
        deactivated = user.deactivate()

        assert deactivated.is_active is False
        assert deactivated.id == user.id
        assert deactivated.updated_at is not None
