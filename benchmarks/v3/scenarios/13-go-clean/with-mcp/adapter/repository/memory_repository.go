// Package repository provides concrete implementations of domain repositories.
package repository

import (
	"context"
	"sort"
	"sync"

	"github.com/corbat/userservice/domain"
)

// InMemoryUserRepository is an in-memory implementation of UserRepository.
// This implementation is thread-safe and suitable for testing and development.
type InMemoryUserRepository struct {
	mu         sync.RWMutex
	users      map[string]*domain.User
	emailIndex map[string]string
}

// NewInMemoryUserRepository creates a new in-memory user repository.
func NewInMemoryUserRepository() *InMemoryUserRepository {
	return &InMemoryUserRepository{
		users:      make(map[string]*domain.User),
		emailIndex: make(map[string]string),
	}
}

// Create persists a new user.
func (r *InMemoryUserRepository) Create(ctx context.Context, user *domain.User) (*domain.User, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.emailIndex[user.Email]; exists {
		return nil, domain.ErrEmailAlreadyExists
	}

	r.users[user.ID] = user
	r.emailIndex[user.Email] = user.ID
	return user, nil
}

// GetByID retrieves a user by ID.
func (r *InMemoryUserRepository) GetByID(ctx context.Context, id string) (*domain.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	if user, exists := r.users[id]; exists {
		return user, nil
	}
	return nil, domain.ErrUserNotFound
}

// GetByEmail retrieves a user by email.
func (r *InMemoryUserRepository) GetByEmail(ctx context.Context, email string) (*domain.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	if id, exists := r.emailIndex[email]; exists {
		return r.users[id], nil
	}
	return nil, domain.ErrUserNotFound
}

// Update modifies an existing user.
func (r *InMemoryUserRepository) Update(ctx context.Context, user *domain.User) (*domain.User, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	existing, exists := r.users[user.ID]
	if !exists {
		return nil, domain.ErrUserNotFound
	}

	// Check for email uniqueness if email changed
	if existing.Email != user.Email {
		if _, emailExists := r.emailIndex[user.Email]; emailExists {
			return nil, domain.ErrEmailAlreadyExists
		}
		delete(r.emailIndex, existing.Email)
		r.emailIndex[user.Email] = user.ID
	}

	r.users[user.ID] = user
	return user, nil
}

// Delete removes a user by ID.
func (r *InMemoryUserRepository) Delete(ctx context.Context, id string) error {
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

// List returns users with pagination.
func (r *InMemoryUserRepository) List(ctx context.Context, offset, limit int) ([]*domain.User, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	users := make([]*domain.User, 0, len(r.users))
	for _, user := range r.users {
		users = append(users, user)
	}

	// Sort by created time for consistent ordering
	sort.Slice(users, func(i, j int) bool {
		return users[i].CreatedAt.Before(users[j].CreatedAt)
	})

	if offset >= len(users) {
		return []*domain.User{}, nil
	}

	end := offset + limit
	if end > len(users) {
		end = len(users)
	}

	return users[offset:end], nil
}

// Count returns the total number of users.
func (r *InMemoryUserRepository) Count(ctx context.Context) (int, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	return len(r.users), nil
}
