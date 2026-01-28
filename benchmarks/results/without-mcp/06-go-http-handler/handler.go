package main

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/go-chi/chi/v5"
)

type BookHandler struct {
	repo *BookRepository
}

func NewBookHandler(repo *BookRepository) *BookHandler {
	return &BookHandler{repo: repo}
}

type ErrorResponse struct {
	Error string `json:"error"`
}

func respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func respondError(w http.ResponseWriter, status int, err error) {
	respondJSON(w, status, ErrorResponse{Error: err.Error()})
}

func (h *BookHandler) CreateBook(w http.ResponseWriter, r *http.Request) {
	var req CreateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, errors.New("invalid request body"))
		return
	}

	if err := req.Validate(); err != nil {
		respondError(w, http.StatusBadRequest, err)
		return
	}

	book, err := h.repo.Create(&req)
	if err != nil {
		respondError(w, http.StatusInternalServerError, err)
		return
	}

	respondJSON(w, http.StatusCreated, book)
}

func (h *BookHandler) GetBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	book, err := h.repo.GetByID(id)
	if err != nil {
		if errors.Is(err, ErrBookNotFound) {
			respondError(w, http.StatusNotFound, err)
			return
		}
		respondError(w, http.StatusInternalServerError, err)
		return
	}

	respondJSON(w, http.StatusOK, book)
}

func (h *BookHandler) GetBooks(w http.ResponseWriter, r *http.Request) {
	author := r.URL.Query().Get("author")
	genre := r.URL.Query().Get("genre")

	var books []*Book
	if author != "" {
		books = h.repo.GetByAuthor(author)
	} else if genre != "" {
		books = h.repo.GetByGenre(genre)
	} else {
		books = h.repo.GetAll()
	}

	respondJSON(w, http.StatusOK, books)
}

func (h *BookHandler) UpdateBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	var req UpdateBookRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, errors.New("invalid request body"))
		return
	}

	if err := req.Validate(); err != nil {
		respondError(w, http.StatusBadRequest, err)
		return
	}

	book, err := h.repo.Update(id, &req)
	if err != nil {
		if errors.Is(err, ErrBookNotFound) {
			respondError(w, http.StatusNotFound, err)
			return
		}
		respondError(w, http.StatusInternalServerError, err)
		return
	}

	respondJSON(w, http.StatusOK, book)
}

func (h *BookHandler) DeleteBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	err := h.repo.Delete(id)
	if err != nil {
		if errors.Is(err, ErrBookNotFound) {
			respondError(w, http.StatusNotFound, err)
			return
		}
		respondError(w, http.StatusInternalServerError, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (h *BookHandler) BorrowBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	book, err := h.repo.Borrow(id)
	if err != nil {
		if errors.Is(err, ErrBookNotFound) {
			respondError(w, http.StatusNotFound, err)
			return
		}
		if errors.Is(err, ErrBookUnavailable) {
			respondError(w, http.StatusConflict, err)
			return
		}
		respondError(w, http.StatusInternalServerError, err)
		return
	}

	respondJSON(w, http.StatusOK, book)
}

func (h *BookHandler) ReturnBook(w http.ResponseWriter, r *http.Request) {
	id := chi.URLParam(r, "id")

	book, err := h.repo.Return(id)
	if err != nil {
		if errors.Is(err, ErrBookNotFound) {
			respondError(w, http.StatusNotFound, err)
			return
		}
		if errors.Is(err, ErrBookNotBorrowed) {
			respondError(w, http.StatusConflict, err)
			return
		}
		respondError(w, http.StatusInternalServerError, err)
		return
	}

	respondJSON(w, http.StatusOK, book)
}

func SetupRoutes(handler *BookHandler) *chi.Mux {
	r := chi.NewRouter()

	r.Route("/books", func(r chi.Router) {
		r.Post("/", handler.CreateBook)
		r.Get("/", handler.GetBooks)
		r.Get("/{id}", handler.GetBook)
		r.Put("/{id}", handler.UpdateBook)
		r.Delete("/{id}", handler.DeleteBook)
		r.Post("/{id}/borrow", handler.BorrowBook)
		r.Post("/{id}/return", handler.ReturnBook)
	})

	return r
}
