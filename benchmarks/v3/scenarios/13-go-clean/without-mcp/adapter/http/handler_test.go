package http

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/example/userservice/domain"
)

// mockUserUseCase implements usecase.UserUseCase for testing
type mockUserUseCase struct {
	createUserFn    func(ctx context.Context, name, email string) (*domain.User, error)
	getUserFn       func(ctx context.Context, id string) (*domain.User, error)
	getUserByEmailFn func(ctx context.Context, email string) (*domain.User, error)
	listUsersFn     func(ctx context.Context) ([]*domain.User, error)
	updateUserFn    func(ctx context.Context, id, name, email string) (*domain.User, error)
	deleteUserFn    func(ctx context.Context, id string) error
}

func (m *mockUserUseCase) CreateUser(ctx context.Context, name, email string) (*domain.User, error) {
	if m.createUserFn != nil {
		return m.createUserFn(ctx, name, email)
	}
	return nil, nil
}

func (m *mockUserUseCase) GetUser(ctx context.Context, id string) (*domain.User, error) {
	if m.getUserFn != nil {
		return m.getUserFn(ctx, id)
	}
	return nil, nil
}

func (m *mockUserUseCase) GetUserByEmail(ctx context.Context, email string) (*domain.User, error) {
	if m.getUserByEmailFn != nil {
		return m.getUserByEmailFn(ctx, email)
	}
	return nil, nil
}

func (m *mockUserUseCase) ListUsers(ctx context.Context) ([]*domain.User, error) {
	if m.listUsersFn != nil {
		return m.listUsersFn(ctx)
	}
	return nil, nil
}

func (m *mockUserUseCase) UpdateUser(ctx context.Context, id, name, email string) (*domain.User, error) {
	if m.updateUserFn != nil {
		return m.updateUserFn(ctx, id, name, email)
	}
	return nil, nil
}

func (m *mockUserUseCase) DeleteUser(ctx context.Context, id string) error {
	if m.deleteUserFn != nil {
		return m.deleteUserFn(ctx, id)
	}
	return nil
}

func TestUserHandler_CreateUser(t *testing.T) {
	tests := []struct {
		name           string
		requestBody    interface{}
		mockSetup      func(*mockUserUseCase)
		wantStatusCode int
		wantUserID     string
	}{
		{
			name: "successful creation",
			requestBody: CreateUserRequest{
				Name:  "John Doe",
				Email: "john@example.com",
			},
			mockSetup: func(m *mockUserUseCase) {
				m.createUserFn = func(ctx context.Context, name, email string) (*domain.User, error) {
					user, _ := domain.NewUser("user-123", name, email)
					return user, nil
				}
			},
			wantStatusCode: http.StatusCreated,
			wantUserID:     "user-123",
		},
		{
			name: "duplicate email",
			requestBody: CreateUserRequest{
				Name:  "John Doe",
				Email: "john@example.com",
			},
			mockSetup: func(m *mockUserUseCase) {
				m.createUserFn = func(ctx context.Context, name, email string) (*domain.User, error) {
					return nil, domain.ErrUserAlreadyExists
				}
			},
			wantStatusCode: http.StatusConflict,
		},
		{
			name: "invalid email",
			requestBody: CreateUserRequest{
				Name:  "John Doe",
				Email: "invalid",
			},
			mockSetup: func(m *mockUserUseCase) {
				m.createUserFn = func(ctx context.Context, name, email string) (*domain.User, error) {
					return nil, domain.ErrInvalidEmail
				}
			},
			wantStatusCode: http.StatusBadRequest,
		},
		{
			name:           "invalid JSON",
			requestBody:    "not json",
			mockSetup:      func(m *mockUserUseCase) {},
			wantStatusCode: http.StatusBadRequest,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := &mockUserUseCase{}
			tt.mockSetup(mock)
			handler := NewUserHandler(mock)

			var body []byte
			if s, ok := tt.requestBody.(string); ok {
				body = []byte(s)
			} else {
				body, _ = json.Marshal(tt.requestBody)
			}

			req := httptest.NewRequest(http.MethodPost, "/users", bytes.NewReader(body))
			req.Header.Set("Content-Type", "application/json")
			w := httptest.NewRecorder()

			handler.CreateUser(w, req)

			if w.Code != tt.wantStatusCode {
				t.Errorf("CreateUser() status = %d, want %d", w.Code, tt.wantStatusCode)
			}

			if tt.wantUserID != "" {
				var response UserResponse
				json.NewDecoder(w.Body).Decode(&response)
				if response.ID != tt.wantUserID {
					t.Errorf("CreateUser() user ID = %s, want %s", response.ID, tt.wantUserID)
				}
			}
		})
	}
}

