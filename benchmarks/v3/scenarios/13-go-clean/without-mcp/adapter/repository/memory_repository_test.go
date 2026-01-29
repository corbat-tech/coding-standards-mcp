package repository

import (
	"context"
	"sync"
	"testing"

	"github.com/example/userservice/domain"
)

func TestMemoryUserRepository_Create(t *testing.T) {
	tests := []struct {
		name      string
		setup     func(*MemoryUserRepository)
		user      *domain.User
		wantErr   error
	}{
		{
			name:  "successful creation",
			setup: func(r *MemoryUserRepository) {},
			user: &domain.User{
				ID:    "user-123",
				Name:  "John Doe",
				Email: "john@example.com",
			},
			wantErr: nil,
		},
		{
			name: "duplicate ID",
			setup: func(r *MemoryUserRepository) {
				r.users["user-123"] = &domain.User{ID: "user-123", Name: "Existing", Email: "existing@example.com"}
				r.emailIndex["existing@example.com"] = "user-123"
			},
			user: &domain.User{
				ID:    "user-123",
				Name:  "John Doe",
				Email: "john@example.com",
			},
			wantErr: domain.ErrUserAlreadyExists,
		},
		{
			name: "duplicate email",
			setup: func(r *MemoryUserRepository) {
				r.users["user-456"] = &domain.User{ID: "user-456", Name: "Existing", Email: "john@example.com"}
				r.emailIndex["john@example.com"] = "user-456"
			},
			user: &domain.User{
				ID:    "user-123",
				Name:  "John Doe",
				Email: "john@example.com",
			},
			wantErr: domain.ErrUserAlreadyExists,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := NewMemoryUserRepository()
			tt.setup(repo)

			err := repo.Create(context.Background(), tt.user)

			if err != tt.wantErr {
				t.Errorf("Create() error = %v, want %v", err, tt.wantErr)
			}

			if tt.wantErr == nil {
				// Verify user was stored
				storedUser, _ := repo.GetByID(context.Background(), tt.user.ID)
				if storedUser == nil {
					t.Error("Create() user not stored in repository")
				}
			}
		})
	}
}

func TestMemoryUserRepository_GetByID(t *testing.T) {
	repo := NewMemoryUserRepository()
	user := &domain.User{ID: "user-123", Name: "John Doe", Email: "john@example.com"}
	repo.users["user-123"] = user

	tests := []struct {
		name    string
		id      string
		wantErr error
	}{
		{
			name:    "existing user",
			id:      "user-123",
			wantErr: nil,
		},
		{
			name:    "non-existing user",
			id:      "user-999",
			wantErr: domain.ErrUserNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := repo.GetByID(context.Background(), tt.id)

			if err != tt.wantErr {
				t.Errorf("GetByID() error = %v, want %v", err, tt.wantErr)
			}

			if tt.wantErr == nil && result == nil {
				t.Error("GetByID() returned nil user without error")
			}
		})
	}
}

func TestMemoryUserRepository_GetByEmail(t *testing.T) {
	repo := NewMemoryUserRepository()
	user := &domain.User{ID: "user-123", Name: "John Doe", Email: "john@example.com"}
	repo.users["user-123"] = user
	repo.emailIndex["john@example.com"] = "user-123"

	tests := []struct {
		name    string
		email   string
		wantErr error
	}{
		{
			name:    "existing email",
			email:   "john@example.com",
			wantErr: nil,
		},
		{
			name:    "non-existing email",
			email:   "nobody@example.com",
			wantErr: domain.ErrUserNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := repo.GetByEmail(context.Background(), tt.email)

			if err != tt.wantErr {
				t.Errorf("GetByEmail() error = %v, want %v", err, tt.wantErr)
			}

			if tt.wantErr == nil && result == nil {
				t.Error("GetByEmail() returned nil user without error")
			}
		})
	}
}

func TestMemoryUserRepository_GetAll(t *testing.T) {
	repo := NewMemoryUserRepository()

	// Test empty repository
	users, err := repo.GetAll(context.Background())
	if err != nil {
		t.Errorf("GetAll() unexpected error: %v", err)
	}
	if len(users) != 0 {
		t.Errorf("GetAll() got %d users, want 0", len(users))
	}

	// Add some users
	repo.users["user-1"] = &domain.User{ID: "user-1", Name: "John", Email: "john@example.com"}
	repo.users["user-2"] = &domain.User{ID: "user-2", Name: "Jane", Email: "jane@example.com"}

	users, err = repo.GetAll(context.Background())
	if err != nil {
		t.Errorf("GetAll() unexpected error: %v", err)
	}
	if len(users) != 2 {
		t.Errorf("GetAll() got %d users, want 2", len(users))
	}
}

