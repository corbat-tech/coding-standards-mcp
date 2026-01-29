"""Custom exceptions and error handling."""

from fastapi import HTTPException, status


class TaskNotFoundError(HTTPException):
    """Exception raised when a task is not found."""

    def __init__(self, task_id: int):
        super().__init__(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Task with id {task_id} not found"
        )


class TaskValidationError(HTTPException):
    """Exception raised when task validation fails."""

    def __init__(self, message: str):
        super().__init__(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=message
        )


class DatabaseError(HTTPException):
    """Exception raised when a database operation fails."""

    def __init__(self, message: str = "Database operation failed"):
        super().__init__(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=message
        )
