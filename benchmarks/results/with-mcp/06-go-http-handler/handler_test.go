package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
)

func setupTestRouter() (*chi.Mux, *BookService) {
	repo := NewInMemoryBookRepository()
	service := NewBookService(repo)
	handler := NewBookHandler(service)

	r := chi.NewRouter()
	handler.RegisterRoutes(r)

	return r, service
}

func createTestBook(t *testing.T, r *chi.Mux) string {
	t.Helper()

	body := `{"title":"Test Book","author":"Test Author","isbn":"1234567890","published_year":2020,"genre":"Fiction"}`
	req := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	r.ServeHTTP(w, req)

	var resp BookResponse
	json.NewDecoder(w.Body).Decode(&resp)
	return resp.ID
}

func TestCreateBook(t *testing.T) {
	tests := []struct {
		name           string
		body           string
		expectedStatus int
		expectedError  string
	}{
		{
			name:           "should_create_book_when_valid_data",
			body:           `{"title":"Go Programming","author":"John Doe","isbn":"1234567890","published_year":2020,"genre":"Technology"}`,
			expectedStatus: http.StatusCreated,
		},
		{
			name:           "should_fail_when_title_missing",
			body:           `{"author":"John Doe","isbn":"1234567890","published_year":2020}`,
			expectedStatus: http.StatusBadRequest,
			expectedError:  "title is required",
		},
		{
			name:           "should_fail_when_author_missing",
			body:           `{"title":"Go Programming","isbn":"1234567890","published_year":2020}`,
			expectedStatus: http.StatusBadRequest,
			expectedError:  "author is required",
		},
		{
			name:           "should_fail_when_isbn_invalid",
			body:           `{"title":"Go Programming","author":"John Doe","isbn":"123","published_year":2020}`,
			expectedStatus: http.StatusBadRequest,
			expectedError:  "invalid ISBN format",
		},
		{
			name:           "should_fail_when_year_before_1450",
			body:           `{"title":"Go Programming","author":"John Doe","isbn":"1234567890","published_year":1400}`,
			expectedStatus: http.StatusBadRequest,
			expectedError:  "published year",
		},
		{
			name:           "should_accept_isbn13",
			body:           `{"title":"Go Programming","author":"John Doe","isbn":"1234567890123","published_year":2020,"genre":"Tech"}`,
			expectedStatus: http.StatusCreated,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, _ := setupTestRouter()
			req := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(tt.body))
			req.Header.Set("Content-Type", "application/json")
			w := httptest.NewRecorder()

			r.ServeHTTP(w, req)

			if w.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, w.Code)
			}

			if tt.expectedError != "" {
				var errResp ErrorResponse
				json.NewDecoder(w.Body).Decode(&errResp)
				if errResp.Error != tt.expectedError && !bytes.Contains([]byte(errResp.Error), []byte(tt.expectedError)) {
					t.Errorf("expected error containing %q, got %q", tt.expectedError, errResp.Error)
				}
			}
		})
	}
}

func TestGetBook(t *testing.T) {
	tests := []struct {
		name           string
		setupBook      bool
		useID          string
		expectedStatus int
	}{
		{
			name:           "should_return_book_when_exists",
			setupBook:      true,
			expectedStatus: http.StatusOK,
		},
		{
			name:           "should_return_404_when_not_found",
			setupBook:      false,
			useID:          "non-existent-id",
			expectedStatus: http.StatusNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, _ := setupTestRouter()

			var id string
			if tt.setupBook {
				id = createTestBook(t, r)
			} else {
				id = tt.useID
			}

			req := httptest.NewRequest(http.MethodGet, "/books/"+id, nil)
			w := httptest.NewRecorder()

			r.ServeHTTP(w, req)

			if w.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, w.Code)
			}
		})
	}
}

func TestGetBooks(t *testing.T) {
	tests := []struct {
		name           string
		queryParam     string
		expectedStatus int
	}{
		{
			name:           "should_return_all_books",
			queryParam:     "",
			expectedStatus: http.StatusOK,
		},
		{
			name:           "should_filter_by_author",
			queryParam:     "?author=Test Author",
			expectedStatus: http.StatusOK,
		},
		{
			name:           "should_filter_by_genre",
			queryParam:     "?genre=Fiction",
			expectedStatus: http.StatusOK,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, _ := setupTestRouter()
			createTestBook(t, r)

			req := httptest.NewRequest(http.MethodGet, "/books"+tt.queryParam, nil)
			w := httptest.NewRecorder()

			r.ServeHTTP(w, req)

			if w.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, w.Code)
			}
		})
	}
}

