package http

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/example/userservice/domain"
	"github.com/example/userservice/usecase"
)

// UserHandler handles HTTP requests for user operations
type UserHandler struct {
	userUseCase usecase.UserUseCase
}

// NewUserHandler creates a new UserHandler with the provided use case
func NewUserHandler(uc usecase.UserUseCase) *UserHandler {
	return &UserHandler{
		userUseCase: uc,
	}
}

// CreateUserRequest represents the request body for creating a user
type CreateUserRequest struct {
	Name  string `json:"name"`
	Email string `json:"email"`
}

// UpdateUserRequest represents the request body for updating a user
type UpdateUserRequest struct {
	Name  string `json:"name"`
	Email string `json:"email"`
}

// UserResponse represents the response body for user operations
type UserResponse struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	Email     string `json:"email"`
	CreatedAt string `json:"created_at"`
	UpdatedAt string `json:"updated_at"`
}

// ErrorResponse represents an error response
type ErrorResponse struct {
	Error   string `json:"error"`
	Message string `json:"message"`
}

// toUserResponse converts a domain user to a response DTO
func toUserResponse(user *domain.User) UserResponse {
	return UserResponse{
		ID:        user.ID,
		Name:      user.Name,
		Email:     user.Email,
		CreatedAt: user.CreatedAt.Format("2006-01-02T15:04:05Z07:00"),
		UpdatedAt: user.UpdatedAt.Format("2006-01-02T15:04:05Z07:00"),
	}
}

// writeJSON writes a JSON response with the given status code
func writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

// writeError writes an error response
func writeError(w http.ResponseWriter, status int, errType, message string) {
	writeJSON(w, status, ErrorResponse{
		Error:   errType,
		Message: message,
	})
}

// mapDomainError maps domain errors to HTTP status codes
func mapDomainError(err error) (int, string) {
	switch {
	case errors.Is(err, domain.ErrUserNotFound):
		return http.StatusNotFound, "User not found"
	case errors.Is(err, domain.ErrUserAlreadyExists):
		return http.StatusConflict, "User with this email already exists"
	case errors.Is(err, domain.ErrInvalidEmail):
		return http.StatusBadRequest, "Invalid email format"
	case errors.Is(err, domain.ErrInvalidName):
		return http.StatusBadRequest, "Name cannot be empty"
	case errors.Is(err, domain.ErrInvalidID):
		return http.StatusBadRequest, "Invalid user ID"
	case errors.Is(err, domain.ErrInvalidUser):
		return http.StatusBadRequest, "Invalid user data"
	default:
		return http.StatusInternalServerError, "Internal server error"
	}
}

// CreateUser handles POST /users
func (h *UserHandler) CreateUser(w http.ResponseWriter, r *http.Request) {
	var req CreateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_request", "Invalid JSON body")
		return
	}

	user, err := h.userUseCase.CreateUser(r.Context(), req.Name, req.Email)
	if err != nil {
		status, message := mapDomainError(err)
		writeError(w, status, "create_failed", message)
		return
	}

	writeJSON(w, http.StatusCreated, toUserResponse(user))
}

// GetUser handles GET /users/{id}
func (h *UserHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	id := extractIDFromPath(r.URL.Path, "/users/")
	if id == "" {
		writeError(w, http.StatusBadRequest, "invalid_request", "User ID is required")
		return
	}

	user, err := h.userUseCase.GetUser(r.Context(), id)
	if err != nil {
		status, message := mapDomainError(err)
		writeError(w, status, "get_failed", message)
		return
	}

	writeJSON(w, http.StatusOK, toUserResponse(user))
}

// ListUsers handles GET /users
func (h *UserHandler) ListUsers(w http.ResponseWriter, r *http.Request) {
	users, err := h.userUseCase.ListUsers(r.Context())
	if err != nil {
		status, message := mapDomainError(err)
		writeError(w, status, "list_failed", message)
		return
	}

	response := make([]UserResponse, len(users))
	for i, user := range users {
		response[i] = toUserResponse(user)
	}

	writeJSON(w, http.StatusOK, response)
}

// UpdateUser handles PUT /users/{id}
func (h *UserHandler) UpdateUser(w http.ResponseWriter, r *http.Request) {
	id := extractIDFromPath(r.URL.Path, "/users/")
	if id == "" {
		writeError(w, http.StatusBadRequest, "invalid_request", "User ID is required")
		return
	}

	var req UpdateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_request", "Invalid JSON body")
		return
	}

	user, err := h.userUseCase.UpdateUser(r.Context(), id, req.Name, req.Email)
	if err != nil {
		status, message := mapDomainError(err)
		writeError(w, status, "update_failed", message)
		return
	}

	writeJSON(w, http.StatusOK, toUserResponse(user))
}

// DeleteUser handles DELETE /users/{id}
func (h *UserHandler) DeleteUser(w http.ResponseWriter, r *http.Request) {
	id := extractIDFromPath(r.URL.Path, "/users/")
	if id == "" {
		writeError(w, http.StatusBadRequest, "invalid_request", "User ID is required")
		return
	}

	err := h.userUseCase.DeleteUser(r.Context(), id)
	if err != nil {
		status, message := mapDomainError(err)
		writeError(w, status, "delete_failed", message)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// extractIDFromPath extracts an ID from a URL path
func extractIDFromPath(path, prefix string) string {
	path = strings.TrimPrefix(path, prefix)
	// Remove any trailing slashes or query parameters
	if idx := strings.Index(path, "/"); idx != -1 {
		path = path[:idx]
	}
	if idx := strings.Index(path, "?"); idx != -1 {
		path = path[:idx]
	}
	return path
}
