package handlers

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"bookstore/models"
	"bookstore/store"
)

// BookHandler handles HTTP requests for books
type BookHandler struct {
	store *store.BookStore
}

// NewBookHandler creates a new BookHandler
func NewBookHandler(s *store.BookStore) *BookHandler {
	return &BookHandler{store: s}
}

// ServeHTTP routes requests to the appropriate handler method
func (h *BookHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	// Extract ID from path if present
	path := strings.TrimPrefix(r.URL.Path, "/api/books")
	path = strings.TrimPrefix(path, "/")

	switch {
	case r.Method == http.MethodGet && path == "":
		h.List(w, r)
	case r.Method == http.MethodGet && path != "":
		h.Get(w, r, path)
	case r.Method == http.MethodPost && path == "":
		h.Create(w, r)
	case r.Method == http.MethodPut && path != "":
		h.Update(w, r, path)
	case r.Method == http.MethodDelete && path != "":
		h.Delete(w, r, path)
	default:
		writeError(w, http.StatusMethodNotAllowed, "method_not_allowed", "Method not allowed")
	}
}

// List returns all books
func (h *BookHandler) List(w http.ResponseWriter, r *http.Request) {
	books := h.store.GetAll()
	writeList(w, books, len(books))
}

// Get returns a single book by ID
func (h *BookHandler) Get(w http.ResponseWriter, r *http.Request, id string) {
	book, err := h.store.GetByID(id)
	if err != nil {
		if errors.Is(err, store.ErrBookNotFound) {
			writeError(w, http.StatusNotFound, "not_found", "Book not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "internal_error", err.Error())
		return
	}
	writeSuccess(w, http.StatusOK, book)
}

// Create adds a new book
func (h *BookHandler) Create(w http.ResponseWriter, r *http.Request) {
	var req models.CreateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_json", "Invalid JSON payload")
		return
	}

	book, err := h.store.Create(&req)
	if err != nil {
		switch {
		case errors.Is(err, store.ErrDuplicateISBN):
			writeError(w, http.StatusConflict, "duplicate_isbn", "A book with this ISBN already exists")
		case errors.Is(err, models.ErrEmptyTitle):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrEmptyAuthor):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrEmptyISBN):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrInvalidPrice):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrInvalidQty):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		default:
			writeError(w, http.StatusInternalServerError, "internal_error", err.Error())
		}
		return
	}

	writeSuccess(w, http.StatusCreated, book)
}

// Update modifies an existing book
func (h *BookHandler) Update(w http.ResponseWriter, r *http.Request, id string) {
	var req models.UpdateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_json", "Invalid JSON payload")
		return
	}

	book, err := h.store.Update(id, &req)
	if err != nil {
		switch {
		case errors.Is(err, store.ErrBookNotFound):
			writeError(w, http.StatusNotFound, "not_found", "Book not found")
		case errors.Is(err, store.ErrDuplicateISBN):
			writeError(w, http.StatusConflict, "duplicate_isbn", "A book with this ISBN already exists")
		case errors.Is(err, models.ErrEmptyTitle):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrEmptyAuthor):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrEmptyISBN):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrInvalidPrice):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		case errors.Is(err, models.ErrInvalidQty):
			writeError(w, http.StatusBadRequest, "validation_error", err.Error())
		default:
			writeError(w, http.StatusInternalServerError, "internal_error", err.Error())
		}
		return
	}

	writeSuccess(w, http.StatusOK, book)
}

// Delete removes a book
func (h *BookHandler) Delete(w http.ResponseWriter, r *http.Request, id string) {
	err := h.store.Delete(id)
	if err != nil {
		if errors.Is(err, store.ErrBookNotFound) {
			writeError(w, http.StatusNotFound, "not_found", "Book not found")
			return
		}
		writeError(w, http.StatusInternalServerError, "internal_error", err.Error())
		return
	}

	w.WriteHeader(http.StatusNoContent)
}
