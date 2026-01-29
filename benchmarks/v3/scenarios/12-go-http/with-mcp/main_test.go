package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"bookstore/domain"
	"bookstore/handler"
	"bookstore/repository"
)

func setupTestServer() *http.ServeMux {
	repo := repository.NewInMemoryBookRepository()
	bookHandler := handler.NewBookHandler(repo)
	mux := http.NewServeMux()
	setupRoutes(mux, bookHandler)
	return mux
}

func TestIntegration_CreateAndGetBook(t *testing.T) {
	server := setupTestServer()

	// Create a book
	book := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "The Go Programming Language",
		Author: "Alan A. A. Donovan",
		Price:  39.99,
	}
	body, _ := json.Marshal(book)

	req := httptest.NewRequest("POST", "/books", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rec := httptest.NewRecorder()

	server.ServeHTTP(rec, req)

	if rec.Code != http.StatusCreated {
		t.Fatalf("Create book: got %d, want %d", rec.Code, http.StatusCreated)
	}

	var createdBook domain.Book
	json.NewDecoder(rec.Body).Decode(&createdBook)

	// Get the book
	req = httptest.NewRequest("GET", "/books/"+createdBook.ID, nil)
	rec = httptest.NewRecorder()

	server.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("Get book: got %d, want %d", rec.Code, http.StatusOK)
	}

	var fetchedBook domain.Book
	json.NewDecoder(rec.Body).Decode(&fetchedBook)

	if fetchedBook.Title != book.Title {
		t.Errorf("Title = %q, want %q", fetchedBook.Title, book.Title)
	}
}

func TestIntegration_UpdateBook(t *testing.T) {
	server := setupTestServer()

	// Create a book
	book := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "Original Title",
		Author: "Original Author",
		Price:  29.99,
	}
	body, _ := json.Marshal(book)

	req := httptest.NewRequest("POST", "/books", bytes.NewReader(body))
	rec := httptest.NewRecorder()
	server.ServeHTTP(rec, req)

	var createdBook domain.Book
	json.NewDecoder(rec.Body).Decode(&createdBook)

	// Update the book
	createdBook.Title = "Updated Title"
	createdBook.Price = 49.99
	body, _ = json.Marshal(createdBook)

	req = httptest.NewRequest("PUT", "/books/"+createdBook.ID, bytes.NewReader(body))
	rec = httptest.NewRecorder()
	server.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("Update book: got %d, want %d", rec.Code, http.StatusOK)
	}

	var updatedBook domain.Book
	json.NewDecoder(rec.Body).Decode(&updatedBook)

	if updatedBook.Title != "Updated Title" {
		t.Errorf("Title = %q, want %q", updatedBook.Title, "Updated Title")
	}
}

func TestIntegration_DeleteBook(t *testing.T) {
	server := setupTestServer()

	// Create a book
	book := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "To Delete",
		Author: "Author",
		Price:  19.99,
	}
	body, _ := json.Marshal(book)

	req := httptest.NewRequest("POST", "/books", bytes.NewReader(body))
	rec := httptest.NewRecorder()
	server.ServeHTTP(rec, req)

	var createdBook domain.Book
	json.NewDecoder(rec.Body).Decode(&createdBook)

	// Delete the book
	req = httptest.NewRequest("DELETE", "/books/"+createdBook.ID, nil)
	rec = httptest.NewRecorder()
	server.ServeHTTP(rec, req)

	if rec.Code != http.StatusNoContent {
		t.Fatalf("Delete book: got %d, want %d", rec.Code, http.StatusNoContent)
	}

	// Verify deletion
	req = httptest.NewRequest("GET", "/books/"+createdBook.ID, nil)
	rec = httptest.NewRecorder()
	server.ServeHTTP(rec, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("Get deleted book: got %d, want %d", rec.Code, http.StatusNotFound)
	}
}

func TestIntegration_GetAllBooks(t *testing.T) {
	server := setupTestServer()

	// Create multiple books
	books := []domain.Book{
		{ISBN: "111", Title: "Book 1", Author: "Author 1", Price: 10},
		{ISBN: "222", Title: "Book 2", Author: "Author 2", Price: 20},
		{ISBN: "333", Title: "Book 3", Author: "Author 3", Price: 30},
	}

	for _, book := range books {
		body, _ := json.Marshal(book)
		req := httptest.NewRequest("POST", "/books", bytes.NewReader(body))
		rec := httptest.NewRecorder()
		server.ServeHTTP(rec, req)
	}

	// Get all books
	req := httptest.NewRequest("GET", "/books", nil)
	rec := httptest.NewRecorder()
	server.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("GetAll: got %d, want %d", rec.Code, http.StatusOK)
	}

	var fetchedBooks []domain.Book
	json.NewDecoder(rec.Body).Decode(&fetchedBooks)

	if len(fetchedBooks) != 3 {
		t.Errorf("GetAll count = %d, want %d", len(fetchedBooks), 3)
	}
}

func TestRoutes_MethodNotAllowed(t *testing.T) {
	server := setupTestServer()

	tests := []struct {
		name   string
		method string
		path   string
	}{
		{"PATCH /books", "PATCH", "/books"},
		{"OPTIONS /books", "OPTIONS", "/books"},
		{"PATCH /books/1", "PATCH", "/books/1"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest(tt.method, tt.path, nil)
			rec := httptest.NewRecorder()
			server.ServeHTTP(rec, req)

			if rec.Code != http.StatusMethodNotAllowed {
				t.Errorf("%s %s: got %d, want %d", tt.method, tt.path, rec.Code, http.StatusMethodNotAllowed)
			}
		})
	}
}
