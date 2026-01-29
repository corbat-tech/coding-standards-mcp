package http

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/corbat/userservice/domain"
	"github.com/corbat/userservice/usecase"
)

// MockUserUseCase is a test double for UserUseCase.
type MockUserUseCase struct {
	createUserFunc func(ctx context.Context, input usecase.CreateUserInput) (*domain.User, error)
	getUserFunc    func(ctx context.Context, id string) (*domain.User, error)
	updateUserFunc func(ctx context.Context, input usecase.UpdateUserInput) (*domain.User, error)
	deleteUserFunc func(ctx context.Context, id string) error
	listUsersFunc  func(ctx context.Context, input usecase.ListUsersInput) (*usecase.ListUsersOutput, error)
}

func (m *MockUserUseCase) CreateUser(ctx context.Context, input usecase.CreateUserInput) (*domain.User, error) {
	if m.createUserFunc != nil {
		return m.createUserFunc(ctx, input)
	}
	return nil, nil
}

func (m *MockUserUseCase) GetUser(ctx context.Context, id string) (*domain.User, error) {
	if m.getUserFunc != nil {
		return m.getUserFunc(ctx, id)
	}
	return nil, nil
}

func (m *MockUserUseCase) UpdateUser(ctx context.Context, input usecase.UpdateUserInput) (*domain.User, error) {
	if m.updateUserFunc != nil {
		return m.updateUserFunc(ctx, input)
	}
	return nil, nil
}

func (m *MockUserUseCase) DeleteUser(ctx context.Context, id string) error {
	if m.deleteUserFunc != nil {
		return m.deleteUserFunc(ctx, id)
	}
	return nil
}

func (m *MockUserUseCase) ListUsers(ctx context.Context, input usecase.ListUsersInput) (*usecase.ListUsersOutput, error) {
	if m.listUsersFunc != nil {
		return m.listUsersFunc(ctx, input)
	}
	return nil, nil
}

