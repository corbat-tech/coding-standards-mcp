package handlers

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"bookstore/models"
	"bookstore/store"
)

func TestBookHandler_Create(t *testing.T) {
	tests := []struct {
		name           string
		requestBody    interface{}
		expectedStatus int
		expectedError  string
	}{
		{
			name: "valid book creation",
			requestBody: models.CreateBookRequest{
				Title:    "The Go Programming Language",
				Author:   "Alan Donovan",
				ISBN:     "978-0134190440",
				Price:    44.99,
				Quantity: 10,
			},
			expectedStatus: http.StatusCreated,
		},
		{
			name: "missing title",
			requestBody: models.CreateBookRequest{
				Author:   "Alan Donovan",
				ISBN:     "978-0134190440",
				Price:    44.99,
				Quantity: 10,
			},
			expectedStatus: http.StatusBadRequest,
			expectedError:  "validation_error",
		},
		{
			name: "missing author",
			requestBody: models.CreateBookRequest{
				Title:    "The Go Programming Language",
				ISBN:     "978-0134190440",
				Price:    44.99,
				Quantity: 10,
			},
			expectedStatus: http.StatusBadRequest,
			expectedError:  "validation_error",
		},
		{
			name: "missing ISBN",
			requestBody: models.CreateBookRequest{
				Title:    "The Go Programming Language",
				Author:   "Alan Donovan",
				Price:    44.99,
				Quantity: 10,
			},
			expectedStatus: http.StatusBadRequest,
			expectedError:  "validation_error",
		},
		{
			name: "invalid price",
			requestBody: models.CreateBookRequest{
				Title:    "The Go Programming Language",
				Author:   "Alan Donovan",
				ISBN:     "978-0134190440",
				Price:    0,
				Quantity: 10,
			},
			expectedStatus: http.StatusBadRequest,
			expectedError:  "validation_error",
		},
		{
			name: "negative quantity",
			requestBody: models.CreateBookRequest{
				Title:    "The Go Programming Language",
				Author:   "Alan Donovan",
				ISBN:     "978-0134190440",
				Price:    44.99,
				Quantity: -1,
			},
			expectedStatus: http.StatusBadRequest,
			expectedError:  "validation_error",
		},
		{
			name:           "invalid JSON",
			requestBody:    "invalid json",
			expectedStatus: http.StatusBadRequest,
			expectedError:  "invalid_json",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := store.NewBookStore()
			h := NewBookHandler(s)

			var body []byte
			if str, ok := tt.requestBody.(string); ok {
				body = []byte(str)
			} else {
				body, _ = json.Marshal(tt.requestBody)
			}

			req := httptest.NewRequest(http.MethodPost, "/api/books", bytes.NewReader(body))
			req.Header.Set("Content-Type", "application/json")
			rec := httptest.NewRecorder()

			h.ServeHTTP(rec, req)

			if rec.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, rec.Code)
			}

			if tt.expectedError != "" {
				var errResp ErrorResponse
				json.NewDecoder(rec.Body).Decode(&errResp)
				if errResp.Error != tt.expectedError {
					t.Errorf("expected error %q, got %q", tt.expectedError, errResp.Error)
				}
			}
		})
	}
}

func TestBookHandler_Get(t *testing.T) {
	tests := []struct {
		name           string
		setupStore     func(*store.BookStore) string
		bookID         string
		expectedStatus int
		expectedError  string
	}{
		{
			name: "existing book",
			setupStore: func(s *store.BookStore) string {
				book, _ := s.Create(&models.CreateBookRequest{
					Title:    "Test Book",
					Author:   "Test Author",
					ISBN:     "123-456",
					Price:    19.99,
					Quantity: 5,
				})
				return book.ID
			},
			expectedStatus: http.StatusOK,
		},
		{
			name: "non-existing book",
			setupStore: func(s *store.BookStore) string {
				return "non-existing-id"
			},
			bookID:         "non-existing-id",
			expectedStatus: http.StatusNotFound,
			expectedError:  "not_found",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := store.NewBookStore()
			h := NewBookHandler(s)

			bookID := tt.setupStore(s)
			if tt.bookID != "" {
				bookID = tt.bookID
			}

			req := httptest.NewRequest(http.MethodGet, "/api/books/"+bookID, nil)
			rec := httptest.NewRecorder()

			h.ServeHTTP(rec, req)

			if rec.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, rec.Code)
			}

			if tt.expectedError != "" {
				var errResp ErrorResponse
				json.NewDecoder(rec.Body).Decode(&errResp)
				if errResp.Error != tt.expectedError {
					t.Errorf("expected error %q, got %q", tt.expectedError, errResp.Error)
				}
			}
		})
	}
}

