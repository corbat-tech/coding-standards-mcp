// Package handler provides HTTP handlers for the bookstore API.
package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"bookstore/domain"
)

// BookHandler handles HTTP requests for book operations.
type BookHandler struct {
	repo domain.BookRepository
}

// NewBookHandler creates a new BookHandler with the given repository.
func NewBookHandler(repo domain.BookRepository) *BookHandler {
	return &BookHandler{repo: repo}
}

// ErrorResponse represents an error response body.
type ErrorResponse struct {
	Error string `json:"error"`
}

// GetAll handles GET /books - returns all books.
func (h *BookHandler) GetAll(w http.ResponseWriter, r *http.Request) {
	books, err := h.repo.GetAll()
	if err != nil {
		h.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	h.writeJSON(w, http.StatusOK, books)
}

// GetByID handles GET /books/{id} - returns a book by ID.
func (h *BookHandler) GetByID(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		h.writeError(w, http.StatusBadRequest, "missing book ID")
		return
	}

	book, err := h.repo.GetByID(id)
	if err != nil {
		if errors.Is(err, domain.ErrBookNotFound) {
			h.writeError(w, http.StatusNotFound, err.Error())
			return
		}
		h.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	h.writeJSON(w, http.StatusOK, book)
}

// Create handles POST /books - creates a new book.
func (h *BookHandler) Create(w http.ResponseWriter, r *http.Request) {
	var book domain.Book
	if err := json.NewDecoder(r.Body).Decode(&book); err != nil {
		h.writeError(w, http.StatusBadRequest, "invalid JSON")
		return
	}

	if err := h.repo.Create(&book); err != nil {
		status := h.errorToStatus(err)
		h.writeError(w, status, err.Error())
		return
	}
	h.writeJSON(w, http.StatusCreated, book)
}

// Update handles PUT /books/{id} - updates an existing book.
func (h *BookHandler) Update(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		h.writeError(w, http.StatusBadRequest, "missing book ID")
		return
	}

	var book domain.Book
	if err := json.NewDecoder(r.Body).Decode(&book); err != nil {
		h.writeError(w, http.StatusBadRequest, "invalid JSON")
		return
	}
	book.ID = id

	if err := h.repo.Update(&book); err != nil {
		status := h.errorToStatus(err)
		h.writeError(w, status, err.Error())
		return
	}
	h.writeJSON(w, http.StatusOK, book)
}

// Delete handles DELETE /books/{id} - deletes a book.
func (h *BookHandler) Delete(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		h.writeError(w, http.StatusBadRequest, "missing book ID")
		return
	}

	if err := h.repo.Delete(id); err != nil {
		if errors.Is(err, domain.ErrBookNotFound) {
			h.writeError(w, http.StatusNotFound, err.Error())
			return
		}
		h.writeError(w, http.StatusInternalServerError, err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// extractID extracts the ID from a path like /books/{id}.
func (h *BookHandler) extractID(path string) string {
	parts := strings.Split(strings.Trim(path, "/"), "/")
	if len(parts) >= 2 {
		return parts[len(parts)-1]
	}
	return ""
}

// writeJSON writes a JSON response with the given status code.
func (h *BookHandler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

// writeError writes an error response with the given status code.
func (h *BookHandler) writeError(w http.ResponseWriter, status int, message string) {
	h.writeJSON(w, status, ErrorResponse{Error: message})
}

// errorToStatus maps domain errors to HTTP status codes.
func (h *BookHandler) errorToStatus(err error) int {
	switch {
	case errors.Is(err, domain.ErrBookNotFound):
		return http.StatusNotFound
	case errors.Is(err, domain.ErrDuplicateISBN):
		return http.StatusConflict
	case errors.Is(err, domain.ErrEmptyTitle),
		errors.Is(err, domain.ErrEmptyAuthor),
		errors.Is(err, domain.ErrInvalidISBN),
		errors.Is(err, domain.ErrNegativePrice),
		errors.Is(err, domain.ErrInvalidBook):
		return http.StatusBadRequest
	default:
		return http.StatusInternalServerError
	}
}
