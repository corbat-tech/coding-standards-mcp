package usecase

import (
	"context"
	"testing"

	"github.com/example/userservice/domain"
)

// mockRepository implements domain.UserRepository for testing
type mockRepository struct {
	users       map[string]*domain.User
	emailIndex  map[string]string
	createErr   error
	getErr      error
	updateErr   error
	deleteErr   error
	getAllErr   error
}

func newMockRepository() *mockRepository {
	return &mockRepository{
		users:      make(map[string]*domain.User),
		emailIndex: make(map[string]string),
	}
}

func (m *mockRepository) Create(ctx context.Context, user *domain.User) error {
	if m.createErr != nil {
		return m.createErr
	}
	m.users[user.ID] = user
	m.emailIndex[user.Email] = user.ID
	return nil
}

func (m *mockRepository) GetByID(ctx context.Context, id string) (*domain.User, error) {
	if m.getErr != nil {
		return nil, m.getErr
	}
	user, ok := m.users[id]
	if !ok {
		return nil, domain.ErrUserNotFound
	}
	return user, nil
}

func (m *mockRepository) GetByEmail(ctx context.Context, email string) (*domain.User, error) {
	if m.getErr != nil {
		return nil, m.getErr
	}
	id, ok := m.emailIndex[email]
	if !ok {
		return nil, domain.ErrUserNotFound
	}
	return m.users[id], nil
}

func (m *mockRepository) GetAll(ctx context.Context) ([]*domain.User, error) {
	if m.getAllErr != nil {
		return nil, m.getAllErr
	}
	users := make([]*domain.User, 0, len(m.users))
	for _, user := range m.users {
		users = append(users, user)
	}
	return users, nil
}

func (m *mockRepository) Update(ctx context.Context, user *domain.User) error {
	if m.updateErr != nil {
		return m.updateErr
	}
	if _, ok := m.users[user.ID]; !ok {
		return domain.ErrUserNotFound
	}
	// Remove old email index
	for email, id := range m.emailIndex {
		if id == user.ID {
			delete(m.emailIndex, email)
			break
		}
	}
	m.users[user.ID] = user
	m.emailIndex[user.Email] = user.ID
	return nil
}

func (m *mockRepository) Delete(ctx context.Context, id string) error {
	if m.deleteErr != nil {
		return m.deleteErr
	}
	user, ok := m.users[id]
	if !ok {
		return domain.ErrUserNotFound
	}
	delete(m.emailIndex, user.Email)
	delete(m.users, id)
	return nil
}

// mockIDGenerator implements IDGenerator for testing
type mockIDGenerator struct {
	nextID string
}

func (m *mockIDGenerator) Generate() string {
	return m.nextID
}

func TestCreateUser(t *testing.T) {
	tests := []struct {
		name       string
		userName   string
		email      string
		nextID     string
		setupRepo  func(*mockRepository)
		wantErr    error
		wantUserID string
	}{
		{
			name:       "successful creation",
			userName:   "John Doe",
			email:      "john@example.com",
			nextID:     "user-123",
			setupRepo:  func(r *mockRepository) {},
			wantErr:    nil,
			wantUserID: "user-123",
		},
		{
			name:     "duplicate email",
			userName: "Jane Doe",
			email:    "john@example.com",
			nextID:   "user-456",
			setupRepo: func(r *mockRepository) {
				user, _ := domain.NewUser("user-123", "John Doe", "john@example.com")
				r.users["user-123"] = user
				r.emailIndex["john@example.com"] = "user-123"
			},
			wantErr: domain.ErrUserAlreadyExists,
		},
		{
			name:      "invalid email",
			userName:  "John Doe",
			email:     "invalid-email",
			nextID:    "user-123",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrInvalidEmail,
		},
		{
			name:      "empty name",
			userName:  "",
			email:     "john@example.com",
			nextID:    "user-123",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrInvalidName,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)

			idGen := &mockIDGenerator{nextID: tt.nextID}
			uc := NewUserUseCase(repo, idGen)

			user, err := uc.CreateUser(context.Background(), tt.userName, tt.email)

			if tt.wantErr != nil {
				if err != tt.wantErr {
					t.Errorf("CreateUser() error = %v, want %v", err, tt.wantErr)
				}
				return
			}

			if err != nil {
				t.Errorf("CreateUser() unexpected error: %v", err)
				return
			}

			if user.ID != tt.wantUserID {
				t.Errorf("CreateUser() user.ID = %v, want %v", user.ID, tt.wantUserID)
			}
		})
	}
}

func TestGetUser(t *testing.T) {
	tests := []struct {
		name      string
		userID    string
		setupRepo func(*mockRepository)
		wantErr   error
	}{
		{
			name:   "existing user",
			userID: "user-123",
			setupRepo: func(r *mockRepository) {
				user, _ := domain.NewUser("user-123", "John Doe", "john@example.com")
				r.users["user-123"] = user
			},
			wantErr: nil,
		},
		{
			name:      "non-existing user",
			userID:    "user-999",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrUserNotFound,
		},
		{
			name:      "empty ID",
			userID:    "",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrInvalidID,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)

			idGen := &mockIDGenerator{}
			uc := NewUserUseCase(repo, idGen)

			user, err := uc.GetUser(context.Background(), tt.userID)

			if tt.wantErr != nil {
				if err != tt.wantErr {
					t.Errorf("GetUser() error = %v, want %v", err, tt.wantErr)
				}
				return
			}

			if err != nil {
				t.Errorf("GetUser() unexpected error: %v", err)
				return
			}

			if user.ID != tt.userID {
				t.Errorf("GetUser() user.ID = %v, want %v", user.ID, tt.userID)
			}
		})
	}
}