func TestBookHandler_List(t *testing.T) {
	tests := []struct {
		name          string
		setupStore    func(*store.BookStore)
		expectedCount int
	}{
		{
			name:          "empty store",
			setupStore:    func(s *store.BookStore) {},
			expectedCount: 0,
		},
		{
			name: "multiple books",
			setupStore: func(s *store.BookStore) {
				s.Create(&models.CreateBookRequest{
					Title: "Book 1", Author: "Author 1", ISBN: "111", Price: 10.00, Quantity: 1,
				})
				s.Create(&models.CreateBookRequest{
					Title: "Book 2", Author: "Author 2", ISBN: "222", Price: 20.00, Quantity: 2,
				})
				s.Create(&models.CreateBookRequest{
					Title: "Book 3", Author: "Author 3", ISBN: "333", Price: 30.00, Quantity: 3,
				})
			},
			expectedCount: 3,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := store.NewBookStore()
			h := NewBookHandler(s)

			tt.setupStore(s)

			req := httptest.NewRequest(http.MethodGet, "/api/books", nil)
			rec := httptest.NewRecorder()

			h.ServeHTTP(rec, req)

			if rec.Code != http.StatusOK {
				t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
			}

			var resp ListResponse
			json.NewDecoder(rec.Body).Decode(&resp)
			if resp.Count != tt.expectedCount {
				t.Errorf("expected count %d, got %d", tt.expectedCount, resp.Count)
			}
		})
	}
}

func TestBookHandler_Update(t *testing.T) {
	newTitle := "Updated Title"
	newPrice := 99.99

	tests := []struct {
		name           string
		setupStore     func(*store.BookStore) string
		bookID         string
		requestBody    interface{}
		expectedStatus int
		expectedError  string
	}{
		{
			name: "update title",
			setupStore: func(s *store.BookStore) string {
				book, _ := s.Create(&models.CreateBookRequest{
					Title: "Original", Author: "Author", ISBN: "123", Price: 10.00, Quantity: 1,
				})
				return book.ID
			},
			requestBody:    models.UpdateBookRequest{Title: &newTitle},
			expectedStatus: http.StatusOK,
		},
		{
			name: "update price",
			setupStore: func(s *store.BookStore) string {
				book, _ := s.Create(&models.CreateBookRequest{
					Title: "Book", Author: "Author", ISBN: "456", Price: 10.00, Quantity: 1,
				})
				return book.ID
			},
			requestBody:    models.UpdateBookRequest{Price: &newPrice},
			expectedStatus: http.StatusOK,
		},
		{
			name: "non-existing book",
			setupStore: func(s *store.BookStore) string {
				return "non-existing-id"
			},
			bookID:         "non-existing-id",
			requestBody:    models.UpdateBookRequest{Title: &newTitle},
			expectedStatus: http.StatusNotFound,
			expectedError:  "not_found",
		},
		{
			name: "invalid JSON",
			setupStore: func(s *store.BookStore) string {
				book, _ := s.Create(&models.CreateBookRequest{
					Title: "Book", Author: "Author", ISBN: "789", Price: 10.00, Quantity: 1,
				})
				return book.ID
			},
			requestBody:    "invalid json",
			expectedStatus: http.StatusBadRequest,
			expectedError:  "invalid_json",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := store.NewBookStore()
			h := NewBookHandler(s)

			bookID := tt.setupStore(s)
			if tt.bookID != "" {
				bookID = tt.bookID
			}

			var body []byte
			if str, ok := tt.requestBody.(string); ok {
				body = []byte(str)
			} else {
				body, _ = json.Marshal(tt.requestBody)
			}

			req := httptest.NewRequest(http.MethodPut, "/api/books/"+bookID, bytes.NewReader(body))
			req.Header.Set("Content-Type", "application/json")
			rec := httptest.NewRecorder()

			h.ServeHTTP(rec, req)

			if rec.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, rec.Code)
			}

			if tt.expectedError != "" {
				var errResp ErrorResponse
				json.NewDecoder(rec.Body).Decode(&errResp)
				if errResp.Error != tt.expectedError {
					t.Errorf("expected error %q, got %q", tt.expectedError, errResp.Error)
				}
			}
		})
	}
}

