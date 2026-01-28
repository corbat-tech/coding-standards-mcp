package main

import (
	"strings"
	"sync"
	"time"

	"github.com/google/uuid"
)

type BookRepository struct {
	mu    sync.RWMutex
	books map[string]*Book
}

func NewBookRepository() *BookRepository {
	return &BookRepository{
		books: make(map[string]*Book),
	}
}

func (r *BookRepository) Create(req *CreateBookRequest) (*Book, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	book := &Book{
		ID:            uuid.New().String(),
		Title:         req.Title,
		Author:        req.Author,
		ISBN:          req.ISBN,
		PublishedYear: req.PublishedYear,
		Genre:         req.Genre,
		Available:     true,
		CreatedAt:     time.Now(),
		UpdatedAt:     time.Now(),
	}

	r.books[book.ID] = book
	return book, nil
}

func (r *BookRepository) GetByID(id string) (*Book, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()

	book, exists := r.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}
	return book, nil
}

func (r *BookRepository) GetAll() []*Book {
	r.mu.RLock()
	defer r.mu.RUnlock()

	books := make([]*Book, 0, len(r.books))
	for _, book := range r.books {
		books = append(books, book)
	}
	return books
}

func (r *BookRepository) GetByAuthor(author string) []*Book {
	r.mu.RLock()
	defer r.mu.RUnlock()

	var result []*Book
	authorLower := strings.ToLower(author)
	for _, book := range r.books {
		if strings.Contains(strings.ToLower(book.Author), authorLower) {
			result = append(result, book)
		}
	}
	return result
}

func (r *BookRepository) GetByGenre(genre string) []*Book {
	r.mu.RLock()
	defer r.mu.RUnlock()

	var result []*Book
	genreLower := strings.ToLower(genre)
	for _, book := range r.books {
		if strings.EqualFold(book.Genre, genreLower) {
			result = append(result, book)
		}
	}
	return result
}

func (r *BookRepository) Update(id string, req *UpdateBookRequest) (*Book, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	book, exists := r.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}

	if req.Title != nil {
		book.Title = *req.Title
	}
	if req.Author != nil {
		book.Author = *req.Author
	}
	if req.ISBN != nil {
		book.ISBN = *req.ISBN
	}
	if req.PublishedYear != nil {
		book.PublishedYear = *req.PublishedYear
	}
	if req.Genre != nil {
		book.Genre = *req.Genre
	}
	book.UpdatedAt = time.Now()

	return book, nil
}

func (r *BookRepository) Delete(id string) error {
	r.mu.Lock()
	defer r.mu.Unlock()

	if _, exists := r.books[id]; !exists {
		return ErrBookNotFound
	}

	delete(r.books, id)
	return nil
}

func (r *BookRepository) Borrow(id string) (*Book, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	book, exists := r.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}

	if !book.Available {
		return nil, ErrBookUnavailable
	}

	book.Available = false
	book.UpdatedAt = time.Now()
	return book, nil
}

func (r *BookRepository) Return(id string) (*Book, error) {
	r.mu.Lock()
	defer r.mu.Unlock()

	book, exists := r.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}

	if book.Available {
		return nil, ErrBookNotBorrowed
	}

	book.Available = true
	book.UpdatedAt = time.Now()
	return book, nil
}
