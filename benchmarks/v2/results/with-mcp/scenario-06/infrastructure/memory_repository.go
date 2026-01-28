package infrastructure

import (
	"bookstore/domain"
	"sync"
)

type InMemoryBookRepository struct {
	mu    sync.RWMutex
	books map[string]*domain.Book
}

func NewInMemoryBookRepository() *InMemoryBookRepository {
	return &InMemoryBookRepository{
		books: make(map[string]*domain.Book),
	}
}

func (r *InMemoryBookRepository) FindByID(id string) (*domain.Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	book, exists := r.books[id]
	if !exists {
		return nil, nil
	}
	return book, nil
}

func (r *InMemoryBookRepository) FindAll() ([]*domain.Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	books := make([]*domain.Book, 0, len(r.books))
	for _, book := range r.books {
		books = append(books, book)
	}
	return books, nil
}

func (r *InMemoryBookRepository) Save(book *domain.Book) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	r.books[book.ID] = book
	return nil
}

func (r *InMemoryBookRepository) Delete(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	delete(r.books, id)
	return nil
}

func (r *InMemoryBookRepository) Clear() {
	r.mu.Lock()
	defer r.mu.Unlock()

	r.books = make(map[string]*domain.Book)
}
