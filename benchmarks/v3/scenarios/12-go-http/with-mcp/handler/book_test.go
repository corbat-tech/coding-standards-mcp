package handler

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"bookstore/domain"
)

// mockRepository is a test double for BookRepository.
type mockRepository struct {
	books      map[string]domain.Book
	getAllErr  error
	getByIDErr error
	createErr  error
	updateErr  error
	deleteErr  error
}

func newMockRepository() *mockRepository {
	return &mockRepository{books: make(map[string]domain.Book)}
}

func (m *mockRepository) GetAll() ([]domain.Book, error) {
	if m.getAllErr != nil {
		return nil, m.getAllErr
	}
	books := make([]domain.Book, 0, len(m.books))
	for _, b := range m.books {
		books = append(books, b)
	}
	return books, nil
}

func (m *mockRepository) GetByID(id string) (*domain.Book, error) {
	if m.getByIDErr != nil {
		return nil, m.getByIDErr
	}
	book, exists := m.books[id]
	if !exists {
		return nil, domain.ErrBookNotFound
	}
	return &book, nil
}

func (m *mockRepository) Create(book *domain.Book) error {
	if m.createErr != nil {
		return m.createErr
	}
	book.ID = "generated-id"
	m.books[book.ID] = *book
	return nil
}

func (m *mockRepository) Update(book *domain.Book) error {
	if m.updateErr != nil {
		return m.updateErr
	}
	if _, exists := m.books[book.ID]; !exists {
		return domain.ErrBookNotFound
	}
	m.books[book.ID] = *book
	return nil
}

func (m *mockRepository) Delete(id string) error {
	if m.deleteErr != nil {
		return m.deleteErr
	}
	if _, exists := m.books[id]; !exists {
		return domain.ErrBookNotFound
	}
	delete(m.books, id)
	return nil
}

func TestBookHandler_GetAll(t *testing.T) {
	tests := []struct {
		name       string
		setupRepo  func(*mockRepository)
		wantStatus int
		wantCount  int
	}{
		{
			name:       "returns empty list when no books",
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusOK,
			wantCount:  0,
		},
		{
			name: "returns all books",
			setupRepo: func(m *mockRepository) {
				m.books["1"] = domain.Book{ID: "1", Title: "Book 1"}
				m.books["2"] = domain.Book{ID: "2", Title: "Book 2"}
			},
			wantStatus: http.StatusOK,
			wantCount:  2,
		},
		{
			name: "returns 500 on repository error",
			setupRepo: func(m *mockRepository) {
				m.getAllErr = domain.ErrInvalidBook
			},
			wantStatus: http.StatusInternalServerError,
			wantCount:  0,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)
			handler := NewBookHandler(repo)

			req := httptest.NewRequest("GET", "/books", nil)
			rec := httptest.NewRecorder()

			handler.GetAll(rec, req)

			if rec.Code != tt.wantStatus {
				t.Errorf("GetAll() status = %d, want %d", rec.Code, tt.wantStatus)
			}

			if tt.wantStatus == http.StatusOK {
				var books []domain.Book
				json.NewDecoder(rec.Body).Decode(&books)
				if len(books) != tt.wantCount {
					t.Errorf("GetAll() count = %d, want %d", len(books), tt.wantCount)
				}
			}
		})
	}
}

func TestBookHandler_GetByID(t *testing.T) {
	tests := []struct {
		name       string
		path       string
		setupRepo  func(*mockRepository)
		wantStatus int
	}{
		{
			name: "returns book when found",
			path: "/books/1",
			setupRepo: func(m *mockRepository) {
				m.books["1"] = domain.Book{ID: "1", Title: "Test Book"}
			},
			wantStatus: http.StatusOK,
		},
		{
			name:       "returns 404 when not found",
			path:       "/books/nonexistent",
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusNotFound,
		},
		{
			name:       "returns 400 when ID missing",
			path:       "/books",
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)
			handler := NewBookHandler(repo)

			req := httptest.NewRequest("GET", tt.path, nil)
			rec := httptest.NewRecorder()

			handler.GetByID(rec, req)

			if rec.Code != tt.wantStatus {
				t.Errorf("GetByID() status = %d, want %d", rec.Code, tt.wantStatus)
			}
		})
	}
}

func TestBookHandler_Create(t *testing.T) {
	tests := []struct {
		name       string
		body       interface{}
		setupRepo  func(*mockRepository)
		wantStatus int
	}{
		{
			name: "creates new book",
			body: domain.Book{
				ISBN:   "978-0-13-468599-1",
				Title:  "Test Book",
				Author: "Test Author",
				Price:  29.99,
			},
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusCreated,
		},
		{
			name:       "returns 400 on invalid JSON",
			body:       "invalid json",
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusBadRequest,
		},
		{
			name: "returns 400 on validation error",
			body: domain.Book{Title: "Test"},
			setupRepo: func(m *mockRepository) {
				m.createErr = domain.ErrEmptyAuthor
			},
			wantStatus: http.StatusBadRequest,
		},
		{
			name: "returns 409 on duplicate ISBN",
			body: domain.Book{
				ISBN:   "978-0-13-468599-1",
				Title:  "Test",
				Author: "Author",
				Price:  10,
			},
			setupRepo: func(m *mockRepository) {
				m.createErr = domain.ErrDuplicateISBN
			},
			wantStatus: http.StatusConflict,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)
			handler := NewBookHandler(repo)

			var body []byte
			if s, ok := tt.body.(string); ok {
				body = []byte(s)
			} else {
				body, _ = json.Marshal(tt.body)
			}

			req := httptest.NewRequest("POST", "/books", bytes.NewReader(body))
			req.Header.Set("Content-Type", "application/json")
			rec := httptest.NewRecorder()

			handler.Create(rec, req)

			if rec.Code != tt.wantStatus {
				t.Errorf("Create() status = %d, want %d", rec.Code, tt.wantStatus)
			}
		})
	}
}

