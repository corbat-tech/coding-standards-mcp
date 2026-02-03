import pytest
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker

from app.main import app, get_user_service
from app.infrastructure.database import Base, get_session
from app.infrastructure.user_repository import SQLAlchemyUserRepository
from app.application.user_service import UserService


@pytest.fixture
async def test_db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async def override_get_session():
        async with async_session() as session:
            yield session

    app.dependency_overrides[get_session] = override_get_session
    yield
    app.dependency_overrides.clear()


@pytest.fixture
async def client(test_db):
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest.mark.asyncio
class TestUserAPI:
    async def test_create_user(self, client):
        response = await client.post("/users", json={
            "email": "test@test.com",
            "name": "Test User",
            "password": "password123"
        })
        assert response.status_code == 201
        assert response.json()["email"] == "test@test.com"

    async def test_create_duplicate_user(self, client):
        await client.post("/users", json={
            "email": "dup@test.com",
            "name": "User",
            "password": "password123"
        })
        response = await client.post("/users", json={
            "email": "dup@test.com",
            "name": "User 2",
            "password": "password123"
        })
        assert response.status_code == 409

    async def test_get_user(self, client):
        create_res = await client.post("/users", json={
            "email": "get@test.com",
            "name": "Get User",
            "password": "password123"
        })
        user_id = create_res.json()["id"]

        response = await client.get(f"/users/{user_id}")
        assert response.status_code == 200
        assert response.json()["email"] == "get@test.com"

    async def test_get_user_not_found(self, client):
        response = await client.get("/users/999")
        assert response.status_code == 404

    async def test_update_user(self, client):
        create_res = await client.post("/users", json={
            "email": "update@test.com",
            "name": "Original",
            "password": "password123"
        })
        user_id = create_res.json()["id"]

        response = await client.put(f"/users/{user_id}", json={"name": "Updated"})
        assert response.status_code == 200
        assert response.json()["name"] == "Updated"

    async def test_delete_user(self, client):
        create_res = await client.post("/users", json={
            "email": "delete@test.com",
            "name": "Delete Me",
            "password": "password123"
        })
        user_id = create_res.json()["id"]

        response = await client.delete(f"/users/{user_id}")
        assert response.status_code == 204

        get_response = await client.get(f"/users/{user_id}")
        assert get_response.status_code == 404
