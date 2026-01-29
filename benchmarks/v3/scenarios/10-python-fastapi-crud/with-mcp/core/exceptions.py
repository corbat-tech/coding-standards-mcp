"""
Custom Exceptions for Task Management API.

Defines domain-specific exceptions for proper error handling.
"""


class TaskException(Exception):
    """Base exception for task-related errors."""

    def __init__(self, message: str):
        self.message = message
        super().__init__(self.message)


class TaskNotFoundException(TaskException):
    """Raised when a task is not found."""

    def __init__(self, task_id: int):
        self.task_id = task_id
        super().__init__(f"Task with id {task_id} not found")


class TaskValidationException(TaskException):
    """Raised when task validation fails."""

    def __init__(self, field: str, message: str):
        self.field = field
        super().__init__(f"Validation error for {field}: {message}")


class TaskOperationException(TaskException):
    """Raised when a task operation fails."""

    def __init__(self, operation: str, reason: str):
        self.operation = operation
        super().__init__(f"Operation '{operation}' failed: {reason}")