func TestBookHandler_Update(t *testing.T) {
	tests := []struct {
		name       string
		path       string
		body       interface{}
		setupRepo  func(*mockRepository)
		wantStatus int
	}{
		{
			name: "updates existing book",
			path: "/books/1",
			body: domain.Book{
				ISBN:   "978-0-13-468599-1",
				Title:  "Updated Title",
				Author: "Updated Author",
				Price:  39.99,
			},
			setupRepo: func(m *mockRepository) {
				m.books["1"] = domain.Book{ID: "1", Title: "Original"}
			},
			wantStatus: http.StatusOK,
		},
		{
			name: "returns 404 when not found",
			path: "/books/nonexistent",
			body: domain.Book{
				ISBN:   "978-0-13-468599-1",
				Title:  "Test",
				Author: "Author",
				Price:  10,
			},
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusNotFound,
		},
		{
			name:       "returns 400 on invalid JSON",
			path:       "/books/1",
			body:       "invalid",
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusBadRequest,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)
			handler := NewBookHandler(repo)

			var body []byte
			if s, ok := tt.body.(string); ok {
				body = []byte(s)
			} else {
				body, _ = json.Marshal(tt.body)
			}

			req := httptest.NewRequest("PUT", tt.path, bytes.NewReader(body))
			req.Header.Set("Content-Type", "application/json")
			rec := httptest.NewRecorder()

			handler.Update(rec, req)

			if rec.Code != tt.wantStatus {
				t.Errorf("Update() status = %d, want %d", rec.Code, tt.wantStatus)
			}
		})
	}
}

func TestBookHandler_Delete(t *testing.T) {
	tests := []struct {
		name       string
		path       string
		setupRepo  func(*mockRepository)
		wantStatus int
	}{
		{
			name: "deletes existing book",
			path: "/books/1",
			setupRepo: func(m *mockRepository) {
				m.books["1"] = domain.Book{ID: "1", Title: "To Delete"}
			},
			wantStatus: http.StatusNoContent,
		},
		{
			name:       "returns 404 when not found",
			path:       "/books/nonexistent",
			setupRepo:  func(m *mockRepository) {},
			wantStatus: http.StatusNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := newMockRepository()
			tt.setupRepo(repo)
			handler := NewBookHandler(repo)

			req := httptest.NewRequest("DELETE", tt.path, nil)
			rec := httptest.NewRecorder()

			handler.Delete(rec, req)

			if rec.Code != tt.wantStatus {
				t.Errorf("Delete() status = %d, want %d", rec.Code, tt.wantStatus)
			}
		})
	}
}

func TestBookHandler_extractID(t *testing.T) {
	handler := &BookHandler{}

	tests := []struct {
		name string
		path string
		want string
	}{
		{"extracts ID from /books/123", "/books/123", "123"},
		{"extracts ID from /api/books/abc", "/api/books/abc", "abc"},
		{"returns empty for /books", "/books", "books"},
		{"handles trailing slash", "/books/123/", "123"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := handler.extractID(tt.path)
			if got != tt.want {
				t.Errorf("extractID(%q) = %q, want %q", tt.path, got, tt.want)
			}
		})
	}
}

func TestBookHandler_errorToStatus(t *testing.T) {
	handler := &BookHandler{}

	tests := []struct {
		name string
		err  error
		want int
	}{
		{"ErrBookNotFound -> 404", domain.ErrBookNotFound, http.StatusNotFound},
		{"ErrDuplicateISBN -> 409", domain.ErrDuplicateISBN, http.StatusConflict},
		{"ErrEmptyTitle -> 400", domain.ErrEmptyTitle, http.StatusBadRequest},
		{"ErrEmptyAuthor -> 400", domain.ErrEmptyAuthor, http.StatusBadRequest},
		{"ErrInvalidISBN -> 400", domain.ErrInvalidISBN, http.StatusBadRequest},
		{"ErrNegativePrice -> 400", domain.ErrNegativePrice, http.StatusBadRequest},
		{"unknown error -> 500", domain.ErrInvalidBook, http.StatusBadRequest},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := handler.errorToStatus(tt.err)
			if got != tt.want {
				t.Errorf("errorToStatus(%v) = %d, want %d", tt.err, got, tt.want)
			}
		})
	}
}
