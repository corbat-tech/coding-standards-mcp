package main

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/go-chi/chi/v5"
)

type BookHandler struct {
	service *BookService
}

func NewBookHandler(service *BookService) *BookHandler {
	return &BookHandler{service: service}
}

func (h *BookHandler) RegisterRoutes(r chi.Router) {
	r.Post("/books", h.CreateBook)
	r.Get("/books", h.GetBooks)
	r.Get("/books/{id}", h.GetBook)
	r.Put("/books/{id}", h.UpdateBook)
	r.Delete("/books/{id}", h.DeleteBook)
	r.Post("/books/{id}/borrow", h.BorrowBook)
	r.Post("/books/{id}/return", h.ReturnBook)
}

func (h *BookHandler) CreateBook(w http.ResponseWriter, r *http.Request) {
	var req CreateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	book, err := h.service.CreateBook(&req)
	if err != nil {
		h.handleError(w, err)
		return
	}

	respondJSON(w, http.StatusCreated, ToBookResponse(book))
}

func (h *BookHandler) GetBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	book, err := h.service.GetBook(id)
	if err != nil {
		h.handleError(w, err)
		return
	}

	respondJSON(w, http.StatusOK, ToBookResponse(book))
}

func (h *BookHandler) GetBooks(w http.ResponseWriter, r *http.Request) {
	author := r.URL.Query().Get("author")
	genre := r.URL.Query().Get("genre")

	var books []*Book
	var err error

	switch {
	case author != "":
		books, err = h.service.FindByAuthor(author)
	case genre != "":
		books, err = h.service.FindByGenre(genre)
	default:
		books, err = h.service.GetAllBooks()
	}

	if err != nil {
		h.handleError(w, err)
		return
	}

	responses := make([]BookResponse, len(books))
	for i, book := range books {
		responses[i] = ToBookResponse(book)
	}

	respondJSON(w, http.StatusOK, responses)
}

func (h *BookHandler) UpdateBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	var req UpdateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "invalid request body")
		return
	}

	book, err := h.service.UpdateBook(id, &req)
	if err != nil {
		h.handleError(w, err)
		return
	}

	respondJSON(w, http.StatusOK, ToBookResponse(book))
}

func (h *BookHandler) DeleteBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	if err := h.service.DeleteBook(id); err != nil {
		h.handleError(w, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *BookHandler) BorrowBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	book, err := h.service.BorrowBook(id)
	if err != nil {
		h.handleError(w, err)
		return
	}

	respondJSON(w, http.StatusOK, ToBookResponse(book))
}

func (h *BookHandler) ReturnBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	book, err := h.service.ReturnBook(id)
	if err != nil {
		h.handleError(w, err)
		return
	}

	respondJSON(w, http.StatusOK, ToBookResponse(book))
}

func (h *BookHandler) handleError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, ErrBookNotFound):
		respondError(w, http.StatusNotFound, err.Error())
	case errors.Is(err, ErrBookNotAvailable):
		respondError(w, http.StatusConflict, err.Error())
	case errors.Is(err, ErrBookNotBorrowed):
		respondError(w, http.StatusConflict, err.Error())
	case errors.Is(err, ErrInvalidISBN),
		errors.Is(err, ErrInvalidYear),
		errors.Is(err, ErrTitleRequired),
		errors.Is(err, ErrAuthorRequired):
		respondError(w, http.StatusBadRequest, err.Error())
	default:
		respondError(w, http.StatusInternalServerError, "internal server error")
	}
}

func respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func respondError(w http.ResponseWriter, status int, message string) {
	respondJSON(w, status, ErrorResponse{Error: message})
}
