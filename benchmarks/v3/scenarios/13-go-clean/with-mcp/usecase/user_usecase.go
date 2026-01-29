package usecase

import (
	"context"

	"github.com/corbat/userservice/domain"
)

// DefaultLimit is the default pagination limit.
const DefaultLimit = 10

// MaxLimit is the maximum allowed pagination limit.
const MaxLimit = 100

// UserUseCaseImpl implements the UserUseCase interface.
type UserUseCaseImpl struct {
	repo domain.UserRepository
}

// NewUserUseCase creates a new UserUseCaseImpl with the given repository.
// Uses constructor injection for dependency injection.
func NewUserUseCase(repo domain.UserRepository) *UserUseCaseImpl {
	return &UserUseCaseImpl{repo: repo}
}

// CreateUser creates a new user with validation.
func (u *UserUseCaseImpl) CreateUser(ctx context.Context, input CreateUserInput) (*domain.User, error) {
	user, err := domain.NewUser(input.Name, input.Email)
	if err != nil {
		return nil, err
	}

	return u.repo.Create(ctx, user)
}

// GetUser retrieves a user by ID.
func (u *UserUseCaseImpl) GetUser(ctx context.Context, id string) (*domain.User, error) {
	if id == "" {
		return nil, domain.ErrInvalidUserID
	}
	return u.repo.GetByID(ctx, id)
}

// UpdateUser updates an existing user.
func (u *UserUseCaseImpl) UpdateUser(ctx context.Context, input UpdateUserInput) (*domain.User, error) {
	if input.ID == "" {
		return nil, domain.ErrInvalidUserID
	}

	user, err := u.repo.GetByID(ctx, input.ID)
	if err != nil {
		return nil, err
	}

	if err := user.Update(input.Name, input.Email); err != nil {
		return nil, err
	}

	return u.repo.Update(ctx, user)
}

// DeleteUser removes a user by ID.
func (u *UserUseCaseImpl) DeleteUser(ctx context.Context, id string) error {
	if id == "" {
		return domain.ErrInvalidUserID
	}
	return u.repo.Delete(ctx, id)
}

// ListUsers returns a paginated list of users.
func (u *UserUseCaseImpl) ListUsers(ctx context.Context, input ListUsersInput) (*ListUsersOutput, error) {
	limit := input.Limit
	if limit <= 0 {
		limit = DefaultLimit
	}
	if limit > MaxLimit {
		limit = MaxLimit
	}

	offset := input.Offset
	if offset < 0 {
		offset = 0
	}

	users, err := u.repo.List(ctx, offset, limit)
	if err != nil {
		return nil, err
	}

	total, err := u.repo.Count(ctx)
	if err != nil {
		return nil, err
	}

	return &ListUsersOutput{
		Users:      users,
		TotalCount: total,
		Offset:     offset,
		Limit:      limit,
	}, nil
}