func TestGetUserByEmail(t *testing.T) {
	tests := []struct {
		name      string
		email     string
		setupRepo func(*mockRepository)
		wantErr   error
	}{
		{
			name:  "existing email",
			email: "john@example.com",
			setupRepo: func(r *mockRepository) {
				user, _ := domain.NewUser("user-123", "John Doe", "john@example.com")
				r.users["user-123"] = user
				r.emailIndex["john@example.com"] = "user-123"
			},
			wantErr: nil,
		},
		{
			name:      "non-existing email",
			email:     "nobody@example.com",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrUserNotFound,
		},
		{
			name:      "empty email",
			email:     "",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrInvalidEmail,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)

			idGen := &mockIDGenerator{}
			uc := NewUserUseCase(repo, idGen)

			_, err := uc.GetUserByEmail(context.Background(), tt.email)

			if tt.wantErr != nil {
				if err != tt.wantErr {
					t.Errorf("GetUserByEmail() error = %v, want %v", err, tt.wantErr)
				}
				return
			}

			if err != nil {
				t.Errorf("GetUserByEmail() unexpected error: %v", err)
			}
		})
	}
}

func TestListUsers(t *testing.T) {
	repo := newMockRepository()
	user1, _ := domain.NewUser("user-1", "John", "john@example.com")
	user2, _ := domain.NewUser("user-2", "Jane", "jane@example.com")
	repo.users["user-1"] = user1
	repo.users["user-2"] = user2

	idGen := &mockIDGenerator{}
	uc := NewUserUseCase(repo, idGen)

	users, err := uc.ListUsers(context.Background())
	if err != nil {
		t.Errorf("ListUsers() unexpected error: %v", err)
		return
	}

	if len(users) != 2 {
		t.Errorf("ListUsers() got %d users, want 2", len(users))
	}
}

func TestUpdateUser(t *testing.T) {
	tests := []struct {
		name      string
		userID    string
		newName   string
		newEmail  string
		setupRepo func(*mockRepository)
		wantErr   error
	}{
		{
			name:     "successful update",
			userID:   "user-123",
			newName:  "John Updated",
			newEmail: "john.updated@example.com",
			setupRepo: func(r *mockRepository) {
				user, _ := domain.NewUser("user-123", "John Doe", "john@example.com")
				r.users["user-123"] = user
				r.emailIndex["john@example.com"] = "user-123"
			},
			wantErr: nil,
		},
		{
			name:      "user not found",
			userID:    "user-999",
			newName:   "John",
			newEmail:  "john@example.com",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrUserNotFound,
		},
		{
			name:     "email already taken by another user",
			userID:   "user-123",
			newName:  "John",
			newEmail: "jane@example.com",
			setupRepo: func(r *mockRepository) {
				user1, _ := domain.NewUser("user-123", "John", "john@example.com")
				user2, _ := domain.NewUser("user-456", "Jane", "jane@example.com")
				r.users["user-123"] = user1
				r.users["user-456"] = user2
				r.emailIndex["john@example.com"] = "user-123"
				r.emailIndex["jane@example.com"] = "user-456"
			},
			wantErr: domain.ErrUserAlreadyExists,
		},
		{
			name:      "empty ID",
			userID:    "",
			newName:   "John",
			newEmail:  "john@example.com",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrInvalidID,
		},
		{
			name:     "invalid email",
			userID:   "user-123",
			newName:  "John",
			newEmail: "invalid",
			setupRepo: func(r *mockRepository) {
				user, _ := domain.NewUser("user-123", "John Doe", "john@example.com")
				r.users["user-123"] = user
				r.emailIndex["john@example.com"] = "user-123"
			},
			wantErr: domain.ErrInvalidEmail,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)

			idGen := &mockIDGenerator{}
			uc := NewUserUseCase(repo, idGen)

			user, err := uc.UpdateUser(context.Background(), tt.userID, tt.newName, tt.newEmail)

			if tt.wantErr != nil {
				if err != tt.wantErr {
					t.Errorf("UpdateUser() error = %v, want %v", err, tt.wantErr)
				}
				return
			}

			if err != nil {
				t.Errorf("UpdateUser() unexpected error: %v", err)
				return
			}

			if user.Name != tt.newName {
				t.Errorf("UpdateUser() user.Name = %v, want %v", user.Name, tt.newName)
			}
		})
	}
}

func TestDeleteUser(t *testing.T) {
	tests := []struct {
		name      string
		userID    string
		setupRepo func(*mockRepository)
		wantErr   error
	}{
		{
			name:   "successful delete",
			userID: "user-123",
			setupRepo: func(r *mockRepository) {
				user, _ := domain.NewUser("user-123", "John Doe", "john@example.com")
				r.users["user-123"] = user
				r.emailIndex["john@example.com"] = "user-123"
			},
			wantErr: nil,
		},
		{
			name:      "user not found",
			userID:    "user-999",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrUserNotFound,
		},
		{
			name:      "empty ID",
			userID:    "",
			setupRepo: func(r *mockRepository) {},
			wantErr:   domain.ErrInvalidID,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)

			idGen := &mockIDGenerator{}
			uc := NewUserUseCase(repo, idGen)

			err := uc.DeleteUser(context.Background(), tt.userID)

			if tt.wantErr != nil {
				if err != tt.wantErr {
					t.Errorf("DeleteUser() error = %v, want %v", err, tt.wantErr)
				}
				return
			}

			if err != nil {
				t.Errorf("DeleteUser() unexpected error: %v", err)
			}

			// Verify user is actually deleted
			_, err = repo.GetByID(context.Background(), tt.userID)
			if err != domain.ErrUserNotFound {
				t.Errorf("DeleteUser() user still exists after deletion")
			}
		})
	}
}
