// Package domain contains business entities and repository interfaces.
package domain

import "errors"

// Domain errors for user operations.
var (
	// ErrUserNotFound is returned when a user cannot be found.
	ErrUserNotFound = errors.New("user not found")
	// ErrInvalidEmail is returned when an email format is invalid.
	ErrInvalidEmail = errors.New("invalid email format")
	// ErrInvalidName is returned when a name is empty or invalid.
	ErrInvalidName = errors.New("name cannot be empty")
	// ErrEmailAlreadyExists is returned when email is already registered.
	ErrEmailAlreadyExists = errors.New("email already exists")
	// ErrInvalidUserID is returned when user ID is invalid.
	ErrInvalidUserID = errors.New("invalid user ID")
)
