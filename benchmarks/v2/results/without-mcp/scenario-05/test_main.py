import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

from main import app
from database import get_session
from models import Base

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"


@pytest.fixture
async def test_session():
    engine = create_async_engine(TEST_DATABASE_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async with async_session() as session:
        yield session

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest.fixture
async def client(test_session):
    async def override_get_session():
        yield test_session

    app.dependency_overrides[get_session] = override_get_session

    async with AsyncClient(app=app, base_url="http://test") as client:
        yield client

    app.dependency_overrides.clear()


@pytest.mark.asyncio
async def test_create_task(client):
    response = await client.post("/tasks", json={"title": "Test Task", "description": "Test description"})

    assert response.status_code == 201
    data = response.json()
    assert data["title"] == "Test Task"
    assert data["description"] == "Test description"
    assert data["status"] == "pending"
    assert "id" in data


@pytest.mark.asyncio
async def test_create_task_without_description(client):
    response = await client.post("/tasks", json={"title": "Task without description"})

    assert response.status_code == 201
    data = response.json()
    assert data["title"] == "Task without description"
    assert data["description"] is None


@pytest.mark.asyncio
async def test_create_task_empty_title(client):
    response = await client.post("/tasks", json={"title": ""})

    assert response.status_code == 422


@pytest.mark.asyncio
async def test_list_tasks(client):
    await client.post("/tasks", json={"title": "Task 1"})
    await client.post("/tasks", json={"title": "Task 2"})

    response = await client.get("/tasks")

    assert response.status_code == 200
    data = response.json()
    assert len(data) == 2


@pytest.mark.asyncio
async def test_get_task(client):
    create_response = await client.post("/tasks", json={"title": "Find me"})
    task_id = create_response.json()["id"]

    response = await client.get(f"/tasks/{task_id}")

    assert response.status_code == 200
    assert response.json()["title"] == "Find me"


@pytest.mark.asyncio
async def test_get_task_not_found(client):
    response = await client.get("/tasks/non-existent-id")

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_update_task(client):
    create_response = await client.post("/tasks", json={"title": "Original"})
    task_id = create_response.json()["id"]

    response = await client.put(f"/tasks/{task_id}", json={"title": "Updated", "status": "completed"})

    assert response.status_code == 200
    data = response.json()
    assert data["title"] == "Updated"
    assert data["status"] == "completed"


@pytest.mark.asyncio
async def test_update_task_not_found(client):
    response = await client.put("/tasks/non-existent-id", json={"title": "Updated"})

    assert response.status_code == 404


@pytest.mark.asyncio
async def test_delete_task(client):
    create_response = await client.post("/tasks", json={"title": "Delete me"})
    task_id = create_response.json()["id"]

    response = await client.delete(f"/tasks/{task_id}")

    assert response.status_code == 204

    get_response = await client.get(f"/tasks/{task_id}")
    assert get_response.status_code == 404


@pytest.mark.asyncio
async def test_delete_task_not_found(client):
    response = await client.delete("/tasks/non-existent-id")

    assert response.status_code == 404