func TestUserHandler_GetUser(t *testing.T) {
	tests := []struct {
		name           string
		userID         string
		mockSetup      func(*mockUserUseCase)
		wantStatusCode int
	}{
		{
			name:   "existing user",
			userID: "user-123",
			mockSetup: func(m *mockUserUseCase) {
				m.getUserFn = func(ctx context.Context, id string) (*domain.User, error) {
					user, _ := domain.NewUser(id, "John Doe", "john@example.com")
					return user, nil
				}
			},
			wantStatusCode: http.StatusOK,
		},
		{
			name:   "user not found",
			userID: "user-999",
			mockSetup: func(m *mockUserUseCase) {
				m.getUserFn = func(ctx context.Context, id string) (*domain.User, error) {
					return nil, domain.ErrUserNotFound
				}
			},
			wantStatusCode: http.StatusNotFound,
		},
		{
			name:   "empty ID",
			userID: "",
			mockSetup: func(m *mockUserUseCase) {},
			wantStatusCode: http.StatusBadRequest,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := &mockUserUseCase{}
			tt.mockSetup(mock)
			handler := NewUserHandler(mock)

			req := httptest.NewRequest(http.MethodGet, "/users/"+tt.userID, nil)
			w := httptest.NewRecorder()

			handler.GetUser(w, req)

			if w.Code != tt.wantStatusCode {
				t.Errorf("GetUser() status = %d, want %d", w.Code, tt.wantStatusCode)
			}
		})
	}
}

func TestUserHandler_ListUsers(t *testing.T) {
	tests := []struct {
		name           string
		mockSetup      func(*mockUserUseCase)
		wantStatusCode int
		wantCount      int
	}{
		{
			name: "list all users",
			mockSetup: func(m *mockUserUseCase) {
				m.listUsersFn = func(ctx context.Context) ([]*domain.User, error) {
					user1, _ := domain.NewUser("user-1", "John", "john@example.com")
					user2, _ := domain.NewUser("user-2", "Jane", "jane@example.com")
					return []*domain.User{user1, user2}, nil
				}
			},
			wantStatusCode: http.StatusOK,
			wantCount:      2,
		},
		{
			name: "empty list",
			mockSetup: func(m *mockUserUseCase) {
				m.listUsersFn = func(ctx context.Context) ([]*domain.User, error) {
					return []*domain.User{}, nil
				}
			},
			wantStatusCode: http.StatusOK,
			wantCount:      0,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := &mockUserUseCase{}
			tt.mockSetup(mock)
			handler := NewUserHandler(mock)

			req := httptest.NewRequest(http.MethodGet, "/users", nil)
			w := httptest.NewRecorder()

			handler.ListUsers(w, req)

			if w.Code != tt.wantStatusCode {
				t.Errorf("ListUsers() status = %d, want %d", w.Code, tt.wantStatusCode)
			}

			var response []UserResponse
			json.NewDecoder(w.Body).Decode(&response)
			if len(response) != tt.wantCount {
				t.Errorf("ListUsers() count = %d, want %d", len(response), tt.wantCount)
			}
		})
	}
}

