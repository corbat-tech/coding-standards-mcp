package api

import (
	"bookstore/application"
	"bookstore/domain"
	"encoding/json"
	"errors"
	"net/http"
	"strings"
)

type BookHandler struct {
	service *application.BookService
}

func NewBookHandler(service *application.BookService) *BookHandler {
	return &BookHandler{service: service}
}

type CreateBookRequest struct {
	Title  string `json:"title"`
	Author string `json:"author"`
	ISBN   string `json:"isbn"`
}

type UpdateBookRequest struct {
	Title  string `json:"title"`
	Author string `json:"author"`
	ISBN   string `json:"isbn"`
}

type BorrowBookRequest struct {
	BorrowerID string `json:"borrower_id"`
}

type BookResponse struct {
	ID         string  `json:"id"`
	Title      string  `json:"title"`
	Author     string  `json:"author"`
	ISBN       string  `json:"isbn"`
	Status     string  `json:"status"`
	BorrowedBy *string `json:"borrowed_by,omitempty"`
}

type ErrorResponse struct {
	Error string `json:"error"`
}

func (h *BookHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	path := strings.TrimPrefix(r.URL.Path, "/books")

	switch {
	case path == "" || path == "/":
		h.handleCollection(w, r)
	case strings.HasSuffix(path, "/borrow"):
		id := strings.TrimSuffix(strings.TrimPrefix(path, "/"), "/borrow")
		h.handleBorrow(w, r, id)
	case strings.HasSuffix(path, "/return"):
		id := strings.TrimSuffix(strings.TrimPrefix(path, "/"), "/return")
		h.handleReturn(w, r, id)
	default:
		id := strings.TrimPrefix(path, "/")
		h.handleSingle(w, r, id)
	}
}

func (h *BookHandler) handleCollection(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodPost:
		h.createBook(w, r)
	case http.MethodGet:
		h.listBooks(w, r)
	default:
		h.methodNotAllowed(w)
	}
}

func (h *BookHandler) handleSingle(w http.ResponseWriter, r *http.Request, id string) {
	switch r.Method {
	case http.MethodGet:
		h.getBook(w, r, id)
	case http.MethodPut:
		h.updateBook(w, r, id)
	case http.MethodDelete:
		h.deleteBook(w, r, id)
	default:
		h.methodNotAllowed(w)
	}
}

func (h *BookHandler) createBook(w http.ResponseWriter, r *http.Request) {
	var req CreateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.badRequest(w, "invalid request body")
		return
	}

	book, err := h.service.CreateBook(application.CreateBookInput{
		Title:  req.Title,
		Author: req.Author,
		ISBN:   req.ISBN,
	})
	if err != nil {
		h.handleError(w, err)
		return
	}

	h.respond(w, http.StatusCreated, h.toResponse(book))
}

func (h *BookHandler) getBook(w http.ResponseWriter, _ *http.Request, id string) {
	book, err := h.service.GetBook(id)
	if err != nil {
		h.handleError(w, err)
		return
	}

	h.respond(w, http.StatusOK, h.toResponse(book))
}

func (h *BookHandler) listBooks(w http.ResponseWriter, _ *http.Request) {
	books, err := h.service.ListBooks()
	if err != nil {
		h.handleError(w, err)
		return
	}

	response := make([]BookResponse, len(books))
	for i, book := range books {
		response[i] = h.toResponse(book)
	}

	h.respond(w, http.StatusOK, response)
}

func (h *BookHandler) updateBook(w http.ResponseWriter, r *http.Request, id string) {
	var req UpdateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.badRequest(w, "invalid request body")
		return
	}

	book, err := h.service.UpdateBook(id, application.UpdateBookInput{
		Title:  req.Title,
		Author: req.Author,
		ISBN:   req.ISBN,
	})
	if err != nil {
		h.handleError(w, err)
		return
	}

	h.respond(w, http.StatusOK, h.toResponse(book))
}

func (h *BookHandler) deleteBook(w http.ResponseWriter, _ *http.Request, id string) {
	if err := h.service.DeleteBook(id); err != nil {
		h.handleError(w, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *BookHandler) handleBorrow(w http.ResponseWriter, r *http.Request, id string) {
	if r.Method != http.MethodPost {
		h.methodNotAllowed(w)
		return
	}

	var req BorrowBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.badRequest(w, "invalid request body")
		return
	}

	book, err := h.service.BorrowBook(id, req.BorrowerID)
	if err != nil {
		h.handleError(w, err)
		return
	}

	h.respond(w, http.StatusOK, h.toResponse(book))
}

func (h *BookHandler) handleReturn(w http.ResponseWriter, r *http.Request, id string) {
	if r.Method != http.MethodPost {
		h.methodNotAllowed(w)
		return
	}

	book, err := h.service.ReturnBook(id)
	if err != nil {
		h.handleError(w, err)
		return
	}

	h.respond(w, http.StatusOK, h.toResponse(book))
}

func (h *BookHandler) toResponse(book *domain.Book) BookResponse {
	resp := BookResponse{
		ID:     book.ID,
		Title:  book.Title,
		Author: book.Author,
		ISBN:   book.ISBN,
		Status: string(book.Status),
	}
	if book.BorrowedBy != "" {
		resp.BorrowedBy = &book.BorrowedBy
	}
	return resp
}

func (h *BookHandler) respond(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *BookHandler) handleError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, domain.ErrBookNotFound):
		h.respond(w, http.StatusNotFound, ErrorResponse{Error: err.Error()})
	case errors.Is(err, domain.ErrBookNotAvailable):
		h.respond(w, http.StatusConflict, ErrorResponse{Error: err.Error()})
	case errors.Is(err, domain.ErrBookNotBorrowed):
		h.respond(w, http.StatusConflict, ErrorResponse{Error: err.Error()})
	case errors.Is(err, domain.ErrInvalidBookInput):
		h.respond(w, http.StatusBadRequest, ErrorResponse{Error: err.Error()})
	default:
		h.respond(w, http.StatusInternalServerError, ErrorResponse{Error: "internal server error"})
	}
}

func (h *BookHandler) badRequest(w http.ResponseWriter, message string) {
	h.respond(w, http.StatusBadRequest, ErrorResponse{Error: message})
}

func (h *BookHandler) methodNotAllowed(w http.ResponseWriter) {
	h.respond(w, http.StatusMethodNotAllowed, ErrorResponse{Error: "method not allowed"})
}