func TestUserHandler_CreateUser_Returns201(t *testing.T) {
	mockUC := &MockUserUseCase{
		createUserFunc: func(ctx context.Context, input usecase.CreateUserInput) (*domain.User, error) {
			return &domain.User{
				ID:    "test-id",
				Name:  input.Name,
				Email: input.Email,
			}, nil
		},
	}
	handler := NewUserHandler(mockUC)

	body := CreateUserRequest{Name: "John Doe", Email: "john@example.com"}
	jsonBody, _ := json.Marshal(body)
	req := httptest.NewRequest(http.MethodPost, "/users", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	handler.CreateUser(rec, req)

	if rec.Code != http.StatusCreated {
		t.Errorf("expected status 201, got %d", rec.Code)
	}

	var response UserResponse
	_ = json.NewDecoder(rec.Body).Decode(&response)
	if response.Name != "John Doe" {
		t.Errorf("expected name 'John Doe', got '%s'", response.Name)
	}
}

func TestUserHandler_CreateUser_InvalidJSON_Returns400(t *testing.T) {
	handler := NewUserHandler(&MockUserUseCase{})

	req := httptest.NewRequest(http.MethodPost, "/users", bytes.NewReader([]byte("invalid json")))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	handler.CreateUser(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", rec.Code)
	}
}

func TestUserHandler_CreateUser_InvalidEmail_Returns400(t *testing.T) {
	mockUC := &MockUserUseCase{
		createUserFunc: func(ctx context.Context, input usecase.CreateUserInput) (*domain.User, error) {
			return nil, domain.ErrInvalidEmail
		},
	}
	handler := NewUserHandler(mockUC)

	body := CreateUserRequest{Name: "John Doe", Email: "invalid"}
	jsonBody, _ := json.Marshal(body)
	req := httptest.NewRequest(http.MethodPost, "/users", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	handler.CreateUser(rec, req)

	if rec.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", rec.Code)
	}
}

func TestUserHandler_CreateUser_DuplicateEmail_Returns409(t *testing.T) {
	mockUC := &MockUserUseCase{
		createUserFunc: func(ctx context.Context, input usecase.CreateUserInput) (*domain.User, error) {
			return nil, domain.ErrEmailAlreadyExists
		},
	}
	handler := NewUserHandler(mockUC)

	body := CreateUserRequest{Name: "John Doe", Email: "john@example.com"}
	jsonBody, _ := json.Marshal(body)
	req := httptest.NewRequest(http.MethodPost, "/users", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	handler.CreateUser(rec, req)

	if rec.Code != http.StatusConflict {
		t.Errorf("expected status 409, got %d", rec.Code)
	}
}

func TestUserHandler_GetUser_Returns200(t *testing.T) {
	mockUC := &MockUserUseCase{
		getUserFunc: func(ctx context.Context, id string) (*domain.User, error) {
			return &domain.User{
				ID:    id,
				Name:  "John Doe",
				Email: "john@example.com",
			}, nil
		},
	}
	handler := NewUserHandler(mockUC)

	req := httptest.NewRequest(http.MethodGet, "/users/test-id", nil)
	rec := httptest.NewRecorder()

	handler.GetUser(rec, req, "test-id")

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", rec.Code)
	}

	var response UserResponse
	_ = json.NewDecoder(rec.Body).Decode(&response)
	if response.ID != "test-id" {
		t.Errorf("expected ID 'test-id', got '%s'", response.ID)
	}
}

func TestUserHandler_GetUser_NotFound_Returns404(t *testing.T) {
	mockUC := &MockUserUseCase{
		getUserFunc: func(ctx context.Context, id string) (*domain.User, error) {
			return nil, domain.ErrUserNotFound
		},
	}
	handler := NewUserHandler(mockUC)

	req := httptest.NewRequest(http.MethodGet, "/users/non-existent", nil)
	rec := httptest.NewRecorder()

	handler.GetUser(rec, req, "non-existent")

	if rec.Code != http.StatusNotFound {
		t.Errorf("expected status 404, got %d", rec.Code)
	}
}

func TestUserHandler_UpdateUser_Returns200(t *testing.T) {
	mockUC := &MockUserUseCase{
		updateUserFunc: func(ctx context.Context, input usecase.UpdateUserInput) (*domain.User, error) {
			return &domain.User{
				ID:    input.ID,
				Name:  input.Name,
				Email: input.Email,
			}, nil
		},
	}
	handler := NewUserHandler(mockUC)

	body := UpdateUserRequest{Name: "Jane Doe", Email: "jane@example.com"}
	jsonBody, _ := json.Marshal(body)
	req := httptest.NewRequest(http.MethodPut, "/users/test-id", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	handler.UpdateUser(rec, req, "test-id")

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", rec.Code)
	}

	var response UserResponse
	_ = json.NewDecoder(rec.Body).Decode(&response)
	if response.Name != "Jane Doe" {
		t.Errorf("expected name 'Jane Doe', got '%s'", response.Name)
	}
}

func TestUserHandler_UpdateUser_NotFound_Returns404(t *testing.T) {
	mockUC := &MockUserUseCase{
		updateUserFunc: func(ctx context.Context, input usecase.UpdateUserInput) (*domain.User, error) {
			return nil, domain.ErrUserNotFound
		},
	}
	handler := NewUserHandler(mockUC)

	body := UpdateUserRequest{Name: "Jane Doe", Email: "jane@example.com"}
	jsonBody, _ := json.Marshal(body)
	req := httptest.NewRequest(http.MethodPut, "/users/non-existent", bytes.NewReader(jsonBody))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	handler.UpdateUser(rec, req, "non-existent")

	if rec.Code != http.StatusNotFound {
		t.Errorf("expected status 404, got %d", rec.Code)
	}
}

func TestUserHandler_DeleteUser_Returns204(t *testing.T) {
	mockUC := &MockUserUseCase{
		deleteUserFunc: func(ctx context.Context, id string) error {
			return nil
		},
	}
	handler := NewUserHandler(mockUC)

	req := httptest.NewRequest(http.MethodDelete, "/users/test-id", nil)
	rec := httptest.NewRecorder()

	handler.DeleteUser(rec, req, "test-id")

	if rec.Code != http.StatusNoContent {
		t.Errorf("expected status 204, got %d", rec.Code)
	}
}

func TestUserHandler_DeleteUser_NotFound_Returns404(t *testing.T) {
	mockUC := &MockUserUseCase{
		deleteUserFunc: func(ctx context.Context, id string) error {
			return domain.ErrUserNotFound
		},
	}
	handler := NewUserHandler(mockUC)

	req := httptest.NewRequest(http.MethodDelete, "/users/non-existent", nil)
	rec := httptest.NewRecorder()

	handler.DeleteUser(rec, req, "non-existent")

	if rec.Code != http.StatusNotFound {
		t.Errorf("expected status 404, got %d", rec.Code)
	}
}

func TestUserHandler_ListUsers_Returns200(t *testing.T) {
	mockUC := &MockUserUseCase{
		listUsersFunc: func(ctx context.Context, input usecase.ListUsersInput) (*usecase.ListUsersOutput, error) {
			return &usecase.ListUsersOutput{
				Users: []*domain.User{
					{ID: "1", Name: "User1", Email: "user1@example.com"},
					{ID: "2", Name: "User2", Email: "user2@example.com"},
				},
				TotalCount: 5,
				Offset:     0,
				Limit:      2,
			}, nil
		},
	}
	handler := NewUserHandler(mockUC)

	req := httptest.NewRequest(http.MethodGet, "/users?offset=0&limit=2", nil)
	rec := httptest.NewRecorder()

	handler.ListUsers(rec, req)

	if rec.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", rec.Code)
	}

	var response ListUsersResponse
	_ = json.NewDecoder(rec.Body).Decode(&response)
	if len(response.Users) != 2 {
		t.Errorf("expected 2 users, got %d", len(response.Users))
	}
	if response.TotalCount != 5 {
		t.Errorf("expected total count 5, got %d", response.TotalCount)
	}
}
