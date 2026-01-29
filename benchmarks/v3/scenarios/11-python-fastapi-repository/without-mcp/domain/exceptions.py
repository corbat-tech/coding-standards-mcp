"""Custom domain exceptions for the user management system."""


class DomainException(Exception):
    """Base exception for all domain errors."""

    def __init__(self, message: str, code: str = "DOMAIN_ERROR") -> None:
        self.message = message
        self.code = code
        super().__init__(self.message)


class UserNotFoundException(DomainException):
    """Raised when a user is not found."""

    def __init__(self, identifier: str) -> None:
        super().__init__(
            message=f"User not found: {identifier}",
            code="USER_NOT_FOUND",
        )
        self.identifier = identifier


class UserAlreadyExistsException(DomainException):
    """Raised when attempting to create a user with existing email."""

    def __init__(self, email: str) -> None:
        super().__init__(
            message=f"User with email already exists: {email}",
            code="USER_ALREADY_EXISTS",
        )
        self.email = email


class InvalidUserDataException(DomainException):
    """Raised when user data validation fails."""

    def __init__(self, message: str) -> None:
        super().__init__(
            message=message,
            code="INVALID_USER_DATA",
        )
