package domain

import "context"

// UserRepository defines the interface for user persistence operations
// This interface follows the Repository pattern and allows for dependency inversion
type UserRepository interface {
	// Create stores a new user in the repository
	Create(ctx context.Context, user *User) error

	// GetByID retrieves a user by their unique identifier
	GetByID(ctx context.Context, id string) (*User, error)

	// GetByEmail retrieves a user by their email address
	GetByEmail(ctx context.Context, email string) (*User, error)

	// GetAll retrieves all users from the repository
	GetAll(ctx context.Context) ([]*User, error)

	// Update modifies an existing user in the repository
	Update(ctx context.Context, user *User) error

	// Delete removes a user from the repository by their ID
	Delete(ctx context.Context, id string) error
}
