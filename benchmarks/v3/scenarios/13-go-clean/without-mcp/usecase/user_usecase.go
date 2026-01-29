package usecase

import (
	"context"
	"strings"

	"github.com/example/userservice/domain"
)

// userUseCaseImpl implements the UserUseCase interface
type userUseCaseImpl struct {
	repo        domain.UserRepository
	idGenerator IDGenerator
}

// NewUserUseCase creates a new UserUseCase with the provided dependencies
func NewUserUseCase(repo domain.UserRepository, idGenerator IDGenerator) UserUseCase {
	return &userUseCaseImpl{
		repo:        repo,
		idGenerator: idGenerator,
	}
}

// CreateUser creates a new user with the given details
func (uc *userUseCaseImpl) CreateUser(ctx context.Context, name, email string) (*domain.User, error) {
	// Normalize email for checking duplicates
	normalizedEmail := strings.ToLower(strings.TrimSpace(email))

	// Check if user with this email already exists
	existingUser, err := uc.repo.GetByEmail(ctx, normalizedEmail)
	if err != nil && err != domain.ErrUserNotFound {
		return nil, err
	}
	if existingUser != nil {
		return nil, domain.ErrUserAlreadyExists
	}

	// Generate a unique ID
	id := uc.idGenerator.Generate()

	// Create the user entity
	user, err := domain.NewUser(id, name, email)
	if err != nil {
		return nil, err
	}

	// Persist the user
	if err := uc.repo.Create(ctx, user); err != nil {
		return nil, err
	}

	return user, nil
}

// GetUser retrieves a user by their ID
func (uc *userUseCaseImpl) GetUser(ctx context.Context, id string) (*domain.User, error) {
	if id == "" {
		return nil, domain.ErrInvalidID
	}

	return uc.repo.GetByID(ctx, id)
}

// GetUserByEmail retrieves a user by their email address
func (uc *userUseCaseImpl) GetUserByEmail(ctx context.Context, email string) (*domain.User, error) {
	normalizedEmail := strings.ToLower(strings.TrimSpace(email))
	if normalizedEmail == "" {
		return nil, domain.ErrInvalidEmail
	}

	return uc.repo.GetByEmail(ctx, normalizedEmail)
}

// ListUsers retrieves all users
func (uc *userUseCaseImpl) ListUsers(ctx context.Context) ([]*domain.User, error) {
	return uc.repo.GetAll(ctx)
}

// UpdateUser updates an existing user's details
func (uc *userUseCaseImpl) UpdateUser(ctx context.Context, id, name, email string) (*domain.User, error) {
	if id == "" {
		return nil, domain.ErrInvalidID
	}

	// Get the existing user
	user, err := uc.repo.GetByID(ctx, id)
	if err != nil {
		return nil, err
	}

	// Normalize email for checking duplicates
	normalizedEmail := strings.ToLower(strings.TrimSpace(email))

	// Check if another user has this email
	existingUser, err := uc.repo.GetByEmail(ctx, normalizedEmail)
	if err != nil && err != domain.ErrUserNotFound {
		return nil, err
	}
	if existingUser != nil && existingUser.ID != id {
		return nil, domain.ErrUserAlreadyExists
	}

	// Update the user entity
	if err := user.Update(name, email); err != nil {
		return nil, err
	}

	// Persist the changes
	if err := uc.repo.Update(ctx, user); err != nil {
		return nil, err
	}

	return user, nil
}

// DeleteUser removes a user by their ID
func (uc *userUseCaseImpl) DeleteUser(ctx context.Context, id string) error {
	if id == "" {
		return domain.ErrInvalidID
	}

	// Check if user exists
	_, err := uc.repo.GetByID(ctx, id)
	if err != nil {
		return err
	}

	return uc.repo.Delete(ctx, id)
}
