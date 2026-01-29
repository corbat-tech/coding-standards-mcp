package domain

import "errors"

var (
	// ErrUserNotFound is returned when a user is not found
	ErrUserNotFound = errors.New("user not found")

	// ErrUserAlreadyExists is returned when trying to create a user with an existing email
	ErrUserAlreadyExists = errors.New("user already exists")

	// ErrInvalidUser is returned when user validation fails
	ErrInvalidUser = errors.New("invalid user data")

	// ErrInvalidEmail is returned when email format is invalid
	ErrInvalidEmail = errors.New("invalid email format")

	// ErrInvalidName is returned when name is empty
	ErrInvalidName = errors.New("name cannot be empty")

	// ErrInvalidID is returned when ID is empty or invalid
	ErrInvalidID = errors.New("invalid user ID")
)