func TestBorrowBook(t *testing.T) {
	tests := []struct {
		name           string
		borrowTwice    bool
		expectedStatus int
	}{
		{
			name:           "should_borrow_available_book",
			borrowTwice:    false,
			expectedStatus: http.StatusOK,
		},
		{
			name:           "should_fail_when_book_already_borrowed",
			borrowTwice:    true,
			expectedStatus: http.StatusConflict,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, _ := setupTestRouter()
			id := createTestBook(t, r)

			if tt.borrowTwice {
				req := httptest.NewRequest(http.MethodPost, "/books/"+id+"/borrow", nil)
				w := httptest.NewRecorder()
				r.ServeHTTP(w, req)
			}

			req := httptest.NewRequest(http.MethodPost, "/books/"+id+"/borrow", nil)
			w := httptest.NewRecorder()

			r.ServeHTTP(w, req)

			if w.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, w.Code)
			}
		})
	}
}

func TestReturnBook(t *testing.T) {
	tests := []struct {
		name           string
		borrowFirst    bool
		expectedStatus int
	}{
		{
			name:           "should_return_borrowed_book",
			borrowFirst:    true,
			expectedStatus: http.StatusOK,
		},
		{
			name:           "should_fail_when_book_not_borrowed",
			borrowFirst:    false,
			expectedStatus: http.StatusConflict,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, _ := setupTestRouter()
			id := createTestBook(t, r)

			if tt.borrowFirst {
				req := httptest.NewRequest(http.MethodPost, "/books/"+id+"/borrow", nil)
				w := httptest.NewRecorder()
				r.ServeHTTP(w, req)
			}

			req := httptest.NewRequest(http.MethodPost, "/books/"+id+"/return", nil)
			w := httptest.NewRecorder()

			r.ServeHTTP(w, req)

			if w.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, w.Code)
			}
		})
	}
}

func TestDeleteBook(t *testing.T) {
	tests := []struct {
		name           string
		setupBook      bool
		useID          string
		expectedStatus int
	}{
		{
			name:           "should_delete_existing_book",
			setupBook:      true,
			expectedStatus: http.StatusNoContent,
		},
		{
			name:           "should_return_404_when_not_found",
			setupBook:      false,
			useID:          "non-existent-id",
			expectedStatus: http.StatusNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, _ := setupTestRouter()

			var id string
			if tt.setupBook {
				id = createTestBook(t, r)
			} else {
				id = tt.useID
			}

			req := httptest.NewRequest(http.MethodDelete, "/books/"+id, nil)
			w := httptest.NewRecorder()

			r.ServeHTTP(w, req)

			if w.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, w.Code)
			}
		})
	}
}

func TestUpdateBook(t *testing.T) {
	tests := []struct {
		name           string
		body           string
		setupBook      bool
		useID          string
		expectedStatus int
	}{
		{
			name:           "should_update_book_title",
			body:           `{"title":"Updated Title"}`,
			setupBook:      true,
			expectedStatus: http.StatusOK,
		},
		{
			name:           "should_fail_with_invalid_isbn",
			body:           `{"isbn":"invalid"}`,
			setupBook:      true,
			expectedStatus: http.StatusBadRequest,
		},
		{
			name:           "should_return_404_when_not_found",
			body:           `{"title":"Updated"}`,
			setupBook:      false,
			useID:          "non-existent-id",
			expectedStatus: http.StatusNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r, _ := setupTestRouter()

			var id string
			if tt.setupBook {
				id = createTestBook(t, r)
			} else {
				id = tt.useID
			}

			req := httptest.NewRequest(http.MethodPut, "/books/"+id, bytes.NewBufferString(tt.body))
			req.Header.Set("Content-Type", "application/json")
			w := httptest.NewRecorder()

			r.ServeHTTP(w, req)

			if w.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, w.Code)
			}
		})
	}
}

func TestValidateISBN(t *testing.T) {
	tests := []struct {
		name    string
		isbn    string
		wantErr bool
	}{
		{name: "should_accept_valid_isbn10", isbn: "1234567890", wantErr: false},
		{name: "should_accept_valid_isbn13", isbn: "1234567890123", wantErr: false},
		{name: "should_reject_too_short", isbn: "123456789", wantErr: true},
		{name: "should_reject_too_long", isbn: "12345678901234", wantErr: true},
		{name: "should_reject_with_letters", isbn: "123456789X", wantErr: true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := ValidateISBN(tt.isbn)
			if (err != nil) != tt.wantErr {
				t.Errorf("ValidateISBN(%q) error = %v, wantErr %v", tt.isbn, err, tt.wantErr)
			}
		})
	}
}

func TestValidatePublishedYear(t *testing.T) {
	tests := []struct {
		name    string
		year    int
		wantErr bool
	}{
		{name: "should_accept_current_year", year: 2024, wantErr: false},
		{name: "should_accept_1450", year: 1450, wantErr: false},
		{name: "should_reject_before_1450", year: 1449, wantErr: true},
		{name: "should_reject_future_year", year: 3000, wantErr: true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := ValidatePublishedYear(tt.year)
			if (err != nil) != tt.wantErr {
				t.Errorf("ValidatePublishedYear(%d) error = %v, wantErr %v", tt.year, err, tt.wantErr)
			}
		})
	}
}
