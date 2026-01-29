"""Tests for FastAPI routes."""
import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from uuid import uuid4

from main import create_app
from infrastructure.database import Base, get_async_engine


@pytest_asyncio.fixture
async def client():
    """Create test client with in-memory database."""
    # Override database URL for testing
    import os
    os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///:memory:"

    # Clear settings cache
    from api.config import get_settings
    get_settings.cache_clear()

    from api.dependencies import get_engine, get_session_factory
    get_engine.cache_clear()
    get_session_factory.cache_clear()

    app = create_app()

    # Create tables
    engine = get_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


class TestUserAPI:
    """Test cases for user API endpoints."""

    @pytest.mark.asyncio
    async def test_should_create_user_when_valid_request(
        self, client: AsyncClient
    ) -> None:
        """API should create user and return 201."""
        response = await client.post(
            "/api/v1/users",
            json={"name": "API User", "email": "api@example.com"},
        )

        assert response.status_code == 201
        data = response.json()
        assert data["name"] == "API User"
        assert data["email"] == "api@example.com"
        assert "id" in data

    @pytest.mark.asyncio
    async def test_should_return_409_when_duplicate_email(
        self, client: AsyncClient
    ) -> None:
        """API should return 409 for duplicate email."""
        await client.post(
            "/api/v1/users",
            json={"name": "First User", "email": "dup@example.com"},
        )

        response = await client.post(
            "/api/v1/users",
            json={"name": "Second User", "email": "dup@example.com"},
        )

        assert response.status_code == 409

    @pytest.mark.asyncio
    async def test_should_get_user_when_exists(
        self, client: AsyncClient
    ) -> None:
        """API should return user by ID."""
        create_response = await client.post(
            "/api/v1/users",
            json={"name": "Get User", "email": "get@example.com"},
        )
        user_id = create_response.json()["id"]

        response = await client.get(f"/api/v1/users/{user_id}")

        assert response.status_code == 200
        assert response.json()["id"] == user_id

    @pytest.mark.asyncio
    async def test_should_return_404_when_user_not_found(
        self, client: AsyncClient
    ) -> None:
        """API should return 404 for non-existent user."""
        response = await client.get(f"/api/v1/users/{uuid4()}")

        assert response.status_code == 404

    @pytest.mark.asyncio
    async def test_should_update_user_when_exists(
        self, client: AsyncClient
    ) -> None:
        """API should update existing user."""
        create_response = await client.post(
            "/api/v1/users",
            json={"name": "Update User", "email": "update@example.com"},
        )
        user_id = create_response.json()["id"]

        response = await client.patch(
            f"/api/v1/users/{user_id}",
            json={"name": "Updated Name"},
        )

        assert response.status_code == 200
        assert response.json()["name"] == "Updated Name"

    @pytest.mark.asyncio
    async def test_should_delete_user_when_exists(
        self, client: AsyncClient
    ) -> None:
        """API should delete user and return 204."""
        create_response = await client.post(
            "/api/v1/users",
            json={"name": "Delete User", "email": "delete@example.com"},
        )
        user_id = create_response.json()["id"]

        response = await client.delete(f"/api/v1/users/{user_id}")

        assert response.status_code == 204

        # Verify deleted
        get_response = await client.get(f"/api/v1/users/{user_id}")
        assert get_response.status_code == 404

    @pytest.mark.asyncio
    async def test_should_list_users_with_pagination(
        self, client: AsyncClient
    ) -> None:
        """API should list users with pagination."""
        for i in range(3):
            await client.post(
                "/api/v1/users",
                json={"name": f"List User {i}", "email": f"list{i}@example.com"},
            )

        response = await client.get("/api/v1/users?skip=0&limit=2")

        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 2
        assert "total" in data
        assert "has_more" in data

    @pytest.mark.asyncio
    async def test_should_return_422_when_invalid_email(
        self, client: AsyncClient
    ) -> None:
        """API should return 422 for invalid email format."""
        response = await client.post(
            "/api/v1/users",
            json={"name": "Invalid", "email": "not-an-email"},
        )

        assert response.status_code == 422
