import pytest
from datetime import datetime, timedelta
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from models import Base, TaskStatus, TaskPriority
from main import app, get_task_service
from repository import TaskRepository
from service import TaskService

# Test database setup
TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(TEST_DATABASE_URL, echo=True)
test_async_session = async_sessionmaker(
    test_engine,
    class_=AsyncSession,
    expire_on_commit=False
)


async def get_test_db():
    async with test_async_session() as session:
        try:
            yield session
        finally:
            await session.close()


def get_test_task_service(db: AsyncSession = None):
    async def _get_service():
        async with test_async_session() as session:
            repository = TaskRepository(session)
            yield TaskService(repository)
    return _get_service


@pytest.fixture
async def async_client():
    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    app.dependency_overrides[get_task_service] = lambda: TaskService(
        TaskRepository(test_async_session())
    )

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

    async with test_engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest.fixture
def sample_task_data():
    return {
        "title": "Test Task",
        "description": "Test description",
        "priority": "medium",
        "due_date": (datetime.utcnow() + timedelta(days=7)).isoformat()
    }


class TestCreateTask:
    @pytest.mark.asyncio
    async def test_create_task_success(self, async_client, sample_task_data):
        response = await async_client.post("/tasks", json=sample_task_data)

        assert response.status_code == 201
        data = response.json()
        assert data["title"] == sample_task_data["title"]
        assert data["status"] == "pending"
        assert data["priority"] == "medium"
        assert "id" in data

    @pytest.mark.asyncio
    async def test_create_task_without_description(self, async_client):
        task_data = {
            "title": "Task without description",
            "priority": "high"
        }
        response = await async_client.post("/tasks", json=task_data)

        assert response.status_code == 201
        assert response.json()["description"] is None

    @pytest.mark.asyncio
    async def test_create_task_empty_title_fails(self, async_client):
        task_data = {
            "title": "",
            "priority": "low"
        }
        response = await async_client.post("/tasks", json=task_data)

        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_create_task_title_too_long_fails(self, async_client):
        task_data = {
            "title": "A" * 201,
            "priority": "low"
        }
        response = await async_client.post("/tasks", json=task_data)

        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_create_task_past_due_date_fails(self, async_client):
        task_data = {
            "title": "Past task",
            "due_date": (datetime.utcnow() - timedelta(days=1)).isoformat()
        }
        response = await async_client.post("/tasks", json=task_data)

        assert response.status_code == 422


class TestGetTasks:
    @pytest.mark.asyncio
    async def test_get_tasks_empty(self, async_client):
        response = await async_client.get("/tasks")

        assert response.status_code == 200
        assert response.json() == []

    @pytest.mark.asyncio
    async def test_get_tasks_with_filter(self, async_client, sample_task_data):
        # Create tasks
        await async_client.post("/tasks", json=sample_task_data)
        await async_client.post("/tasks", json={**sample_task_data, "priority": "high"})

        response = await async_client.get("/tasks?priority=high")

        assert response.status_code == 200
        tasks = response.json()
        assert all(t["priority"] == "high" for t in tasks)


class TestGetTask:
    @pytest.mark.asyncio
    async def test_get_task_success(self, async_client, sample_task_data):
        create_response = await async_client.post("/tasks", json=sample_task_data)
        task_id = create_response.json()["id"]

        response = await async_client.get(f"/tasks/{task_id}")

        assert response.status_code == 200
        assert response.json()["id"] == task_id

    @pytest.mark.asyncio
    async def test_get_task_not_found(self, async_client):
        response = await async_client.get("/tasks/nonexistent-id")

        assert response.status_code == 404


class TestUpdateTask:
    @pytest.mark.asyncio
    async def test_update_task_success(self, async_client, sample_task_data):
        create_response = await async_client.post("/tasks", json=sample_task_data)
        task_id = create_response.json()["id"]

        update_data = {"title": "Updated Title"}
        response = await async_client.put(f"/tasks/{task_id}", json=update_data)

        assert response.status_code == 200
        assert response.json()["title"] == "Updated Title"

    @pytest.mark.asyncio
    async def test_update_completed_task_fails(self, async_client, sample_task_data):
        create_response = await async_client.post("/tasks", json=sample_task_data)
        task_id = create_response.json()["id"]

        # Mark as completed
        await async_client.post(f"/tasks/{task_id}/complete")

        # Try to update
        update_data = {"title": "New Title"}
        response = await async_client.put(f"/tasks/{task_id}", json=update_data)

        assert response.status_code == 400
        assert "completed" in response.json()["detail"].lower()


class TestDeleteTask:
    @pytest.mark.asyncio
    async def test_delete_task_success(self, async_client, sample_task_data):
        create_response = await async_client.post("/tasks", json=sample_task_data)
        task_id = create_response.json()["id"]

        response = await async_client.delete(f"/tasks/{task_id}")

        assert response.status_code == 204

        # Verify deletion
        get_response = await async_client.get(f"/tasks/{task_id}")
        assert get_response.status_code == 404


class TestMarkComplete:
    @pytest.mark.asyncio
    async def test_mark_complete_success(self, async_client, sample_task_data):
        create_response = await async_client.post("/tasks", json=sample_task_data)
        task_id = create_response.json()["id"]

        response = await async_client.post(f"/tasks/{task_id}/complete")

        assert response.status_code == 200
        assert response.json()["status"] == "completed"

    @pytest.mark.asyncio
    async def test_mark_already_completed_fails(self, async_client, sample_task_data):
        create_response = await async_client.post("/tasks", json=sample_task_data)
        task_id = create_response.json()["id"]

        # Complete once
        await async_client.post(f"/tasks/{task_id}/complete")

        # Try to complete again
        response = await async_client.post(f"/tasks/{task_id}/complete")

        assert response.status_code == 400
        assert "already completed" in response.json()["detail"].lower()
