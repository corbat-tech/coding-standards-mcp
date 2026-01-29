package usecase

import (
	"context"
	"testing"

	"github.com/corbat/userservice/domain"
)

// MockUserRepository is a test double for UserRepository.
type MockUserRepository struct {
	users       map[string]*domain.User
	emailIndex  map[string]string
	createError error
	getError    error
	updateError error
	deleteError error
}

func NewMockUserRepository() *MockUserRepository {
	return &MockUserRepository{
		users:      make(map[string]*domain.User),
		emailIndex: make(map[string]string),
	}
}

func (m *MockUserRepository) Create(ctx context.Context, user *domain.User) (*domain.User, error) {
	if m.createError != nil {
		return nil, m.createError
	}
	if _, exists := m.emailIndex[user.Email]; exists {
		return nil, domain.ErrEmailAlreadyExists
	}
	m.users[user.ID] = user
	m.emailIndex[user.Email] = user.ID
	return user, nil
}

func (m *MockUserRepository) GetByID(ctx context.Context, id string) (*domain.User, error) {
	if m.getError != nil {
		return nil, m.getError
	}
	if user, exists := m.users[id]; exists {
		return user, nil
	}
	return nil, domain.ErrUserNotFound
}

func (m *MockUserRepository) GetByEmail(ctx context.Context, email string) (*domain.User, error) {
	if id, exists := m.emailIndex[email]; exists {
		return m.users[id], nil
	}
	return nil, domain.ErrUserNotFound
}

func (m *MockUserRepository) Update(ctx context.Context, user *domain.User) (*domain.User, error) {
	if m.updateError != nil {
		return nil, m.updateError
	}
	if _, exists := m.users[user.ID]; !exists {
		return nil, domain.ErrUserNotFound
	}
	m.users[user.ID] = user
	return user, nil
}

func (m *MockUserRepository) Delete(ctx context.Context, id string) error {
	if m.deleteError != nil {
		return m.deleteError
	}
	if user, exists := m.users[id]; exists {
		delete(m.emailIndex, user.Email)
		delete(m.users, id)
		return nil
	}
	return domain.ErrUserNotFound
}

func (m *MockUserRepository) List(ctx context.Context, offset, limit int) ([]*domain.User, error) {
	users := make([]*domain.User, 0, len(m.users))
	for _, user := range m.users {
		users = append(users, user)
	}
	if offset >= len(users) {
		return []*domain.User{}, nil
	}
	end := offset + limit
	if end > len(users) {
		end = len(users)
	}
	return users[offset:end], nil
}

func (m *MockUserRepository) Count(ctx context.Context) (int, error) {
	return len(m.users), nil
}

func TestUserUseCaseImpl_CreateUser_Success(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	user, err := uc.CreateUser(ctx, CreateUserInput{
		Name:  "John Doe",
		Email: "john@example.com",
	})

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if user.Name != "John Doe" {
		t.Errorf("expected name 'John Doe', got '%s'", user.Name)
	}
}

func TestUserUseCaseImpl_CreateUser_InvalidEmail_ReturnsError(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	_, err := uc.CreateUser(ctx, CreateUserInput{
		Name:  "John Doe",
		Email: "invalid-email",
	})

	if err != domain.ErrInvalidEmail {
		t.Errorf("expected ErrInvalidEmail, got %v", err)
	}
}

func TestUserUseCaseImpl_CreateUser_DuplicateEmail_ReturnsError(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	_, _ = uc.CreateUser(ctx, CreateUserInput{
		Name:  "John Doe",
		Email: "john@example.com",
	})

	_, err := uc.CreateUser(ctx, CreateUserInput{
		Name:  "Jane Doe",
		Email: "john@example.com",
	})

	if err != domain.ErrEmailAlreadyExists {
		t.Errorf("expected ErrEmailAlreadyExists, got %v", err)
	}
}

func TestUserUseCaseImpl_GetUser_Success(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	created, _ := uc.CreateUser(ctx, CreateUserInput{
		Name:  "John Doe",
		Email: "john@example.com",
	})

	user, err := uc.GetUser(ctx, created.ID)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if user.ID != created.ID {
		t.Errorf("expected ID '%s', got '%s'", created.ID, user.ID)
	}
}

func TestUserUseCaseImpl_GetUser_NotFound_ReturnsError(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	_, err := uc.GetUser(ctx, "non-existent-id")

	if err != domain.ErrUserNotFound {
		t.Errorf("expected ErrUserNotFound, got %v", err)
	}
}

func TestUserUseCaseImpl_GetUser_EmptyID_ReturnsError(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	_, err := uc.GetUser(ctx, "")

	if err != domain.ErrInvalidUserID {
		t.Errorf("expected ErrInvalidUserID, got %v", err)
	}
}

func TestUserUseCaseImpl_UpdateUser_Success(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	created, _ := uc.CreateUser(ctx, CreateUserInput{
		Name:  "John Doe",
		Email: "john@example.com",
	})

	updated, err := uc.UpdateUser(ctx, UpdateUserInput{
		ID:    created.ID,
		Name:  "Jane Doe",
		Email: "jane@example.com",
	})

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if updated.Name != "Jane Doe" {
		t.Errorf("expected name 'Jane Doe', got '%s'", updated.Name)
	}
}

func TestUserUseCaseImpl_UpdateUser_NotFound_ReturnsError(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	_, err := uc.UpdateUser(ctx, UpdateUserInput{
		ID:    "non-existent-id",
		Name:  "Jane Doe",
		Email: "jane@example.com",
	})

	if err != domain.ErrUserNotFound {
		t.Errorf("expected ErrUserNotFound, got %v", err)
	}
}

func TestUserUseCaseImpl_DeleteUser_Success(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	created, _ := uc.CreateUser(ctx, CreateUserInput{
		Name:  "John Doe",
		Email: "john@example.com",
	})

	err := uc.DeleteUser(ctx, created.ID)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	_, err = uc.GetUser(ctx, created.ID)
	if err != domain.ErrUserNotFound {
		t.Error("expected user to be deleted")
	}
}

func TestUserUseCaseImpl_DeleteUser_NotFound_ReturnsError(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	err := uc.DeleteUser(ctx, "non-existent-id")

	if err != domain.ErrUserNotFound {
		t.Errorf("expected ErrUserNotFound, got %v", err)
	}
}

func TestUserUseCaseImpl_ListUsers_Success(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	for i := 0; i < 5; i++ {
		_, _ = uc.CreateUser(ctx, CreateUserInput{
			Name:  "User",
			Email: "user" + string(rune('0'+i)) + "@example.com",
		})
	}

	result, err := uc.ListUsers(ctx, ListUsersInput{
		Offset: 0,
		Limit:  3,
	})

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if len(result.Users) != 3 {
		t.Errorf("expected 3 users, got %d", len(result.Users))
	}
	if result.TotalCount != 5 {
		t.Errorf("expected total count 5, got %d", result.TotalCount)
	}
}

func TestUserUseCaseImpl_ListUsers_DefaultLimit(t *testing.T) {
	repo := NewMockUserRepository()
	uc := NewUserUseCase(repo)
	ctx := context.Background()

	result, err := uc.ListUsers(ctx, ListUsersInput{
		Offset: 0,
		Limit:  0,
	})

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if result.Limit != DefaultLimit {
		t.Errorf("expected default limit %d, got %d", DefaultLimit, result.Limit)
	}
}
