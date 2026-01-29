// Package usecase contains the business logic and use case interfaces.
package usecase

import (
	"context"

	"github.com/corbat/userservice/domain"
)

// CreateUserInput contains the data needed to create a user.
type CreateUserInput struct {
	Name  string
	Email string
}

// UpdateUserInput contains the data needed to update a user.
type UpdateUserInput struct {
	ID    string
	Name  string
	Email string
}

// ListUsersInput contains pagination parameters.
type ListUsersInput struct {
	Offset int
	Limit  int
}

// ListUsersOutput contains the list result with pagination info.
type ListUsersOutput struct {
	Users      []*domain.User
	TotalCount int
	Offset     int
	Limit      int
}

// UserUseCase defines all user-related business operations.
// This interface can be split into smaller interfaces (ISP) if needed.
type UserUseCase interface {
	// CreateUser creates a new user with the given input.
	CreateUser(ctx context.Context, input CreateUserInput) (*domain.User, error)

	// GetUser retrieves a user by ID.
	GetUser(ctx context.Context, id string) (*domain.User, error)

	// UpdateUser updates an existing user.
	UpdateUser(ctx context.Context, input UpdateUserInput) (*domain.User, error)

	// DeleteUser removes a user by ID.
	DeleteUser(ctx context.Context, id string) error

	// ListUsers returns a paginated list of users.
	ListUsers(ctx context.Context, input ListUsersInput) (*ListUsersOutput, error)
}

// Smaller interfaces following Interface Segregation Principle.

// CreateUserUseCase handles user creation.
type CreateUserUseCase interface {
	Execute(ctx context.Context, input CreateUserInput) (*domain.User, error)
}

// GetUserUseCase handles user retrieval.
type GetUserUseCase interface {
	Execute(ctx context.Context, id string) (*domain.User, error)
}

// UpdateUserUseCase handles user updates.
type UpdateUserUseCase interface {
	Execute(ctx context.Context, input UpdateUserInput) (*domain.User, error)
}

// DeleteUserUseCase handles user deletion.
type DeleteUserUseCase interface {
	Execute(ctx context.Context, id string) error
}

// ListUsersUseCase handles listing users with pagination.
type ListUsersUseCase interface {
	Execute(ctx context.Context, input ListUsersInput) (*ListUsersOutput, error)
}
