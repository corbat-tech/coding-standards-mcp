package http

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/example/userservice/domain"
	"github.com/example/userservice/usecase"
)

type UserHandler struct {
	useCase usecase.UserUseCase
}

func NewUserHandler(uc usecase.UserUseCase) *UserHandler {
	return &UserHandler{useCase: uc}
}

func (h *UserHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		h.handleGet(w, r)
	case http.MethodPost:
		h.handleCreate(w, r)
	case http.MethodPut:
		h.handleUpdate(w, r)
	case http.MethodDelete:
		h.handleDelete(w, r)
	default:
		h.writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func (h *UserHandler) handleGet(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		users, _ := h.useCase.GetAllUsers()
		h.writeJSON(w, http.StatusOK, users)
		return
	}

	user, err := h.useCase.GetUser(id)
	if err != nil {
		if errors.Is(err, domain.ErrUserNotFound) {
			h.writeError(w, http.StatusNotFound, "user not found")
			return
		}
		h.writeError(w, http.StatusInternalServerError, "internal error")
		return
	}
	h.writeJSON(w, http.StatusOK, user)
}

func (h *UserHandler) handleCreate(w http.ResponseWriter, r *http.Request) {
	var input domain.CreateUserInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		h.writeError(w, http.StatusBadRequest, "invalid JSON")
		return
	}

	user, err := h.useCase.CreateUser(input)
	if err != nil {
		status := http.StatusInternalServerError
		if errors.Is(err, domain.ErrEmailAlreadyExists) {
			status = http.StatusConflict
		} else if errors.Is(err, domain.ErrInvalidEmail) || errors.Is(err, domain.ErrInvalidName) {
			status = http.StatusBadRequest
		}
		h.writeError(w, status, err.Error())
		return
	}

	h.writeJSON(w, http.StatusCreated, user)
}

func (h *UserHandler) handleUpdate(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		h.writeError(w, http.StatusBadRequest, "user ID required")
		return
	}

	var input domain.UpdateUserInput
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		h.writeError(w, http.StatusBadRequest, "invalid JSON")
		return
	}

	user, err := h.useCase.UpdateUser(id, input)
	if err != nil {
		if errors.Is(err, domain.ErrUserNotFound) {
			h.writeError(w, http.StatusNotFound, "user not found")
			return
		}
		if errors.Is(err, domain.ErrEmailAlreadyExists) {
			h.writeError(w, http.StatusConflict, err.Error())
			return
		}
		h.writeError(w, http.StatusInternalServerError, "internal error")
		return
	}

	h.writeJSON(w, http.StatusOK, user)
}

func (h *UserHandler) handleDelete(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		h.writeError(w, http.StatusBadRequest, "user ID required")
		return
	}

	if err := h.useCase.DeleteUser(id); err != nil {
		if errors.Is(err, domain.ErrUserNotFound) {
			h.writeError(w, http.StatusNotFound, "user not found")
			return
		}
		h.writeError(w, http.StatusInternalServerError, "internal error")
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *UserHandler) extractID(path string) string {
	parts := strings.Split(strings.Trim(path, "/"), "/")
	if len(parts) >= 2 {
		return parts[1]
	}
	return ""
}

func (h *UserHandler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *UserHandler) writeError(w http.ResponseWriter, status int, msg string) {
	h.writeJSON(w, status, map[string]string{"error": msg})
}
