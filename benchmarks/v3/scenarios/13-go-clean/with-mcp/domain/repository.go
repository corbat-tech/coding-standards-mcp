package domain

import "context"

// UserRepository defines the interface for user persistence operations.
// This interface allows for different implementations (in-memory, database, etc.)
// following the Dependency Inversion Principle.
type UserRepository interface {
	// Create persists a new user and returns the created user.
	Create(ctx context.Context, user *User) (*User, error)

	// GetByID retrieves a user by their ID.
	// Returns ErrUserNotFound if the user doesn't exist.
	GetByID(ctx context.Context, id string) (*User, error)

	// GetByEmail retrieves a user by their email.
	// Returns ErrUserNotFound if the user doesn't exist.
	GetByEmail(ctx context.Context, email string) (*User, error)

	// Update modifies an existing user.
	// Returns ErrUserNotFound if the user doesn't exist.
	Update(ctx context.Context, user *User) (*User, error)

	// Delete removes a user by their ID.
	// Returns ErrUserNotFound if the user doesn't exist.
	Delete(ctx context.Context, id string) error

	// List returns all users with optional pagination.
	List(ctx context.Context, offset, limit int) ([]*User, error)

	// Count returns the total number of users.
	Count(ctx context.Context) (int, error)
}
