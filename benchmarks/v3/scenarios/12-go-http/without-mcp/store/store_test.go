package store

import (
	"errors"
	"sync"
	"testing"

	"bookstore/models"
)

func TestBookStore_Create(t *testing.T) {
	tests := []struct {
		name        string
		request     *models.CreateBookRequest
		expectError error
	}{
		{
			name: "valid book",
			request: &models.CreateBookRequest{
				Title:    "The Go Programming Language",
				Author:   "Alan Donovan",
				ISBN:     "978-0134190440",
				Price:    44.99,
				Quantity: 10,
			},
			expectError: nil,
		},
		{
			name: "empty title",
			request: &models.CreateBookRequest{
				Title:    "",
				Author:   "Author",
				ISBN:     "123",
				Price:    10.00,
				Quantity: 1,
			},
			expectError: models.ErrEmptyTitle,
		},
		{
			name: "empty author",
			request: &models.CreateBookRequest{
				Title:    "Title",
				Author:   "",
				ISBN:     "123",
				Price:    10.00,
				Quantity: 1,
			},
			expectError: models.ErrEmptyAuthor,
		},
		{
			name: "empty ISBN",
			request: &models.CreateBookRequest{
				Title:    "Title",
				Author:   "Author",
				ISBN:     "",
				Price:    10.00,
				Quantity: 1,
			},
			expectError: models.ErrEmptyISBN,
		},
		{
			name: "zero price",
			request: &models.CreateBookRequest{
				Title:    "Title",
				Author:   "Author",
				ISBN:     "123",
				Price:    0,
				Quantity: 1,
			},
			expectError: models.ErrInvalidPrice,
		},
		{
			name: "negative quantity",
			request: &models.CreateBookRequest{
				Title:    "Title",
				Author:   "Author",
				ISBN:     "123",
				Price:    10.00,
				Quantity: -1,
			},
			expectError: models.ErrInvalidQty,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			store := NewBookStore()
			book, err := store.Create(tt.request)

			if tt.expectError != nil {
				if !errors.Is(err, tt.expectError) {
					t.Errorf("expected error %v, got %v", tt.expectError, err)
				}
				return
			}

			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}

			if book.ID == "" {
				t.Error("book ID should not be empty")
			}
			if book.Title != tt.request.Title {
				t.Errorf("expected title %q, got %q", tt.request.Title, book.Title)
			}
			if book.CreatedAt.IsZero() {
				t.Error("CreatedAt should be set")
			}
			if book.UpdatedAt.IsZero() {
				t.Error("UpdatedAt should be set")
			}
		})
	}
}

func TestBookStore_DuplicateISBN(t *testing.T) {
	store := NewBookStore()

	req := &models.CreateBookRequest{
		Title:    "Book 1",
		Author:   "Author",
		ISBN:     "duplicate-isbn",
		Price:    10.00,
		Quantity: 1,
	}

	_, err := store.Create(req)
	if err != nil {
		t.Fatalf("first creation should succeed: %v", err)
	}

	req2 := &models.CreateBookRequest{
		Title:    "Book 2",
		Author:   "Author 2",
		ISBN:     "duplicate-isbn",
		Price:    20.00,
		Quantity: 2,
	}

	_, err = store.Create(req2)
	if !errors.Is(err, ErrDuplicateISBN) {
		t.Errorf("expected ErrDuplicateISBN, got %v", err)
	}
}

func TestBookStore_GetByID(t *testing.T) {
	store := NewBookStore()

	// Create a book
	req := &models.CreateBookRequest{
		Title:    "Test Book",
		Author:   "Test Author",
		ISBN:     "test-isbn",
		Price:    15.99,
		Quantity: 5,
	}
	created, _ := store.Create(req)

	tests := []struct {
		name        string
		id          string
		expectError error
	}{
		{
			name:        "existing book",
			id:          created.ID,
			expectError: nil,
		},
		{
			name:        "non-existing book",
			id:          "non-existing-id",
			expectError: ErrBookNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			book, err := store.GetByID(tt.id)

			if tt.expectError != nil {
				if !errors.Is(err, tt.expectError) {
					t.Errorf("expected error %v, got %v", tt.expectError, err)
				}
				return
			}

			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			if book.ID != tt.id {
				t.Errorf("expected book ID %q, got %q", tt.id, book.ID)
			}
		})
	}
}