func TestBookHandler_Delete(t *testing.T) {
	tests := []struct {
		name           string
		setupStore     func(*store.BookStore) string
		bookID         string
		expectedStatus int
		expectedError  string
	}{
		{
			name: "delete existing book",
			setupStore: func(s *store.BookStore) string {
				book, _ := s.Create(&models.CreateBookRequest{
					Title: "Book to Delete", Author: "Author", ISBN: "delete-123", Price: 10.00, Quantity: 1,
				})
				return book.ID
			},
			expectedStatus: http.StatusNoContent,
		},
		{
			name: "delete non-existing book",
			setupStore: func(s *store.BookStore) string {
				return "non-existing-id"
			},
			bookID:         "non-existing-id",
			expectedStatus: http.StatusNotFound,
			expectedError:  "not_found",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := store.NewBookStore()
			h := NewBookHandler(s)

			bookID := tt.setupStore(s)
			if tt.bookID != "" {
				bookID = tt.bookID
			}

			req := httptest.NewRequest(http.MethodDelete, "/api/books/"+bookID, nil)
			rec := httptest.NewRecorder()

			h.ServeHTTP(rec, req)

			if rec.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, rec.Code)
			}

			if tt.expectedError != "" {
				var errResp ErrorResponse
				json.NewDecoder(rec.Body).Decode(&errResp)
				if errResp.Error != tt.expectedError {
					t.Errorf("expected error %q, got %q", tt.expectedError, errResp.Error)
				}
			}
		})
	}
}

func TestBookHandler_DuplicateISBN(t *testing.T) {
	s := store.NewBookStore()
	h := NewBookHandler(s)

	// Create first book
	firstBook := models.CreateBookRequest{
		Title:    "First Book",
		Author:   "Author",
		ISBN:     "duplicate-isbn",
		Price:    10.00,
		Quantity: 1,
	}
	body, _ := json.Marshal(firstBook)
	req := httptest.NewRequest(http.MethodPost, "/api/books", bytes.NewReader(body))
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusCreated {
		t.Fatalf("expected first book creation to succeed")
	}

	// Try to create second book with same ISBN
	secondBook := models.CreateBookRequest{
		Title:    "Second Book",
		Author:   "Another Author",
		ISBN:     "duplicate-isbn",
		Price:    20.00,
		Quantity: 2,
	}
	body, _ = json.Marshal(secondBook)
	req = httptest.NewRequest(http.MethodPost, "/api/books", bytes.NewReader(body))
	rec = httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusConflict {
		t.Errorf("expected status %d for duplicate ISBN, got %d", http.StatusConflict, rec.Code)
	}

	var errResp ErrorResponse
	json.NewDecoder(rec.Body).Decode(&errResp)
	if errResp.Error != "duplicate_isbn" {
		t.Errorf("expected error 'duplicate_isbn', got %q", errResp.Error)
	}
}

func TestBookHandler_MethodNotAllowed(t *testing.T) {
	s := store.NewBookStore()
	h := NewBookHandler(s)

	// PATCH is not allowed
	req := httptest.NewRequest(http.MethodPatch, "/api/books", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)

	if rec.Code != http.StatusMethodNotAllowed {
		t.Errorf("expected status %d, got %d", http.StatusMethodNotAllowed, rec.Code)
	}
}
