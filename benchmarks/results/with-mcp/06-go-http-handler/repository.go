package main

import (
	"sync"
	"time"

	"github.com/google/uuid"
)

type BookRepository interface {
	Create(book *Book) (*Book, error)
	GetByID(id string) (*Book, error)
	GetAll() ([]*Book, error)
	Update(book *Book) (*Book, error)
	Delete(id string) error
	FindByAuthor(author string) ([]*Book, error)
	FindByGenre(genre string) ([]*Book, error)
}

type InMemoryBookRepository struct {
	mu    sync.RWMutex
	books map[string]*Book
}

func NewInMemoryBookRepository() *InMemoryBookRepository {
	return &InMemoryBookRepository{
		books: make(map[string]*Book),
	}
}

func (r *InMemoryBookRepository) Create(book *Book) (*Book, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	book.ID = uuid.New().String()
	book.Available = true
	book.CreatedAt = time.Now()
	book.UpdatedAt = time.Now()

	r.books[book.ID] = book
	return book, nil
}

func (r *InMemoryBookRepository) GetByID(id string) (*Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	book, exists := r.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}
	return book, nil
}

func (r *InMemoryBookRepository) GetAll() ([]*Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	books := make([]*Book, 0, len(r.books))
	for _, book := range r.books {
		books = append(books, book)
	}
	return books, nil
}

func (r *InMemoryBookRepository) Update(book *Book) (*Book, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.books[book.ID]; !exists {
		return nil, ErrBookNotFound
	}

	book.UpdatedAt = time.Now()
	r.books[book.ID] = book
	return book, nil
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

func (r *InMemoryBookRepository) FindByAuthor(author string) ([]*Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	var books []*Book
	for _, book := range r.books {
		if book.Author == author {
			books = append(books, book)
		}
	}
	return books, nil
}

func (r *InMemoryBookRepository) FindByGenre(genre string) ([]*Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	var books []*Book
	for _, book := range r.books {
		if book.Genre == genre {
			books = append(books, book)
		}
	}
	return books, nil
}
