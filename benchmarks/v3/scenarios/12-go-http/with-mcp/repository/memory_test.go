package repository

import (
	"testing"

	"bookstore/domain"
)

func TestInMemoryBookRepository_Create(t *testing.T) {
	tests := []struct {
		name    string
		book    domain.Book
		wantErr error
	}{
		{
			name: "creates valid book",
			book: domain.Book{
				ISBN:   "978-0-13-468599-1",
				Title:  "The Go Programming Language",
				Author: "Alan A. A. Donovan",
				Price:  39.99,
			},
			wantErr: nil,
		},
		{
			name: "fails on invalid book",
			book: domain.Book{
				ISBN:   "",
				Title:  "Invalid",
				Author: "Author",
				Price:  10.00,
			},
			wantErr: domain.ErrInvalidISBN,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := NewInMemoryBookRepository()
			err := repo.Create(&tt.book)
			if err != tt.wantErr {
				t.Errorf("Create() error = %v, wantErr %v", err, tt.wantErr)
			}
			if err == nil && tt.book.ID == "" {
				t.Error("Create() should set book ID")
			}
		})
	}
}

func TestInMemoryBookRepository_Create_DuplicateISBN(t *testing.T) {
	repo := NewInMemoryBookRepository()
	book1 := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "Book One",
		Author: "Author One",
		Price:  29.99,
	}
	book2 := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "Book Two",
		Author: "Author Two",
		Price:  39.99,
	}

	if err := repo.Create(&book1); err != nil {
		t.Fatalf("Create() first book error = %v", err)
	}

	if err := repo.Create(&book2); err != domain.ErrDuplicateISBN {
		t.Errorf("Create() duplicate ISBN error = %v, want %v", err, domain.ErrDuplicateISBN)
	}
}

func TestInMemoryBookRepository_GetAll(t *testing.T) {
	tests := []struct {
		name      string
		seedBooks []domain.Book
		wantCount int
	}{
		{
			name:      "returns empty list when no books",
			seedBooks: []domain.Book{},
			wantCount: 0,
		},
		{
			name: "returns all books",
			seedBooks: []domain.Book{
				{ISBN: "111", Title: "Book 1", Author: "Author 1", Price: 10},
				{ISBN: "222", Title: "Book 2", Author: "Author 2", Price: 20},
				{ISBN: "333", Title: "Book 3", Author: "Author 3", Price: 30},
			},
			wantCount: 3,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			repo := NewInMemoryBookRepository()
			for i := range tt.seedBooks {
				_ = repo.Create(&tt.seedBooks[i])
			}

			books, err := repo.GetAll()
			if err != nil {
				t.Errorf("GetAll() error = %v", err)
			}
			if len(books) != tt.wantCount {
				t.Errorf("GetAll() count = %d, want %d", len(books), tt.wantCount)
			}
		})
	}
}

func TestInMemoryBookRepository_GetByID(t *testing.T) {
	repo := NewInMemoryBookRepository()
	book := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "The Go Programming Language",
		Author: "Alan A. A. Donovan",
		Price:  39.99,
	}
	_ = repo.Create(&book)

	tests := []struct {
		name    string
		id      string
		wantErr error
	}{
		{
			name:    "returns book when found",
			id:      book.ID,
			wantErr: nil,
		},
		{
			name:    "returns error when not found",
			id:      "nonexistent",
			wantErr: domain.ErrBookNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result, err := repo.GetByID(tt.id)
			if err != tt.wantErr {
				t.Errorf("GetByID() error = %v, wantErr %v", err, tt.wantErr)
			}
			if err == nil && result.ID != tt.id {
				t.Errorf("GetByID() returned wrong book")
			}
		})
	}
}

func TestInMemoryBookRepository_Update(t *testing.T) {
	repo := NewInMemoryBookRepository()
	book := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "Original Title",
		Author: "Original Author",
		Price:  29.99,
	}
	_ = repo.Create(&book)

	tests := []struct {
		name       string
		updateBook domain.Book
		wantErr    error
	}{
		{
			name: "updates existing book",
			updateBook: domain.Book{
				ID:     book.ID,
				ISBN:   "978-0-13-468599-1",
				Title:  "Updated Title",
				Author: "Updated Author",
				Price:  49.99,
			},
			wantErr: nil,
		},
		{
			name: "returns error for nonexistent book",
			updateBook: domain.Book{
				ID:     "nonexistent",
				ISBN:   "978-0-13-468599-2",
				Title:  "Some Title",
				Author: "Some Author",
				Price:  19.99,
			},
			wantErr: domain.ErrBookNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := repo.Update(&tt.updateBook)
			if err != tt.wantErr {
				t.Errorf("Update() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestInMemoryBookRepository_Delete(t *testing.T) {
	repo := NewInMemoryBookRepository()
	book := domain.Book{
		ISBN:   "978-0-13-468599-1",
		Title:  "To Delete",
		Author: "Author",
		Price:  29.99,
	}
	_ = repo.Create(&book)

	tests := []struct {
		name    string
		id      string
		wantErr error
	}{
		{
			name:    "deletes existing book",
			id:      book.ID,
			wantErr: nil,
		},
		{
			name:    "returns error for nonexistent book",
			id:      "nonexistent",
			wantErr: domain.ErrBookNotFound,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := repo.Delete(tt.id)
			if err != tt.wantErr {
				t.Errorf("Delete() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
