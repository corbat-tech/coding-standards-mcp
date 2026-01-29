package repository

import (
	"context"
	"testing"

	"github.com/corbat/userservice/domain"
)

func TestInMemoryUserRepository_Create_ReturnsUser(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user, _ := domain.NewUser("John Doe", "john@example.com")

	created, err := repo.Create(ctx, user)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if created.ID != user.ID {
		t.Errorf("expected ID '%s', got '%s'", user.ID, created.ID)
	}
}

func TestInMemoryUserRepository_Create_DuplicateEmail_ReturnsError(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user1, _ := domain.NewUser("John Doe", "john@example.com")
	user2, _ := domain.NewUser("Jane Doe", "john@example.com")

	_, _ = repo.Create(ctx, user1)
	_, err := repo.Create(ctx, user2)

	if err != domain.ErrEmailAlreadyExists {
		t.Errorf("expected ErrEmailAlreadyExists, got %v", err)
	}
}

func TestInMemoryUserRepository_GetByID_ReturnsUser(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user, _ := domain.NewUser("John Doe", "john@example.com")
	created, _ := repo.Create(ctx, user)

	found, err := repo.GetByID(ctx, created.ID)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if found.ID != created.ID {
		t.Errorf("expected ID '%s', got '%s'", created.ID, found.ID)
	}
}

func TestInMemoryUserRepository_GetByID_NotFound_ReturnsError(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()

	_, err := repo.GetByID(ctx, "non-existent-id")

	if err != domain.ErrUserNotFound {
		t.Errorf("expected ErrUserNotFound, got %v", err)
	}
}

func TestInMemoryUserRepository_GetByEmail_ReturnsUser(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user, _ := domain.NewUser("John Doe", "john@example.com")
	_, _ = repo.Create(ctx, user)

	found, err := repo.GetByEmail(ctx, "john@example.com")

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if found.Email != "john@example.com" {
		t.Errorf("expected email 'john@example.com', got '%s'", found.Email)
	}
}

func TestInMemoryUserRepository_GetByEmail_NotFound_ReturnsError(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()

	_, err := repo.GetByEmail(ctx, "nonexistent@example.com")

	if err != domain.ErrUserNotFound {
		t.Errorf("expected ErrUserNotFound, got %v", err)
	}
}

func TestInMemoryUserRepository_Update_ReturnsUpdatedUser(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user, _ := domain.NewUser("John Doe", "john@example.com")
	created, _ := repo.Create(ctx, user)

	created.Name = "Updated Name"
	updated, err := repo.Update(ctx, created)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if updated.Name != "Updated Name" {
		t.Errorf("expected name 'Updated Name', got '%s'", updated.Name)
	}
}

func TestInMemoryUserRepository_Update_NotFound_ReturnsError(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user := &domain.User{ID: "non-existent", Name: "Test", Email: "test@example.com"}

	_, err := repo.Update(ctx, user)

	if err != domain.ErrUserNotFound {
		t.Errorf("expected ErrUserNotFound, got %v", err)
	}
}

func TestInMemoryUserRepository_Update_DuplicateEmail_ReturnsError(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user1, _ := domain.NewUser("John Doe", "john@example.com")
	user2, _ := domain.NewUser("Jane Doe", "jane@example.com")
	_, _ = repo.Create(ctx, user1)
	created2, _ := repo.Create(ctx, user2)

	created2.Email = "john@example.com"
	_, err := repo.Update(ctx, created2)

	if err != domain.ErrEmailAlreadyExists {
		t.Errorf("expected ErrEmailAlreadyExists, got %v", err)
	}
}

func TestInMemoryUserRepository_Delete_RemovesUser(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()
	user, _ := domain.NewUser("John Doe", "john@example.com")
	created, _ := repo.Create(ctx, user)

	err := repo.Delete(ctx, created.ID)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	_, err = repo.GetByID(ctx, created.ID)
	if err != domain.ErrUserNotFound {
		t.Error("expected user to be deleted")
	}
}

func TestInMemoryUserRepository_Delete_NotFound_ReturnsError(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()

	err := repo.Delete(ctx, "non-existent-id")

	if err != domain.ErrUserNotFound {
		t.Errorf("expected ErrUserNotFound, got %v", err)
	}
}

func TestInMemoryUserRepository_List_ReturnsPaginatedUsers(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()

	for i := 0; i < 5; i++ {
		user, _ := domain.NewUser("User", "user"+string(rune('0'+i))+"@example.com")
		_, _ = repo.Create(ctx, user)
	}

	users, err := repo.List(ctx, 0, 3)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if len(users) != 3 {
		t.Errorf("expected 3 users, got %d", len(users))
	}
}

func TestInMemoryUserRepository_List_WithOffset(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()

	for i := 0; i < 5; i++ {
		user, _ := domain.NewUser("User", "user"+string(rune('0'+i))+"@example.com")
		_, _ = repo.Create(ctx, user)
	}

	users, err := repo.List(ctx, 3, 10)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if len(users) != 2 {
		t.Errorf("expected 2 users, got %d", len(users))
	}
}

func TestInMemoryUserRepository_Count_ReturnsCorrectCount(t *testing.T) {
	repo := NewInMemoryUserRepository()
	ctx := context.Background()

	for i := 0; i < 3; i++ {
		user, _ := domain.NewUser("User", "user"+string(rune('0'+i))+"@example.com")
		_, _ = repo.Create(ctx, user)
	}

	count, err := repo.Count(ctx)

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if count != 3 {
		t.Errorf("expected count 3, got %d", count)
	}
}
