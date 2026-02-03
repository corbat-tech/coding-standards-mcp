package repository

import (
	"errors"
	"sync"
	"time"

	"github.com/example/bookstore/model"
)

var (
	ErrBookNotFound = errors.New("book not found")
	ErrISBNExists   = errors.New("ISBN already exists")
)

type BookRepository interface {
	Create(book *model.Book) error
	GetByID(id string) (*model.Book, error)
	GetAll() ([]*model.Book, error)
	Update(id string, book *model.Book) error
	Delete(id string) error
}

type InMemoryBookRepository struct {
	mu    sync.RWMutex
	books map[string]*model.Book
}

func NewInMemoryBookRepository() *InMemoryBookRepository {
	return &InMemoryBookRepository{
		books: make(map[string]*model.Book),
	}
}

func (r *InMemoryBookRepository) Create(book *model.Book) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	for _, b := range r.books {
		if b.ISBN == book.ISBN {
			return ErrISBNExists
		}
	}

	book.CreatedAt = time.Now()
	book.UpdatedAt = time.Now()
	r.books[book.ID] = book
	return nil
}

func (r *InMemoryBookRepository) GetByID(id string) (*model.Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	book, exists := r.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}
	return book, nil
}

func (r *InMemoryBookRepository) GetAll() ([]*model.Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	books := make([]*model.Book, 0, len(r.books))
	for _, book := range r.books {
		books = append(books, book)
	}
	return books, nil
}

func (r *InMemoryBookRepository) Update(id string, book *model.Book) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.books[id]; !exists {
		return ErrBookNotFound
	}

	book.UpdatedAt = time.Now()
	r.books[id] = book
	return nil
}

func (r *InMemoryBookRepository) Delete(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.books[id]; !exists {
		return ErrBookNotFound
	}

	delete(r.books, id)
	return nil
}