func TestBookStore_GetAll(t *testing.T) {
	store := NewBookStore()

	// Empty store
	books := store.GetAll()
	if len(books) != 0 {
		t.Errorf("expected 0 books, got %d", len(books))
	}

	// Add books
	for i := 1; i <= 3; i++ {
		store.Create(&models.CreateBookRequest{
			Title:    "Book",
			Author:   "Author",
			ISBN:     string(rune('0' + i)),
			Price:    10.00,
			Quantity: 1,
		})
	}

	books = store.GetAll()
	if len(books) != 3 {
		t.Errorf("expected 3 books, got %d", len(books))
	}
}

func TestBookStore_Update(t *testing.T) {
	store := NewBookStore()

	// Create initial book
	created, _ := store.Create(&models.CreateBookRequest{
		Title:    "Original Title",
		Author:   "Original Author",
		ISBN:     "original-isbn",
		Price:    10.00,
		Quantity: 1,
	})

	newTitle := "Updated Title"
	newPrice := 25.99

	tests := []struct {
		name        string
		id          string
		request     *models.UpdateBookRequest
		expectError error
		validate    func(*models.Book)
	}{
		{
			name:    "update title",
			id:      created.ID,
			request: &models.UpdateBookRequest{Title: &newTitle},
			validate: func(b *models.Book) {
				if b.Title != newTitle {
					t.Errorf("expected title %q, got %q", newTitle, b.Title)
				}
			},
		},
		{
			name:    "update price",
			id:      created.ID,
			request: &models.UpdateBookRequest{Price: &newPrice},
			validate: func(b *models.Book) {
				if b.Price != newPrice {
					t.Errorf("expected price %f, got %f", newPrice, b.Price)
				}
			},
		},
		{
			name:        "non-existing book",
			id:          "non-existing-id",
			request:     &models.UpdateBookRequest{Title: &newTitle},
			expectError: ErrBookNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			book, err := store.Update(tt.id, tt.request)

			if tt.expectError != nil {
				if !errors.Is(err, tt.expectError) {
					t.Errorf("expected error %v, got %v", tt.expectError, err)
				}
				return
			}

			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}

			if tt.validate != nil {
				tt.validate(book)
			}
		})
	}
}

func TestBookStore_Delete(t *testing.T) {
	store := NewBookStore()

	// Create a book
	created, _ := store.Create(&models.CreateBookRequest{
		Title:    "Book to Delete",
		Author:   "Author",
		ISBN:     "delete-isbn",
		Price:    10.00,
		Quantity: 1,
	})

	tests := []struct {
		name        string
		id          string
		expectError error
	}{
		{
			name:        "delete existing book",
			id:          created.ID,
			expectError: nil,
		},
		{
			name:        "delete non-existing book",
			id:          "non-existing-id",
			expectError: ErrBookNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := store.Delete(tt.id)

			if tt.expectError != nil {
				if !errors.Is(err, tt.expectError) {
					t.Errorf("expected error %v, got %v", tt.expectError, err)
				}
				return
			}

			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}

			// Verify book is deleted
			_, err = store.GetByID(tt.id)
			if !errors.Is(err, ErrBookNotFound) {
				t.Error("book should be deleted")
			}
		})
	}
}

func TestBookStore_Concurrency(t *testing.T) {
	store := NewBookStore()
	var wg sync.WaitGroup

	// Concurrent creates
	for i := 0; i < 100; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			store.Create(&models.CreateBookRequest{
				Title:    "Book",
				Author:   "Author",
				ISBN:     string(rune(i)),
				Price:    10.00,
				Quantity: 1,
			})
		}(i)
	}

	wg.Wait()

	// Concurrent reads
	books := store.GetAll()
	for _, book := range books {
		wg.Add(1)
		go func(id string) {
			defer wg.Done()
			store.GetByID(id)
		}(book.ID)
	}

	wg.Wait()
}
