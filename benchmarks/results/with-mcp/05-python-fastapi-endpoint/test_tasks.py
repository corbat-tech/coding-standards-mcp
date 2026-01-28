from datetime import datetime, timedelta
import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
class TestCreateTask:
    async def test_should_create_task_when_valid_data(self, client: AsyncClient):
        # Arrange
        data = {
            "title": "Test Task",
            "description": "Test Description",
            "priority": "high",
        }

        # Act
        response = await client.post("/tasks", json=data)

        # Assert
        assert response.status_code == 201
        assert response.json()["title"] == "Test Task"

    async def test_should_return_created_task_with_id(self, client: AsyncClient):
        # Arrange
        data = {"title": "New Task"}

        # Act
        response = await client.post("/tasks", json=data)

        # Assert
        assert "id" in response.json()
        assert response.json()["id"] > 0

    async def test_should_set_default_status_to_pending(self, client: AsyncClient):
        # Arrange
        data = {"title": "Task with default status"}

        # Act
        response = await client.post("/tasks", json=data)

        # Assert
        assert response.json()["status"] == "pending"

    async def test_should_fail_when_title_too_long(self, client: AsyncClient):
        # Arrange
        data = {"title": "x" * 201}

        # Act
        response = await client.post("/tasks", json=data)

        # Assert
        assert response.status_code == 422

    async def test_should_fail_when_due_date_in_past(self, client: AsyncClient):
        # Arrange
        past_date = (datetime.now() - timedelta(days=1)).isoformat()
        data = {"title": "Past task", "due_date": past_date}

        # Act
        response = await client.post("/tasks", json=data)

        # Assert
        assert response.status_code == 422


@pytest.mark.asyncio
class TestGetTask:
    async def test_should_return_task_when_exists(self, client: AsyncClient):
        # Arrange
        create_response = await client.post("/tasks", json={"title": "Get Test"})
        task_id = create_response.json()["id"]

        # Act
        response = await client.get(f"/tasks/{task_id}")

        # Assert
        assert response.status_code == 200
        assert response.json()["id"] == task_id

    async def test_should_return_404_when_not_found(self, client: AsyncClient):
        # Arrange & Act
        response = await client.get("/tasks/99999")

        # Assert
        assert response.status_code == 404


@pytest.mark.asyncio
class TestListTasks:
    async def test_should_return_empty_list_when_no_tasks(self, client: AsyncClient):
        # Arrange & Act
        response = await client.get("/tasks")

        # Assert
        assert response.status_code == 200
        assert response.json()["items"] == []

    async def test_should_filter_by_status(self, client: AsyncClient):
        # Arrange
        await client.post("/tasks", json={"title": "Pending Task"})
        create_response = await client.post("/tasks", json={"title": "To Complete"})
        task_id = create_response.json()["id"]
        await client.post(f"/tasks/{task_id}/complete")

        # Act
        response = await client.get("/tasks?status=completed")

        # Assert
        assert response.status_code == 200
        assert len(response.json()["items"]) == 1

    async def test_should_filter_by_priority(self, client: AsyncClient):
        # Arrange
        await client.post("/tasks", json={"title": "Low", "priority": "low"})
        await client.post("/tasks", json={"title": "High", "priority": "high"})

        # Act
        response = await client.get("/tasks?priority=high")

        # Assert
        assert response.status_code == 200
        assert all(t["priority"] == "high" for t in response.json()["items"])


@pytest.mark.asyncio
class TestUpdateTask:
    async def test_should_update_title_when_valid(self, client: AsyncClient):
        # Arrange
        create_response = await client.post("/tasks", json={"title": "Original"})
        task_id = create_response.json()["id"]

        # Act
        response = await client.put(f"/tasks/{task_id}", json={"title": "Updated"})

        # Assert
        assert response.status_code == 200
        assert response.json()["title"] == "Updated"

    async def test_should_fail_when_task_completed(self, client: AsyncClient):
        # Arrange
        create_response = await client.post("/tasks", json={"title": "To Complete"})
        task_id = create_response.json()["id"]
        await client.post(f"/tasks/{task_id}/complete")

        # Act
        response = await client.put(f"/tasks/{task_id}", json={"title": "Try Update"})

        # Assert
        assert response.status_code == 400

    async def test_should_return_404_when_not_found(self, client: AsyncClient):
        # Arrange & Act
        response = await client.put("/tasks/99999", json={"title": "Update"})

        # Assert
        assert response.status_code == 404


@pytest.mark.asyncio
class TestDeleteTask:
    async def test_should_delete_task_when_exists(self, client: AsyncClient):
        # Arrange
        create_response = await client.post("/tasks", json={"title": "To Delete"})
        task_id = create_response.json()["id"]

        # Act
        response = await client.delete(f"/tasks/{task_id}")

        # Assert
        assert response.status_code == 204

    async def test_should_return_404_after_delete(self, client: AsyncClient):
        # Arrange
        create_response = await client.post("/tasks", json={"title": "To Delete"})
        task_id = create_response.json()["id"]
        await client.delete(f"/tasks/{task_id}")

        # Act
        response = await client.get(f"/tasks/{task_id}")

        # Assert
        assert response.status_code == 404


@pytest.mark.asyncio
class TestMarkAsCompleted:
    async def test_should_mark_task_as_completed(self, client: AsyncClient):
        # Arrange
        create_response = await client.post("/tasks", json={"title": "To Complete"})
        task_id = create_response.json()["id"]

        # Act
        response = await client.post(f"/tasks/{task_id}/complete")

        # Assert
        assert response.status_code == 200
        assert response.json()["status"] == "completed"

    async def test_should_fail_when_already_completed(self, client: AsyncClient):
        # Arrange
        create_response = await client.post("/tasks", json={"title": "Already Done"})
        task_id = create_response.json()["id"]
        await client.post(f"/tasks/{task_id}/complete")

        # Act
        response = await client.post(f"/tasks/{task_id}/complete")

        # Assert
        assert response.status_code == 400

    async def test_should_return_404_when_not_found(self, client: AsyncClient):
        # Arrange & Act
        response = await client.post("/tasks/99999/complete")

        # Assert
        assert response.status_code == 404
