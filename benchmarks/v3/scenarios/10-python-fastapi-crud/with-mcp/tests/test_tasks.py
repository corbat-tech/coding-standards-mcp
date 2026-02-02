import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app, get_task_service
from app.models import Base, get_db
from app.service import TaskService


@pytest.fixture
def test_db():
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool
    )
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    Base.metadata.create_all(bind=engine)

    def override_get_db():
        db = TestingSessionLocal()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    yield
    app.dependency_overrides.clear()


@pytest.fixture
def client(test_db):
    return TestClient(app)


class TestTaskAPI:
    def test_create_task(self, client):
        response = client.post("/tasks", json={
            "title": "Test Task",
            "description": "Test description",
            "priority": "high"
        })
        assert response.status_code == 201
        data = response.json()
        assert data["title"] == "Test Task"
        assert data["completed"] is False

    def test_get_task(self, client):
        create_response = client.post("/tasks", json={"title": "Get Test"})
        task_id = create_response.json()["id"]

        response = client.get(f"/tasks/{task_id}")
        assert response.status_code == 200
        assert response.json()["title"] == "Get Test"

    def test_get_task_not_found(self, client):
        response = client.get("/tasks/999")
        assert response.status_code == 404

    def test_get_all_tasks(self, client):
        client.post("/tasks", json={"title": "Task 1"})
        client.post("/tasks", json={"title": "Task 2"})

        response = client.get("/tasks")
        assert response.status_code == 200
        assert len(response.json()) == 2

    def test_update_task(self, client):
        create_response = client.post("/tasks", json={"title": "Original"})
        task_id = create_response.json()["id"]

        response = client.put(f"/tasks/{task_id}", json={"title": "Updated", "completed": True})
        assert response.status_code == 200
        assert response.json()["title"] == "Updated"
        assert response.json()["completed"] is True

    def test_delete_task(self, client):
        create_response = client.post("/tasks", json={"title": "To Delete"})
        task_id = create_response.json()["id"]

        response = client.delete(f"/tasks/{task_id}")
        assert response.status_code == 204

        get_response = client.get(f"/tasks/{task_id}")
        assert get_response.status_code == 404

    def test_filter_completed_tasks(self, client):
        client.post("/tasks", json={"title": "Incomplete"})
        create_response = client.post("/tasks", json={"title": "Complete"})
        client.put(f"/tasks/{create_response.json()['id']}", json={"completed": True})

        response = client.get("/tasks?completed=true")
        assert len(response.json()) == 1
        assert response.json()[0]["title"] == "Complete"

    def test_validation_error(self, client):
        response = client.post("/tasks", json={"title": ""})
        assert response.status_code == 422
