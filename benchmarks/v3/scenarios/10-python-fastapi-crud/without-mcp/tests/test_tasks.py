"""Tests for Task CRUD endpoints."""

import pytest


class TestCreateTask:
    """Tests for creating tasks."""

    def test_create_task_success(self, client, sample_task_data):
        """Test successful task creation."""
        response = client.post("/api/v1/tasks", json=sample_task_data)

        assert response.status_code == 201
        data = response.json()
        assert data["title"] == sample_task_data["title"]
        assert data["description"] == sample_task_data["description"]
        assert data["status"] == sample_task_data["status"]
        assert data["priority"] == sample_task_data["priority"]
        assert data["completed"] == sample_task_data["completed"]
        assert "id" in data
        assert "created_at" in data

    def test_create_task_with_defaults(self, client):
        """Test task creation with default values."""
        response = client.post("/api/v1/tasks", json={"title": "Minimal Task"})

        assert response.status_code == 201
        data = response.json()
        assert data["title"] == "Minimal Task"
        assert data["status"] == "pending"
        assert data["priority"] == "medium"
        assert data["completed"] is False

    def test_create_task_invalid_title_empty(self, client):
        """Test task creation with empty title fails."""
        response = client.post("/api/v1/tasks", json={"title": ""})

        assert response.status_code == 422

    def test_create_task_missing_title(self, client):
        """Test task creation without title fails."""
        response = client.post("/api/v1/tasks", json={"description": "No title"})

        assert response.status_code == 422

    def test_create_task_invalid_status(self, client):
        """Test task creation with invalid status fails."""
        response = client.post(
            "/api/v1/tasks",
            json={"title": "Test", "status": "invalid_status"}
        )

        assert response.status_code == 422

    def test_create_task_invalid_priority(self, client):
        """Test task creation with invalid priority fails."""
        response = client.post(
            "/api/v1/tasks",
            json={"title": "Test", "priority": "invalid_priority"}
        )

        assert response.status_code == 422


class TestGetTasks:
    """Tests for retrieving tasks."""

    def test_get_tasks_empty(self, client):
        """Test getting tasks when none exist."""
        response = client.get("/api/v1/tasks")

        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["total"] == 0
        assert data["page"] == 1

    def test_get_tasks_with_items(self, client, created_task):
        """Test getting tasks when they exist."""
        response = client.get("/api/v1/tasks")

        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 1
        assert data["total"] == 1
        assert data["items"][0]["id"] == created_task["id"]

    def test_get_tasks_pagination(self, client, sample_task_data):
        """Test task pagination."""
        # Create 15 tasks
        for i in range(15):
            task_data = sample_task_data.copy()
            task_data["title"] = f"Task {i}"
            client.post("/api/v1/tasks", json=task_data)

        # Get first page
        response = client.get("/api/v1/tasks?page=1&page_size=5")
        data = response.json()

        assert response.status_code == 200
        assert len(data["items"]) == 5
        assert data["total"] == 15
        assert data["pages"] == 3
        assert data["page"] == 1
        assert data["page_size"] == 5

        # Get second page
        response = client.get("/api/v1/tasks?page=2&page_size=5")
        data = response.json()

        assert len(data["items"]) == 5
        assert data["page"] == 2

    def test_get_tasks_filter_by_status(self, client, sample_task_data):
        """Test filtering tasks by status."""
        # Create tasks with different statuses
        client.post("/api/v1/tasks", json={**sample_task_data, "status": "pending"})
        client.post("/api/v1/tasks", json={**sample_task_data, "status": "completed"})
        client.post("/api/v1/tasks", json={**sample_task_data, "status": "completed"})

        response = client.get("/api/v1/tasks?status=completed")

        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 2
        assert all(item["status"] == "completed" for item in data["items"])

    def test_get_tasks_filter_by_priority(self, client, sample_task_data):
        """Test filtering tasks by priority."""
        # Create tasks with different priorities
        client.post("/api/v1/tasks", json={**sample_task_data, "priority": "low"})
        client.post("/api/v1/tasks", json={**sample_task_data, "priority": "high"})
        client.post("/api/v1/tasks", json={**sample_task_data, "priority": "high"})

        response = client.get("/api/v1/tasks?priority=high")

        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 2
        assert all(item["priority"] == "high" for item in data["items"])


class TestGetTaskById:
    """Tests for retrieving a single task."""

    def test_get_task_success(self, client, created_task):
        """Test getting a task by ID."""
        response = client.get(f"/api/v1/tasks/{created_task['id']}")

        assert response.status_code == 200
        data = response.json()
        assert data["id"] == created_task["id"]
        assert data["title"] == created_task["title"]

    def test_get_task_not_found(self, client):
        """Test getting a non-existent task."""
        response = client.get("/api/v1/tasks/9999")

        assert response.status_code == 404
        assert "not found" in response.json()["detail"].lower()


class TestUpdateTask:
    """Tests for updating tasks."""

    def test_update_task_success(self, client, created_task):
        """Test successful task update."""
        update_data = {"title": "Updated Title", "status": "completed"}
        response = client.put(
            f"/api/v1/tasks/{created_task['id']}",
            json=update_data
        )

        assert response.status_code == 200
        data = response.json()
        assert data["title"] == "Updated Title"
        assert data["status"] == "completed"
        assert data["description"] == created_task["description"]  # Unchanged

    def test_update_task_partial(self, client, created_task):
        """Test partial task update."""
        response = client.put(
            f"/api/v1/tasks/{created_task['id']}",
            json={"priority": "high"}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["priority"] == "high"
        assert data["title"] == created_task["title"]  # Unchanged

    def test_update_task_not_found(self, client):
        """Test updating a non-existent task."""
        response = client.put(
            "/api/v1/tasks/9999",
            json={"title": "Updated"}
        )

        assert response.status_code == 404

    def test_update_task_invalid_status(self, client, created_task):
        """Test updating task with invalid status."""
        response = client.put(
            f"/api/v1/tasks/{created_task['id']}",
            json={"status": "invalid"}
        )

        assert response.status_code == 422


class TestDeleteTask:
    """Tests for deleting tasks."""

    def test_delete_task_success(self, client, created_task):
        """Test successful task deletion."""
        response = client.delete(f"/api/v1/tasks/{created_task['id']}")

        assert response.status_code == 204

        # Verify task is deleted
        get_response = client.get(f"/api/v1/tasks/{created_task['id']}")
        assert get_response.status_code == 404

    def test_delete_task_not_found(self, client):
        """Test deleting a non-existent task."""
        response = client.delete("/api/v1/tasks/9999")

        assert response.status_code == 404


class TestHealthAndRoot:
    """Tests for health and root endpoints."""

    def test_root_endpoint(self, client):
        """Test root endpoint."""
        response = client.get("/")

        assert response.status_code == 200
        data = response.json()
        assert "name" in data
        assert "version" in data
        assert "docs" in data

    def test_health_endpoint(self, client):
        """Test health check endpoint."""
        response = client.get("/health")

        assert response.status_code == 200
        assert response.json()["status"] == "healthy"
