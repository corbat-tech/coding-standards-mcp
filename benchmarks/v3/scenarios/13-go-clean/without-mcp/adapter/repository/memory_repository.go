package repository

import (
	"context"
	"sync"

	"github.com/example/userservice/domain"
)

// MemoryUserRepository implements domain.UserRepository using in-memory storage
type MemoryUserRepository struct {
	mu         sync.RWMutex
	users      map[string]*domain.User
	emailIndex map[string]string
}

// NewMemoryUserRepository creates a new in-memory user repository
func NewMemoryUserRepository() *MemoryUserRepository {
	return &MemoryUserRepository{
		users:      make(map[string]*domain.User),
		emailIndex: make(map[string]string),
	}
}

// Create stores a new user in the repository
func (r *MemoryUserRepository) Create(ctx context.Context, user *domain.User) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	// Check if user with this ID already exists
	if _, exists := r.users[user.ID]; exists {
		return domain.ErrUserAlreadyExists
	}

	// Check if email is already taken
	if _, exists := r.emailIndex[user.Email]; exists {
		return domain.ErrUserAlreadyExists
	}

	// Store the user
	r.users[user.ID] = user
	r.emailIndex[user.Email] = user.ID

	return nil
}

// GetByID retrieves a user by their unique identifier
func (r *MemoryUserRepository) GetByID(ctx context.Context, id string) (*domain.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	user, exists := r.users[id]
	if !exists {
		return nil, domain.ErrUserNotFound
	}

	return user, nil
}

// GetByEmail retrieves a user by their email address
func (r *MemoryUserRepository) GetByEmail(ctx context.Context, email string) (*domain.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	id, exists := r.emailIndex[email]
	if !exists {
		return nil, domain.ErrUserNotFound
	}

	user, exists := r.users[id]
	if !exists {
		return nil, domain.ErrUserNotFound
	}

	return user, nil
}

// GetAll retrieves all users from the repository
func (r *MemoryUserRepository) GetAll(ctx context.Context) ([]*domain.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	users := make([]*domain.User, 0, len(r.users))
	for _, user := range r.users {
		users = append(users, user)
	}

	return users, nil
}

// Update modifies an existing user in the repository
func (r *MemoryUserRepository) Update(ctx context.Context, user *domain.User) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	existingUser, exists := r.users[user.ID]
	if !exists {
		return domain.ErrUserNotFound
	}

	// Check if new email is taken by another user
	if existingUser.Email != user.Email {
		if existingID, emailTaken := r.emailIndex[user.Email]; emailTaken && existingID != user.ID {
			return domain.ErrUserAlreadyExists
		}
		// Remove old email index
		delete(r.emailIndex, existingUser.Email)
		// Add new email index
		r.emailIndex[user.Email] = user.ID
	}

	r.users[user.ID] = user

	return nil
}

// Delete removes a user from the repository by their ID
func (r *MemoryUserRepository) Delete(ctx context.Context, id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	user, exists := r.users[id]
	if !exists {
		return domain.ErrUserNotFound
	}

	delete(r.emailIndex, user.Email)
	delete(r.users, id)

	return nil
}
