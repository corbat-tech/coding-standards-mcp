package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func setupTestHandler() (*BookHandler, *BookRepository) {
	repo := NewBookRepository()
	handler := NewBookHandler(repo)
	return handler, repo
}

func createTestBook(t *testing.T, handler *BookHandler, repo *BookRepository) *Book {
	t.Helper()
	book, _ := repo.Create(&CreateBookRequest{
		Title:         "Test Book",
		Author:        "Test Author",
		ISBN:          "9780123456789",
		PublishedYear: 2020,
		Genre:         "Fiction",
	})
	return book
}

func TestCreateBook(t *testing.T) {
	tests := []struct {
		name           string
		request        CreateBookRequest
		expectedStatus int
		expectError    bool
	}{
		{
			name: "valid book",
			request: CreateBookRequest{
				Title:         "The Great Gatsby",
				Author:        "F. Scott Fitzgerald",
				ISBN:          "9780743273565",
				PublishedYear: 1925,
				Genre:         "Fiction",
			},
			expectedStatus: http.StatusCreated,
			expectError:    false,
		},
		{
			name: "valid ISBN-10",
			request: CreateBookRequest{
				Title:         "Test Book",
				Author:        "Test Author",
				ISBN:          "0123456789",
				PublishedYear: 2000,
				Genre:         "Fiction",
			},
			expectedStatus: http.StatusCreated,
			expectError:    false,
		},
		{
			name: "empty title",
			request: CreateBookRequest{
				Author:        "Test Author",
				ISBN:          "9780123456789",
				PublishedYear: 2020,
				Genre:         "Fiction",
			},
			expectedStatus: http.StatusBadRequest,
			expectError:    true,
		},
		{
			name: "empty author",
			request: CreateBookRequest{
				Title:         "Test Book",
				ISBN:          "9780123456789",
				PublishedYear: 2020,
				Genre:         "Fiction",
			},
			expectedStatus: http.StatusBadRequest,
			expectError:    true,
		},
		{
			name: "invalid ISBN - too short",
			request: CreateBookRequest{
				Title:         "Test Book",
				Author:        "Test Author",
				ISBN:          "123456",
				PublishedYear: 2020,
				Genre:         "Fiction",
			},
			expectedStatus: http.StatusBadRequest,
			expectError:    true,
		},
		{
			name: "invalid year - too early",
			request: CreateBookRequest{
				Title:         "Test Book",
				Author:        "Test Author",
				ISBN:          "9780123456789",
				PublishedYear: 1400,
				Genre:         "Fiction",
			},
			expectedStatus: http.StatusBadRequest,
			expectError:    true,
		},
		{
			name: "invalid year - future",
			request: CreateBookRequest{
				Title:         "Test Book",
				Author:        "Test Author",
				ISBN:          "9780123456789",
				PublishedYear: time.Now().Year() + 1,
				Genre:         "Fiction",
			},
			expectedStatus: http.StatusBadRequest,
			expectError:    true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			handler, _ := setupTestHandler()
			router := SetupRoutes(handler)

			body, _ := json.Marshal(tt.request)
			req := httptest.NewRequest(http.MethodPost, "/books/", bytes.NewBuffer(body))
			req.Header.Set("Content-Type", "application/json")
			rec := httptest.NewRecorder()

			router.ServeHTTP(rec, req)

			if rec.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, rec.Code)
			}

			if !tt.expectError {
				var book Book
				json.NewDecoder(rec.Body).Decode(&book)
				if book.ID == "" {
					t.Error("expected book ID to be set")
				}
				if book.Available != true {
					t.Error("expected book to be available")
				}
			}
		})
	}
}

func TestGetBook(t *testing.T) {
	handler, repo := setupTestHandler()
	router := SetupRoutes(handler)

	t.Run("existing book", func(t *testing.T) {
		book := createTestBook(t, handler, repo)

		req := httptest.NewRequest(http.MethodGet, "/books/"+book.ID, nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var result Book
		json.NewDecoder(rec.Body).Decode(&result)
		if result.ID != book.ID {
			t.Errorf("expected book ID %s, got %s", book.ID, result.ID)
		}
	})

	t.Run("non-existent book", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/books/nonexistent-id", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusNotFound {
			t.Errorf("expected status %d, got %d", http.StatusNotFound, rec.Code)
		}
	})
}

func TestGetBooks(t *testing.T) {
	handler, repo := setupTestHandler()
	router := SetupRoutes(handler)

	// Create test books
	repo.Create(&CreateBookRequest{
		Title:         "Book One",
		Author:        "Author A",
		ISBN:          "9780123456789",
		PublishedYear: 2020,
		Genre:         "Fiction",
	})
	repo.Create(&CreateBookRequest{
		Title:         "Book Two",
		Author:        "Author B",
		ISBN:          "9780987654321",
		PublishedYear: 2021,
		Genre:         "Science",
	})

	t.Run("get all books", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/books/", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var books []*Book
		json.NewDecoder(rec.Body).Decode(&books)
		if len(books) != 2 {
			t.Errorf("expected 2 books, got %d", len(books))
		}
	})

	t.Run("filter by author", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/books/?author=Author+A", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var books []*Book
		json.NewDecoder(rec.Body).Decode(&books)
		if len(books) != 1 {
			t.Errorf("expected 1 book, got %d", len(books))
		}
	})

	t.Run("filter by genre", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/books/?genre=Science", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var books []*Book
		json.NewDecoder(rec.Body).Decode(&books)
		if len(books) != 1 {
			t.Errorf("expected 1 book, got %d", len(books))
		}
	})
}

