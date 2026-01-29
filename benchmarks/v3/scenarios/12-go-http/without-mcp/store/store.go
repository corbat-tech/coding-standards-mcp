package store

import (
	"errors"
	"sync"
	"time"

	"bookstore/models"

	"github.com/google/uuid"
)

// Errors
var (
	ErrBookNotFound = errors.New("book not found")
	ErrDuplicateISBN = errors.New("book with this ISBN already exists")
)

// BookStore provides in-memory storage for books
type BookStore struct {
	mu    sync.RWMutex
	books map[string]*models.Book
}

// NewBookStore creates a new BookStore instance
func NewBookStore() *BookStore {
	return &BookStore{
		books: make(map[string]*models.Book),
	}
}

// Create adds a new book to the store
func (s *BookStore) Create(req *models.CreateBookRequest) (*models.Book, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	// Check for duplicate ISBN
	for _, book := range s.books {
		if book.ISBN == req.ISBN {
			return nil, ErrDuplicateISBN
		}
	}

	now := time.Now()
	book := &models.Book{
		ID:        uuid.New().String(),
		Title:     req.Title,
		Author:    req.Author,
		ISBN:      req.ISBN,
		Price:     req.Price,
		Quantity:  req.Quantity,
		CreatedAt: now,
		UpdatedAt: now,
	}

	if err := book.Validate(); err != nil {
		return nil, err
	}

	s.books[book.ID] = book
	return book, nil
}

// GetByID retrieves a book by its ID
func (s *BookStore) GetByID(id string) (*models.Book, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	book, exists := s.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}
	return book, nil
}

// GetAll retrieves all books from the store
func (s *BookStore) GetAll() []*models.Book {
	s.mu.RLock()
	defer s.mu.RUnlock()

	books := make([]*models.Book, 0, len(s.books))
	for _, book := range s.books {
		books = append(books, book)
	}
	return books
}

// Update modifies an existing book
func (s *BookStore) Update(id string, req *models.UpdateBookRequest) (*models.Book, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	book, exists := s.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}

	// Check for duplicate ISBN if being updated
	if req.ISBN != nil && *req.ISBN != book.ISBN {
		for _, b := range s.books {
			if b.ISBN == *req.ISBN && b.ID != id {
				return nil, ErrDuplicateISBN
			}
		}
	}

	// Apply updates
	if req.Title != nil {
		book.Title = *req.Title
	}
	if req.Author != nil {
		book.Author = *req.Author
	}
	if req.ISBN != nil {
		book.ISBN = *req.ISBN
	}
	if req.Price != nil {
		book.Price = *req.Price
	}
	if req.Quantity != nil {
		book.Quantity = *req.Quantity
	}

	if err := book.Validate(); err != nil {
		return nil, err
	}

	book.UpdatedAt = time.Now()
	return book, nil
}

// Delete removes a book from the store
func (s *BookStore) Delete(id string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, exists := s.books[id]; !exists {
		return ErrBookNotFound
	}

	delete(s.books, id)
	return nil
}
