// Package repository provides implementations of domain repository interfaces.
package repository

import (
	"sync"
	"time"

	"bookstore/domain"

	"github.com/google/uuid"
)

// InMemoryBookRepository is an in-memory implementation of BookRepository.
type InMemoryBookRepository struct {
	mu    sync.RWMutex
	books map[string]domain.Book
}

// NewInMemoryBookRepository creates a new in-memory book repository.
func NewInMemoryBookRepository() *InMemoryBookRepository {
	return &InMemoryBookRepository{
		books: make(map[string]domain.Book),
	}
}

// GetAll returns all books in the repository.
func (r *InMemoryBookRepository) GetAll() ([]domain.Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	books := make([]domain.Book, 0, len(r.books))
	for _, book := range r.books {
		books = append(books, book)
	}
	return books, nil
}

// GetByID returns a book by its ID.
func (r *InMemoryBookRepository) GetByID(id string) (*domain.Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	book, exists := r.books[id]
	if !exists {
		return nil, domain.ErrBookNotFound
	}
	return &book, nil
}

// Create adds a new book to the repository.
func (r *InMemoryBookRepository) Create(book *domain.Book) error {
	if err := book.Validate(); err != nil {
		return err
	}

	r.mu.Lock()
	defer r.mu.Unlock()

	// Check for duplicate ISBN
	for _, b := range r.books {
		if b.ISBN == book.ISBN {
			return domain.ErrDuplicateISBN
		}
	}

	book.ID = uuid.New().String()
	book.CreatedAt = time.Now()
	book.UpdatedAt = book.CreatedAt
	r.books[book.ID] = *book
	return nil
}

// Update modifies an existing book in the repository.
func (r *InMemoryBookRepository) Update(book *domain.Book) error {
	if err := book.Validate(); err != nil {
		return err
	}

	r.mu.Lock()
	defer r.mu.Unlock()

	existing, exists := r.books[book.ID]
	if !exists {
		return domain.ErrBookNotFound
	}

	// Check for duplicate ISBN (excluding current book)
	for _, b := range r.books {
		if b.ISBN == book.ISBN && b.ID != book.ID {
			return domain.ErrDuplicateISBN
		}
	}

	book.CreatedAt = existing.CreatedAt
	book.UpdatedAt = time.Now()
	r.books[book.ID] = *book
	return nil
}

// Delete removes a book from the repository.
func (r *InMemoryBookRepository) Delete(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.books[id]; !exists {
		return domain.ErrBookNotFound
	}
	delete(r.books, id)
	return nil
}

// Ensure InMemoryBookRepository implements BookRepository
var _ domain.BookRepository = (*InMemoryBookRepository)(nil)
