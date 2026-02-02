package handler

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/example/bookstore/model"
	"github.com/example/bookstore/repository"
)

func TestBookHandler(t *testing.T) {
	tests := []struct {
		name           string
		method         string
		path           string
		body           interface{}
		setup          func(*repository.InMemoryBookRepository)
		expectedStatus int
		checkResponse  func(*testing.T, *httptest.ResponseRecorder)
	}{
		{
			name:           "GET all books - empty",
			method:         http.MethodGet,
			path:           "/books",
			expectedStatus: http.StatusOK,
			checkResponse: func(t *testing.T, rr *httptest.ResponseRecorder) {
				var books []*model.Book
				json.NewDecoder(rr.Body).Decode(&books)
				if len(books) != 0 {
					t.Errorf("expected 0 books, got %d", len(books))
				}
			},
		},
		{
			name:   "POST create book",
			method: http.MethodPost,
			path:   "/books",
			body: model.CreateBookRequest{
				Title:  "Test Book",
				Author: "Test Author",
				ISBN:   "1234567890",
				Price:  29.99,
			},
			expectedStatus: http.StatusCreated,
			checkResponse: func(t *testing.T, rr *httptest.ResponseRecorder) {
				var book model.Book
				json.NewDecoder(rr.Body).Decode(&book)
				if book.Title != "Test Book" {
					t.Errorf("expected title 'Test Book', got '%s'", book.Title)
				}
			},
		},
		{
			name:           "POST create book - missing fields",
			method:         http.MethodPost,
			path:           "/books",
			body:           model.CreateBookRequest{Title: "Only Title"},
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "GET book - not found",
			method:         http.MethodGet,
			path:           "/books/nonexistent",
			expectedStatus: http.StatusNotFound,
		},
		{
			name:   "GET book by ID",
			method: http.MethodGet,
			path:   "/books/test-id",
			setup: func(repo *repository.InMemoryBookRepository) {
				repo.Create(&model.Book{
					ID:     "test-id",
					Title:  "Existing Book",
					Author: "Author",
					ISBN:   "9999999999",
				})
			},
			expectedStatus: http.StatusOK,
			checkResponse: func(t *testing.T, rr *httptest.ResponseRecorder) {
				var book model.Book
				json.NewDecoder(rr.Body).Decode(&book)
				if book.Title != "Existing Book" {
					t.Errorf("expected title 'Existing Book', got '%s'", book.Title)
				}
			},
		},
		{
			name:   "PUT update book",
			method: http.MethodPut,
			path:   "/books/update-id",
			body:   map[string]interface{}{"title": "Updated Title"},
			setup: func(repo *repository.InMemoryBookRepository) {
				repo.Create(&model.Book{
					ID:     "update-id",
					Title:  "Original",
					Author: "Author",
					ISBN:   "1111111111",
				})
			},
			expectedStatus: http.StatusOK,
			checkResponse: func(t *testing.T, rr *httptest.ResponseRecorder) {
				var book model.Book
				json.NewDecoder(rr.Body).Decode(&book)
				if book.Title != "Updated Title" {
					t.Errorf("expected title 'Updated Title', got '%s'", book.Title)
				}
			},
		},
		{
			name:   "DELETE book",
			method: http.MethodDelete,
			path:   "/books/delete-id",
			setup: func(repo *repository.InMemoryBookRepository) {
				repo.Create(&model.Book{
					ID:     "delete-id",
					Title:  "To Delete",
					Author: "Author",
					ISBN:   "2222222222",
				})
			},
			expectedStatus: http.StatusNoContent,
		},
		{
			name:           "DELETE book - not found",
			method:         http.MethodDelete,
			path:           "/books/nonexistent",
			expectedStatus: http.StatusNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := repository.NewInMemoryBookRepository()
			if tt.setup != nil {
				tt.setup(repo)
			}

			handler := NewBookHandler(repo)

			var body bytes.Buffer
			if tt.body != nil {
				json.NewEncoder(&body).Encode(tt.body)
			}

			req := httptest.NewRequest(tt.method, tt.path, &body)
			req.Header.Set("Content-Type", "application/json")
			rr := httptest.NewRecorder()

			handler.ServeHTTP(rr, req)

			if rr.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, rr.Code)
			}

			if tt.checkResponse != nil {
				tt.checkResponse(t, rr)
			}
		})
	}
}