func TestUserHandler_UpdateUser(t *testing.T) {
	tests := []struct {
		name           string
		userID         string
		requestBody    interface{}
		mockSetup      func(*mockUserUseCase)
		wantStatusCode int
	}{
		{
			name:   "successful update",
			userID: "user-123",
			requestBody: UpdateUserRequest{
				Name:  "John Updated",
				Email: "john.updated@example.com",
			},
			mockSetup: func(m *mockUserUseCase) {
				m.updateUserFn = func(ctx context.Context, id, name, email string) (*domain.User, error) {
					user, _ := domain.NewUser(id, name, email)
					return user, nil
				}
			},
			wantStatusCode: http.StatusOK,
		},
		{
			name:   "user not found",
			userID: "user-999",
			requestBody: UpdateUserRequest{
				Name:  "John",
				Email: "john@example.com",
			},
			mockSetup: func(m *mockUserUseCase) {
				m.updateUserFn = func(ctx context.Context, id, name, email string) (*domain.User, error) {
					return nil, domain.ErrUserNotFound
				}
			},
			wantStatusCode: http.StatusNotFound,
		},
		{
			name:           "invalid JSON",
			userID:         "user-123",
			requestBody:    "not json",
			mockSetup:      func(m *mockUserUseCase) {},
			wantStatusCode: http.StatusBadRequest,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := &mockUserUseCase{}
			tt.mockSetup(mock)
			handler := NewUserHandler(mock)

			var body []byte
			if s, ok := tt.requestBody.(string); ok {
				body = []byte(s)
			} else {
				body, _ = json.Marshal(tt.requestBody)
			}

			req := httptest.NewRequest(http.MethodPut, "/users/"+tt.userID, bytes.NewReader(body))
			req.Header.Set("Content-Type", "application/json")
			w := httptest.NewRecorder()

			handler.UpdateUser(w, req)

			if w.Code != tt.wantStatusCode {
				t.Errorf("UpdateUser() status = %d, want %d", w.Code, tt.wantStatusCode)
			}
		})
	}
}

func TestUserHandler_DeleteUser(t *testing.T) {
	tests := []struct {
		name           string
		userID         string
		mockSetup      func(*mockUserUseCase)
		wantStatusCode int
	}{
		{
			name:   "successful delete",
			userID: "user-123",
			mockSetup: func(m *mockUserUseCase) {
				m.deleteUserFn = func(ctx context.Context, id string) error {
					return nil
				}
			},
			wantStatusCode: http.StatusNoContent,
		},
		{
			name:   "user not found",
			userID: "user-999",
			mockSetup: func(m *mockUserUseCase) {
				m.deleteUserFn = func(ctx context.Context, id string) error {
					return domain.ErrUserNotFound
				}
			},
			wantStatusCode: http.StatusNotFound,
		},
		{
			name:           "empty ID",
			userID:         "",
			mockSetup:      func(m *mockUserUseCase) {},
			wantStatusCode: http.StatusBadRequest,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			mock := &mockUserUseCase{}
			tt.mockSetup(mock)
			handler := NewUserHandler(mock)

			req := httptest.NewRequest(http.MethodDelete, "/users/"+tt.userID, nil)
			w := httptest.NewRecorder()

			handler.DeleteUser(w, req)

			if w.Code != tt.wantStatusCode {
				t.Errorf("DeleteUser() status = %d, want %d", w.Code, tt.wantStatusCode)
			}
		})
	}
}

func TestRouter_SetupRoutes(t *testing.T) {
	mock := &mockUserUseCase{
		listUsersFn: func(ctx context.Context) ([]*domain.User, error) {
			return []*domain.User{}, nil
		},
	}
	handler := NewUserHandler(mock)
	router := NewRouter(handler)
	mux := router.SetupRoutes()

	// Test health endpoint
	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	w := httptest.NewRecorder()
	mux.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("Health check status = %d, want %d", w.Code, http.StatusOK)
	}

	// Test users endpoint
	req = httptest.NewRequest(http.MethodGet, "/users", nil)
	w = httptest.NewRecorder()
	mux.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("List users status = %d, want %d", w.Code, http.StatusOK)
	}
}

func TestExtractIDFromPath(t *testing.T) {
	tests := []struct {
		path   string
		prefix string
		want   string
	}{
		{"/users/123", "/users/", "123"},
		{"/users/abc-def", "/users/", "abc-def"},
		{"/users/123/", "/users/", "123"},
		{"/users/123?foo=bar", "/users/", "123"},
		{"/users/", "/users/", ""},
	}

	for _, tt := range tests {
		t.Run(tt.path, func(t *testing.T) {
			got := extractIDFromPath(tt.path, tt.prefix)
			if got != tt.want {
				t.Errorf("extractIDFromPath(%q, %q) = %q, want %q", tt.path, tt.prefix, got, tt.want)
			}
		})
	}
}
