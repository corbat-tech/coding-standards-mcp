// Package http provides HTTP handlers for the user service.
package http

import (
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"time"

	"github.com/corbat/userservice/domain"
	"github.com/corbat/userservice/usecase"
)

// CreateUserRequest represents the request body for creating a user.
type CreateUserRequest struct {
	Name  string `json:"name"`
	Email string `json:"email"`
}

// UpdateUserRequest represents the request body for updating a user.
type UpdateUserRequest struct {
	Name  string `json:"name"`
	Email string `json:"email"`
}

// UserResponse represents a user in API responses.
type UserResponse struct {
	ID        string    `json:"id"`
	Name      string    `json:"name"`
	Email     string    `json:"email"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// ListUsersResponse represents the response for listing users.
type ListUsersResponse struct {
	Users      []UserResponse `json:"users"`
	TotalCount int            `json:"total_count"`
	Offset     int            `json:"offset"`
	Limit      int            `json:"limit"`
}

// ErrorResponse represents an error response.
type ErrorResponse struct {
	Error   string `json:"error"`
	Message string `json:"message"`
}

// UserHandler handles HTTP requests for user operations.
type UserHandler struct {
	userUseCase usecase.UserUseCase
}

// NewUserHandler creates a new UserHandler with the given use case.
func NewUserHandler(uc usecase.UserUseCase) *UserHandler {
	return &UserHandler{userUseCase: uc}
}

// CreateUser handles POST /users requests.
func (h *UserHandler) CreateUser(w http.ResponseWriter, r *http.Request) {
	var req CreateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.writeError(w, http.StatusBadRequest, "invalid_request", "Invalid JSON body")
		return
	}

	user, err := h.userUseCase.CreateUser(r.Context(), usecase.CreateUserInput{
		Name:  req.Name,
		Email: req.Email,
	})

	if err != nil {
		h.handleError(w, err)
		return
	}

	h.writeJSON(w, http.StatusCreated, toUserResponse(user))
}

// GetUser handles GET /users/{id} requests.
func (h *UserHandler) GetUser(w http.ResponseWriter, r *http.Request, id string) {
	user, err := h.userUseCase.GetUser(r.Context(), id)
	if err != nil {
		h.handleError(w, err)
		return
	}

	h.writeJSON(w, http.StatusOK, toUserResponse(user))
}

// UpdateUser handles PUT /users/{id} requests.
func (h *UserHandler) UpdateUser(w http.ResponseWriter, r *http.Request, id string) {
	var req UpdateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.writeError(w, http.StatusBadRequest, "invalid_request", "Invalid JSON body")
		return
	}

	user, err := h.userUseCase.UpdateUser(r.Context(), usecase.UpdateUserInput{
		ID:    id,
		Name:  req.Name,
		Email: req.Email,
	})

	if err != nil {
		h.handleError(w, err)
		return
	}

	h.writeJSON(w, http.StatusOK, toUserResponse(user))
}

// DeleteUser handles DELETE /users/{id} requests.
func (h *UserHandler) DeleteUser(w http.ResponseWriter, r *http.Request, id string) {
	err := h.userUseCase.DeleteUser(r.Context(), id)
	if err != nil {
		h.handleError(w, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// ListUsers handles GET /users requests.
func (h *UserHandler) ListUsers(w http.ResponseWriter, r *http.Request) {
	offset, _ := strconv.Atoi(r.URL.Query().Get("offset"))
	limit, _ := strconv.Atoi(r.URL.Query().Get("limit"))

	result, err := h.userUseCase.ListUsers(r.Context(), usecase.ListUsersInput{
		Offset: offset,
		Limit:  limit,
	})

	if err != nil {
		h.handleError(w, err)
		return
	}

	users := make([]UserResponse, len(result.Users))
	for i, u := range result.Users {
		users[i] = toUserResponse(u)
	}

	h.writeJSON(w, http.StatusOK, ListUsersResponse{
		Users:      users,
		TotalCount: result.TotalCount,
		Offset:     result.Offset,
		Limit:      result.Limit,
	})
}

// handleError maps domain errors to HTTP status codes.
func (h *UserHandler) handleError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, domain.ErrUserNotFound):
		h.writeError(w, http.StatusNotFound, "not_found", "User not found")
	case errors.Is(err, domain.ErrInvalidEmail):
		h.writeError(w, http.StatusBadRequest, "invalid_email", "Invalid email format")
	case errors.Is(err, domain.ErrInvalidName):
		h.writeError(w, http.StatusBadRequest, "invalid_name", "Name cannot be empty")
	case errors.Is(err, domain.ErrEmailAlreadyExists):
		h.writeError(w, http.StatusConflict, "email_exists", "Email already registered")
	case errors.Is(err, domain.ErrInvalidUserID):
		h.writeError(w, http.StatusBadRequest, "invalid_id", "Invalid user ID")
	default:
		h.writeError(w, http.StatusInternalServerError, "internal_error", "Internal server error")
	}
}

// writeJSON writes a JSON response.
func (h *UserHandler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(data)
}

// writeError writes an error response.
func (h *UserHandler) writeError(w http.ResponseWriter, status int, errCode, message string) {
	h.writeJSON(w, status, ErrorResponse{Error: errCode, Message: message})
}

// toUserResponse converts a domain User to a UserResponse.
func toUserResponse(u *domain.User) UserResponse {
	return UserResponse{
		ID:        u.ID,
		Name:      u.Name,
		Email:     u.Email,
		CreatedAt: u.CreatedAt,
		UpdatedAt: u.UpdatedAt,
	}
}
