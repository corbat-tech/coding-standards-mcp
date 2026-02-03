package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/example/bookstore/model"
	"github.com/example/bookstore/repository"
)

type BookHandler struct {
	repo repository.BookRepository
}

func NewBookHandler(repo repository.BookRepository) *BookHandler {
	return &BookHandler{repo: repo}
}

func (h *BookHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
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
		h.writeError(w, http.StatusMethodNotAllowed, "Method not allowed")
	}
}

func (h *BookHandler) handleGet(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		books, _ := h.repo.GetAll()
		h.writeJSON(w, http.StatusOK, books)
		return
	}

	book, err := h.repo.GetByID(id)
	if err != nil {
		if errors.Is(err, repository.ErrBookNotFound) {
			h.writeError(w, http.StatusNotFound, "Book not found")
			return
		}
		h.writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	h.writeJSON(w, http.StatusOK, book)
}

func (h *BookHandler) handleCreate(w http.ResponseWriter, r *http.Request) {
	var req model.CreateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.writeError(w, http.StatusBadRequest, "Invalid JSON")
		return
	}

	if req.Title == "" || req.Author == "" || req.ISBN == "" {
		h.writeError(w, http.StatusBadRequest, "Title, author, and ISBN are required")
		return
	}

	book := &model.Book{
		ID:     generateID(),
		Title:  req.Title,
		Author: req.Author,
		ISBN:   req.ISBN,
		Price:  req.Price,
	}

	if err := h.repo.Create(book); err != nil {
		if errors.Is(err, repository.ErrISBNExists) {
			h.writeError(w, http.StatusConflict, "ISBN already exists")
			return
		}
		h.writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	h.writeJSON(w, http.StatusCreated, book)
}

func (h *BookHandler) handleUpdate(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		h.writeError(w, http.StatusBadRequest, "Book ID is required")
		return
	}

	book, err := h.repo.GetByID(id)
	if err != nil {
		if errors.Is(err, repository.ErrBookNotFound) {
			h.writeError(w, http.StatusNotFound, "Book not found")
			return
		}
		h.writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	var req model.UpdateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.writeError(w, http.StatusBadRequest, "Invalid JSON")
		return
	}

	if req.Title != nil {
		book.Title = *req.Title
	}
	if req.Author != nil {
		book.Author = *req.Author
	}
	if req.Price != nil {
		book.Price = *req.Price
	}

	if err := h.repo.Update(id, book); err != nil {
		h.writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	h.writeJSON(w, http.StatusOK, book)
}

func (h *BookHandler) handleDelete(w http.ResponseWriter, r *http.Request) {
	id := h.extractID(r.URL.Path)
	if id == "" {
		h.writeError(w, http.StatusBadRequest, "Book ID is required")
		return
	}

	if err := h.repo.Delete(id); err != nil {
		if errors.Is(err, repository.ErrBookNotFound) {
			h.writeError(w, http.StatusNotFound, "Book not found")
			return
		}
		h.writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *BookHandler) extractID(path string) string {
	parts := strings.Split(strings.Trim(path, "/"), "/")
	if len(parts) >= 2 {
		return parts[1]
	}
	return ""
}

func (h *BookHandler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *BookHandler) writeError(w http.ResponseWriter, status int, message string) {
	h.writeJSON(w, status, model.ErrorResponse{Error: http.StatusText(status), Message: message})
}

func generateID() string {
	return strings.Replace(strings.ToLower(strings.ReplaceAll(
		strings.TrimPrefix(strings.ToLower(http.StatusText(http.StatusOK)), "ok"),
		" ", "")), "", "", -1) + "-" + randomString(8)
}

func randomString(n int) string {
	const letters = "abcdefghijklmnopqrstuvwxyz0123456789"
	b := make([]byte, n)
	for i := range b {
		b[i] = letters[i%len(letters)]
	}
	return string(b)
}