func TestUpdateBook(t *testing.T) {
	handler, repo := setupTestHandler()
	router := SetupRoutes(handler)
	book := createTestBook(t, handler, repo)

	t.Run("valid update", func(t *testing.T) {
		newTitle := "Updated Title"
		updateReq := UpdateBookRequest{Title: &newTitle}
		body, _ := json.Marshal(updateReq)

		req := httptest.NewRequest(http.MethodPut, "/books/"+book.ID, bytes.NewBuffer(body))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var result Book
		json.NewDecoder(rec.Body).Decode(&result)
		if result.Title != newTitle {
			t.Errorf("expected title %s, got %s", newTitle, result.Title)
		}
	})

	t.Run("invalid ISBN update", func(t *testing.T) {
		invalidISBN := "123"
		updateReq := UpdateBookRequest{ISBN: &invalidISBN}
		body, _ := json.Marshal(updateReq)

		req := httptest.NewRequest(http.MethodPut, "/books/"+book.ID, bytes.NewBuffer(body))
		req.Header.Set("Content-Type", "application/json")
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
		}
	})
}

func TestDeleteBook(t *testing.T) {
	handler, repo := setupTestHandler()
	router := SetupRoutes(handler)

	t.Run("delete existing book", func(t *testing.T) {
		book := createTestBook(t, handler, repo)

		req := httptest.NewRequest(http.MethodDelete, "/books/"+book.ID, nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusNoContent {
			t.Errorf("expected status %d, got %d", http.StatusNoContent, rec.Code)
		}

		// Verify deletion
		_, err := repo.GetByID(book.ID)
		if err != ErrBookNotFound {
			t.Error("expected book to be deleted")
		}
	})

	t.Run("delete non-existent book", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodDelete, "/books/nonexistent-id", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusNotFound {
			t.Errorf("expected status %d, got %d", http.StatusNotFound, rec.Code)
		}
	})
}

func TestBorrowBook(t *testing.T) {
	handler, repo := setupTestHandler()
	router := SetupRoutes(handler)

	t.Run("borrow available book", func(t *testing.T) {
		book := createTestBook(t, handler, repo)

		req := httptest.NewRequest(http.MethodPost, "/books/"+book.ID+"/borrow", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var result Book
		json.NewDecoder(rec.Body).Decode(&result)
		if result.Available {
			t.Error("expected book to be unavailable after borrowing")
		}
	})

	t.Run("borrow unavailable book", func(t *testing.T) {
		book := createTestBook(t, handler, repo)
		repo.Borrow(book.ID) // Borrow first

		req := httptest.NewRequest(http.MethodPost, "/books/"+book.ID+"/borrow", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusConflict {
			t.Errorf("expected status %d, got %d", http.StatusConflict, rec.Code)
		}
	})

	t.Run("borrow non-existent book", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodPost, "/books/nonexistent-id/borrow", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusNotFound {
			t.Errorf("expected status %d, got %d", http.StatusNotFound, rec.Code)
		}
	})
}

func TestReturnBook(t *testing.T) {
	handler, repo := setupTestHandler()
	router := SetupRoutes(handler)

	t.Run("return borrowed book", func(t *testing.T) {
		book := createTestBook(t, handler, repo)
		repo.Borrow(book.ID) // Borrow first

		req := httptest.NewRequest(http.MethodPost, "/books/"+book.ID+"/return", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var result Book
		json.NewDecoder(rec.Body).Decode(&result)
		if !result.Available {
			t.Error("expected book to be available after returning")
		}
	})

	t.Run("return available book", func(t *testing.T) {
		book := createTestBook(t, handler, repo)

		req := httptest.NewRequest(http.MethodPost, "/books/"+book.ID+"/return", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusConflict {
			t.Errorf("expected status %d, got %d", http.StatusConflict, rec.Code)
		}
	})

	t.Run("return non-existent book", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodPost, "/books/nonexistent-id/return", nil)
		rec := httptest.NewRecorder()

		router.ServeHTTP(rec, req)

		if rec.Code != http.StatusNotFound {
			t.Errorf("expected status %d, got %d", http.StatusNotFound, rec.Code)
		}
	})
}

func TestValidateISBN(t *testing.T) {
	tests := []struct {
		name     string
		isbn     string
		expected bool
	}{
		{"valid ISBN-13", "9780123456789", true},
		{"valid ISBN-10", "0123456789", true},
		{"valid ISBN-10 with X", "012345678X", true},
		{"ISBN-13 with hyphens", "978-0-12-345678-9", true},
		{"too short", "123456", false},
		{"too long", "12345678901234", false},
		{"with letters", "978012345678a", false},
		{"empty", "", false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := ValidateISBN(tt.isbn)
			if result != tt.expected {
				t.Errorf("ValidateISBN(%q) = %v, expected %v", tt.isbn, result, tt.expected)
			}
		})
	}
}

func TestValidateYear(t *testing.T) {
	tests := []struct {
		name     string
		year     int
		expected bool
	}{
		{"valid year 2020", 2020, true},
		{"valid year 1450", 1450, true},
		{"current year", time.Now().Year(), true},
		{"too early", 1449, false},
		{"future year", time.Now().Year() + 1, false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := ValidateYear(tt.year)
			if result != tt.expected {
				t.Errorf("ValidateYear(%d) = %v, expected %v", tt.year, result, tt.expected)
			}
		})
	}
}
