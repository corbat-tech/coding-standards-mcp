"""
Tests for Task API Endpoints.

Tests the REST API layer functionality.
"""
import pytest
from fastapi.testclient import TestClient

from models.task import Task
from schemas.task_schema import TaskStatus


class TestTaskAPI:
    """Test suite for Task API endpoints."""

    def test_create_task_endpoint(self, client: TestClient):
        """Should create task via POST endpoint."""
        response = client.post(
            "/api/v1/tasks",
            json={
                "title": "API Task",
                "description": "Created via API",
                "priority": "high"
            }
        )

        assert response.status_code == 201
        data = response.json()
        assert data["title"] == "API Task"
        assert data["id"] is not None

    def test_create_task_validation_error(self, client: TestClient):
        """Should return 422 for invalid task data."""
        response = client.post(
            "/api/v1/tasks",
            json={"title": ""}  # Empty title
        )

        assert response.status_code == 422

    def test_get_task_endpoint(
        self,
        client: TestClient,
        sample_task: Task
    ):
        """Should get task by ID via GET endpoint."""
        response = client.get(f"/api/v1/tasks/{sample_task.id}")

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == sample_task.id
        assert data["title"] == sample_task.title

    def test_get_task_not_found(self, client: TestClient):
        """Should return 404 for non-existent task."""
        response = client.get("/api/v1/tasks/999")

        assert response.status_code == 404
        assert "not found" in response.json()["detail"].lower()

    def test_list_tasks_endpoint(
        self,
        client: TestClient,
        sample_tasks: list[Task]
    ):
        """Should list tasks with pagination."""
        response = client.get("/api/v1/tasks")

        assert response.status_code == 200
        data = response.json()
        assert "items" in data
        assert data["total"] == 5
        assert len(data["items"]) == 5

    def test_list_tasks_with_pagination(
        self,
        client: TestClient,
        sample_tasks: list[Task]
    ):
        """Should apply pagination parameters."""
        response = client.get("/api/v1/tasks?page=1&page_size=2")

        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 2
        assert data["page"] == 1
        assert data["page_size"] == 2

    def test_list_tasks_with_status_filter(
        self,
        client: TestClient,
        sample_tasks: list[Task]
    ):
        """Should filter tasks by status."""
        response = client.get("/api/v1/tasks?status=completed")

        assert response.status_code == 200
        data = response.json()
        assert all(
            item["status"] == "completed"
            for item in data["items"]
        )

    def test_update_task_endpoint(
        self,
        client: TestClient,
        sample_task: Task
    ):
        """Should update task via PUT endpoint."""
        response = client.put(
            f"/api/v1/tasks/{sample_task.id}",
            json={"title": "Updated Title", "status": "completed"}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["title"] == "Updated Title"
        assert data["status"] == "completed"

    def test_update_task_not_found(self, client: TestClient):
        """Should return 404 on update non-existent task."""
        response = client.put(
            "/api/v1/tasks/999",
            json={"title": "New Title"}
        )

        assert response.status_code == 404

    def test_delete_task_endpoint(
        self,
        client: TestClient,
        sample_task: Task
    ):
        """Should delete task via DELETE endpoint."""
        response = client.delete(f"/api/v1/tasks/{sample_task.id}")

        assert response.status_code == 204

        # Verify task is deleted
        get_response = client.get(f"/api/v1/tasks/{sample_task.id}")
        assert get_response.status_code == 404

    def test_delete_task_not_found(self, client: TestClient):
        """Should return 404 on delete non-existent task."""
        response = client.delete("/api/v1/tasks/999")

        assert response.status_code == 404

    def test_health_check(self, client: TestClient):
        """Should return health status."""
        response = client.get("/health")

        assert response.status_code == 200
        assert response.json()["status"] == "healthy"
