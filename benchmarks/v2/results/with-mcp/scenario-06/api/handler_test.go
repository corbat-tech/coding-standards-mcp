package api

import (
	"bookstore/application"
	"bookstore/infrastructure"
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

type stubIDGenerator struct {
	counter int
}

func (g *stubIDGenerator) Generate() string {
	g.counter++
	return "book-" + string(rune('0'+g.counter))
}

type stubClock struct {
	fixedTime time.Time
}

func (c *stubClock) Now() time.Time {
	return c.fixedTime
}

func setupHandler() (*BookHandler, *infrastructure.InMemoryBookRepository) {
	repo := infrastructure.NewInMemoryBookRepository()
	idGen := &stubIDGenerator{}
	clock := &stubClock{fixedTime: time.Date(2024, 1, 15, 10, 0, 0, 0, time.UTC)}
	service := application.NewBookService(repo, idGen, clock)
	handler := NewBookHandler(service)
	return handler, repo
}

func TestCreateBook(t *testing.T) {
	t.Run("should_create_book_when_valid_input", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		body := `{"title":"Test Book","author":"Test Author","isbn":"123"}`
		req := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(body))
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusCreated {
			t.Errorf("expected status %d, got %d", http.StatusCreated, rec.Code)
		}

		var resp BookResponse
		json.NewDecoder(rec.Body).Decode(&resp)
		if resp.Title != "Test Book" {
			t.Errorf("expected title 'Test Book', got '%s'", resp.Title)
		}
	})

	t.Run("should_return_400_when_title_empty", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		body := `{"title":"","author":"Test Author","isbn":"123"}`
		req := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(body))
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusBadRequest {
			t.Errorf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
		}
	})
}

func TestGetBook(t *testing.T) {
	t.Run("should_return_book_when_exists", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		createBody := `{"title":"Test Book","author":"Test Author","isbn":"123"}`
		createReq := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(createBody))
		createRec := httptest.NewRecorder()
		handler.ServeHTTP(createRec, createReq)

		var created BookResponse
		json.NewDecoder(createRec.Body).Decode(&created)

		req := httptest.NewRequest(http.MethodGet, "/books/"+created.ID, nil)
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}
	})

	t.Run("should_return_404_when_not_found", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		req := httptest.NewRequest(http.MethodGet, "/books/non-existent", nil)
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusNotFound {
			t.Errorf("expected status %d, got %d", http.StatusNotFound, rec.Code)
		}
	})
}

func TestListBooks(t *testing.T) {
	t.Run("should_return_empty_list_when_no_books", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		req := httptest.NewRequest(http.MethodGet, "/books", nil)
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var books []BookResponse
		json.NewDecoder(rec.Body).Decode(&books)
		if len(books) != 0 {
			t.Errorf("expected 0 books, got %d", len(books))
		}
	})
}

func TestBorrowBook(t *testing.T) {
	t.Run("should_borrow_book_when_available", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		createBody := `{"title":"Test Book","author":"Test Author","isbn":"123"}`
		createReq := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(createBody))
		createRec := httptest.NewRecorder()
		handler.ServeHTTP(createRec, createReq)

		var created BookResponse
		json.NewDecoder(createRec.Body).Decode(&created)

		borrowBody := `{"borrower_id":"user-123"}`
		req := httptest.NewRequest(http.MethodPost, "/books/"+created.ID+"/borrow", bytes.NewBufferString(borrowBody))
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var resp BookResponse
		json.NewDecoder(rec.Body).Decode(&resp)
		if resp.Status != "borrowed" {
			t.Errorf("expected status 'borrowed', got '%s'", resp.Status)
		}
	})

	t.Run("should_return_conflict_when_already_borrowed", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		createBody := `{"title":"Test Book","author":"Test Author","isbn":"123"}`
		createReq := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(createBody))
		createRec := httptest.NewRecorder()
		handler.ServeHTTP(createRec, createReq)

		var created BookResponse
		json.NewDecoder(createRec.Body).Decode(&created)

		borrowBody := `{"borrower_id":"user-123"}`
		firstBorrow := httptest.NewRequest(http.MethodPost, "/books/"+created.ID+"/borrow", bytes.NewBufferString(borrowBody))
		handler.ServeHTTP(httptest.NewRecorder(), firstBorrow)

		req := httptest.NewRequest(http.MethodPost, "/books/"+created.ID+"/borrow", bytes.NewBufferString(borrowBody))
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusConflict {
			t.Errorf("expected status %d, got %d", http.StatusConflict, rec.Code)
		}
	})
}

func TestReturnBook(t *testing.T) {
	t.Run("should_return_book_when_borrowed", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		createBody := `{"title":"Test Book","author":"Test Author","isbn":"123"}`
		createReq := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(createBody))
		createRec := httptest.NewRecorder()
		handler.ServeHTTP(createRec, createReq)

		var created BookResponse
		json.NewDecoder(createRec.Body).Decode(&created)

		borrowBody := `{"borrower_id":"user-123"}`
		borrowReq := httptest.NewRequest(http.MethodPost, "/books/"+created.ID+"/borrow", bytes.NewBufferString(borrowBody))
		handler.ServeHTTP(httptest.NewRecorder(), borrowReq)

		req := httptest.NewRequest(http.MethodPost, "/books/"+created.ID+"/return", nil)
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusOK {
			t.Errorf("expected status %d, got %d", http.StatusOK, rec.Code)
		}

		var resp BookResponse
		json.NewDecoder(rec.Body).Decode(&resp)
		if resp.Status != "available" {
			t.Errorf("expected status 'available', got '%s'", resp.Status)
		}
	})

	t.Run("should_return_conflict_when_not_borrowed", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		createBody := `{"title":"Test Book","author":"Test Author","isbn":"123"}`
		createReq := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(createBody))
		createRec := httptest.NewRecorder()
		handler.ServeHTTP(createRec, createReq)

		var created BookResponse
		json.NewDecoder(createRec.Body).Decode(&created)

		req := httptest.NewRequest(http.MethodPost, "/books/"+created.ID+"/return", nil)
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusConflict {
			t.Errorf("expected status %d, got %d", http.StatusConflict, rec.Code)
		}
	})
}

func TestDeleteBook(t *testing.T) {
	t.Run("should_delete_book_when_exists", func(t *testing.T) {
		// Arrange
		handler, _ := setupHandler()
		createBody := `{"title":"Test Book","author":"Test Author","isbn":"123"}`
		createReq := httptest.NewRequest(http.MethodPost, "/books", bytes.NewBufferString(createBody))
		createRec := httptest.NewRecorder()
		handler.ServeHTTP(createRec, createReq)

		var created BookResponse
		json.NewDecoder(createRec.Body).Decode(&created)

		req := httptest.NewRequest(http.MethodDelete, "/books/"+created.ID, nil)
		rec := httptest.NewRecorder()

		// Act
		handler.ServeHTTP(rec, req)

		// Assert
		if rec.Code != http.StatusNoContent {
			t.Errorf("expected status %d, got %d", http.StatusNoContent, rec.Code)
		}
	})
}
