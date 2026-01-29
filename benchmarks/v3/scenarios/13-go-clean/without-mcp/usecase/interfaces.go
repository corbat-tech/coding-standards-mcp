package usecase

import (
	"context"

	"github.com/example/userservice/domain"
)

// UserUseCase defines the interface for user business operations
// This interface allows for dependency injection and testability
type UserUseCase interface {
	// CreateUser creates a new user with the given details
	CreateUser(ctx context.Context, name, email string) (*domain.User, error)

	// GetUser retrieves a user by their ID
	GetUser(ctx context.Context, id string) (*domain.User, error)

	// GetUserByEmail retrieves a user by their email address
	GetUserByEmail(ctx context.Context, email string) (*domain.User, error)

	// ListUsers retrieves all users
	ListUsers(ctx context.Context) ([]*domain.User, error)

	// UpdateUser updates an existing user's details
	UpdateUser(ctx context.Context, id, name, email string) (*domain.User, error)

	// DeleteUser removes a user by their ID
	DeleteUser(ctx context.Context, id string) error
}

// IDGenerator defines the interface for generating unique identifiers
type IDGenerator interface {
	Generate() string
}