func TestMemoryUserRepository_Update(t *testing.T) {
	tests := []struct {
		name    string
		setup   func(*MemoryUserRepository)
		user    *domain.User
		wantErr error
	}{
		{
			name: "successful update",
			setup: func(r *MemoryUserRepository) {
				r.users["user-123"] = &domain.User{ID: "user-123", Name: "John", Email: "john@example.com"}
				r.emailIndex["john@example.com"] = "user-123"
			},
			user: &domain.User{
				ID:    "user-123",
				Name:  "John Updated",
				Email: "john.updated@example.com",
			},
			wantErr: nil,
		},
		{
			name:  "user not found",
			setup: func(r *MemoryUserRepository) {},
			user: &domain.User{
				ID:    "user-999",
				Name:  "Nobody",
				Email: "nobody@example.com",
			},
			wantErr: domain.ErrUserNotFound,
		},
		{
			name: "email taken by another user",
			setup: func(r *MemoryUserRepository) {
				r.users["user-123"] = &domain.User{ID: "user-123", Name: "John", Email: "john@example.com"}
				r.users["user-456"] = &domain.User{ID: "user-456", Name: "Jane", Email: "jane@example.com"}
				r.emailIndex["john@example.com"] = "user-123"
				r.emailIndex["jane@example.com"] = "user-456"
			},
			user: &domain.User{
				ID:    "user-123",
				Name:  "John",
				Email: "jane@example.com",
			},
			wantErr: domain.ErrUserAlreadyExists,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := NewMemoryUserRepository()
			tt.setup(repo)

			err := repo.Update(context.Background(), tt.user)

			if err != tt.wantErr {
				t.Errorf("Update() error = %v, want %v", err, tt.wantErr)
			}

			if tt.wantErr == nil {
				// Verify user was updated
				storedUser, _ := repo.GetByID(context.Background(), tt.user.ID)
				if storedUser.Name != tt.user.Name {
					t.Errorf("Update() name not updated, got %v, want %v", storedUser.Name, tt.user.Name)
				}
			}
		})
	}
}

func TestMemoryUserRepository_Delete(t *testing.T) {
	tests := []struct {
		name    string
		setup   func(*MemoryUserRepository)
		id      string
		wantErr error
	}{
		{
			name: "successful delete",
			setup: func(r *MemoryUserRepository) {
				r.users["user-123"] = &domain.User{ID: "user-123", Name: "John", Email: "john@example.com"}
				r.emailIndex["john@example.com"] = "user-123"
			},
			id:      "user-123",
			wantErr: nil,
		},
		{
			name:    "user not found",
			setup:   func(r *MemoryUserRepository) {},
			id:      "user-999",
			wantErr: domain.ErrUserNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := NewMemoryUserRepository()
			tt.setup(repo)

			err := repo.Delete(context.Background(), tt.id)

			if err != tt.wantErr {
				t.Errorf("Delete() error = %v, want %v", err, tt.wantErr)
			}

			if tt.wantErr == nil {
				// Verify user was deleted
				_, err := repo.GetByID(context.Background(), tt.id)
				if err != domain.ErrUserNotFound {
					t.Error("Delete() user not removed from repository")
				}
			}
		})
	}
}

func TestMemoryUserRepository_Concurrency(t *testing.T) {
	repo := NewMemoryUserRepository()
	ctx := context.Background()

	// Test concurrent reads and writes
	var wg sync.WaitGroup
	numGoroutines := 100

	// Create initial users
	for i := 0; i < 10; i++ {
		user, _ := domain.NewUser(
			"user-"+string(rune('a'+i)),
			"User"+string(rune('A'+i)),
			string(rune('a'+i))+"@example.com",
		)
		_ = repo.Create(ctx, user)
	}

	// Concurrent reads
	for i := 0; i < numGoroutines; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			_, _ = repo.GetAll(ctx)
			_, _ = repo.GetByID(ctx, "user-a")
			_, _ = repo.GetByEmail(ctx, "a@example.com")
		}()
	}

	wg.Wait()
	// If we get here without deadlock or panic, the test passes
}
