package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
)

func setupRouter() (*chi.Mux, *BookStore) {
	store := NewBookStore()
	handler := NewBookHandler(store)

	r := chi.NewRouter()
	r.Route("/books", func(r chi.Router) {
		r.Post("/", handler.Create)
		r.Get("/", handler.GetAll)
		r.Get("/{id}", handler.GetByID)
		r.Put("/{id}", handler.Update)
		r.Delete("/{id}", handler.Delete)
		r.Post("/{id}/borrow", handler.Borrow)
		r.Post("/{id}/return", handler.Return)
	})

	return r, store
}

func TestCreateBook(t *testing.T) {
	r, _ := setupRouter()

	body := `{"title": "Test Book", "author": "Test Author", "isbn": "1234567890"}`
	req := httptest.NewRequest("POST", "/books", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusCreated {
		t.Errorf("Expected status 201, got %d", w.Code)
	}

	var book Book
	json.Unmarshal(w.Body.Bytes(), &book)

	if book.Title != "Test Book" {
		t.Errorf("Expected title 'Test Book', got '%s'", book.Title)
	}
	if !book.Available {
		t.Error("Expected book to be available")
	}
}

func TestCreateBookMissingTitle(t *testing.T) {
	r, _ := setupRouter()

	body := `{"author": "Test Author"}`
	req := httptest.NewRequest("POST", "/books", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("Expected status 400, got %d", w.Code)
	}
}

func TestGetAllBooks(t *testing.T) {
	r, store := setupRouter()

	store.Create(CreateBookRequest{Title: "Book 1", Author: "Author 1"})
	store.Create(CreateBookRequest{Title: "Book 2", Author: "Author 2"})

	req := httptest.NewRequest("GET", "/books", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", w.Code)
	}

	var books []*Book
	json.Unmarshal(w.Body.Bytes(), &books)

	if len(books) != 2 {
		t.Errorf("Expected 2 books, got %d", len(books))
	}
}

func TestGetBookByID(t *testing.T) {
	r, store := setupRouter()

	book, _ := store.Create(CreateBookRequest{Title: "Find Me", Author: "Author"})

	req := httptest.NewRequest("GET", "/books/"+book.ID, nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", w.Code)
	}

	var found Book
	json.Unmarshal(w.Body.Bytes(), &found)

	if found.Title != "Find Me" {
		t.Errorf("Expected title 'Find Me', got '%s'", found.Title)
	}
}

func TestGetBookNotFound(t *testing.T) {
	r, _ := setupRouter()

	req := httptest.NewRequest("GET", "/books/non-existent", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusNotFound {
		t.Errorf("Expected status 404, got %d", w.Code)
	}
}

func TestUpdateBook(t *testing.T) {
	r, store := setupRouter()

	book, _ := store.Create(CreateBookRequest{Title: "Original", Author: "Author"})

	body := `{"title": "Updated"}`
	req := httptest.NewRequest("PUT", "/books/"+book.ID, bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", w.Code)
	}

	var updated Book
	json.Unmarshal(w.Body.Bytes(), &updated)

	if updated.Title != "Updated" {
		t.Errorf("Expected title 'Updated', got '%s'", updated.Title)
	}
}

func TestDeleteBook(t *testing.T) {
	r, store := setupRouter()

	book, _ := store.Create(CreateBookRequest{Title: "Delete Me", Author: "Author"})

	req := httptest.NewRequest("DELETE", "/books/"+book.ID, nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusNoContent {
		t.Errorf("Expected status 204, got %d", w.Code)
	}
}

func TestBorrowBook(t *testing.T) {
	r, store := setupRouter()

	book, _ := store.Create(CreateBookRequest{Title: "Borrow Me", Author: "Author"})

	body := `{"user_id": "user-123"}`
	req := httptest.NewRequest("POST", "/books/"+book.ID+"/borrow", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", w.Code)
	}

	var borrowed Book
	json.Unmarshal(w.Body.Bytes(), &borrowed)

	if borrowed.Available {
		t.Error("Expected book to be unavailable after borrowing")
	}
	if borrowed.BorrowedBy != "user-123" {
		t.Errorf("Expected borrowed_by 'user-123', got '%s'", borrowed.BorrowedBy)
	}
}

func TestBorrowUnavailableBook(t *testing.T) {
	r, store := setupRouter()

	book, _ := store.Create(CreateBookRequest{Title: "Already Borrowed", Author: "Author"})
	store.Borrow(book.ID, "user-1")

	body := `{"user_id": "user-2"}`
	req := httptest.NewRequest("POST", "/books/"+book.ID+"/borrow", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusConflict {
		t.Errorf("Expected status 409, got %d", w.Code)
	}
}

func TestReturnBook(t *testing.T) {
	r, store := setupRouter()

	book, _ := store.Create(CreateBookRequest{Title: "Return Me", Author: "Author"})
	store.Borrow(book.ID, "user-123")

	req := httptest.NewRequest("POST", "/books/"+book.ID+"/return", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("Expected status 200, got %d", w.Code)
	}

	var returned Book
	json.Unmarshal(w.Body.Bytes(), &returned)

	if !returned.Available {
		t.Error("Expected book to be available after return")
	}
}

func TestReturnNotBorrowedBook(t *testing.T) {
	r, store := setupRouter()

	book, _ := store.Create(CreateBookRequest{Title: "Not Borrowed", Author: "Author"})

	req := httptest.NewRequest("POST", "/books/"+book.ID+"/return", nil)
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	if w.Code != http.StatusConflict {
		t.Errorf("Expected status 409, got %d", w.Code)
	}
}
