package main

import (
	"errors"
	"sync"
	"time"

	"github.com/google/uuid"
)

var (
	ErrBookNotFound     = errors.New("book not found")
	ErrBookNotAvailable = errors.New("book is not available")
	ErrBookNotBorrowed  = errors.New("book is not borrowed")
)

type BookStore struct {
	books map[string]*Book
	mu    sync.RWMutex
}

func NewBookStore() *BookStore {
	return &BookStore{
		books: make(map[string]*Book),
	}
}

func (s *BookStore) Create(req CreateBookRequest) (*Book, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	book := &Book{
		ID:        uuid.New().String(),
		Title:     req.Title,
		Author:    req.Author,
		ISBN:      req.ISBN,
		Available: true,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	s.books[book.ID] = book
	return book, nil
}

func (s *BookStore) GetAll() []*Book {
	s.mu.RLock()
	defer s.mu.RUnlock()

	books := make([]*Book, 0, len(s.books))
	for _, book := range s.books {
		books = append(books, book)
	}
	return books
}

func (s *BookStore) GetByID(id string) (*Book, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	book, exists := s.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}
	return book, nil
}

func (s *BookStore) Update(id string, req UpdateBookRequest) (*Book, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	book, exists := s.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}

	if req.Title != "" {
		book.Title = req.Title
	}
	if req.Author != "" {
		book.Author = req.Author
	}
	book.UpdatedAt = time.Now()

	return book, nil
}

func (s *BookStore) Delete(id string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, exists := s.books[id]; !exists {
		return ErrBookNotFound
	}

	delete(s.books, id)
	return nil
}

func (s *BookStore) Borrow(id string, userID string) (*Book, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	book, exists := s.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}

	if !book.Available {
		return nil, ErrBookNotAvailable
	}

	book.Available = false
	book.BorrowedBy = userID
	book.BorrowedAt = time.Now()
	book.UpdatedAt = time.Now()

	return book, nil
}

func (s *BookStore) Return(id string) (*Book, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	book, exists := s.books[id]
	if !exists {
		return nil, ErrBookNotFound
	}

	if book.Available {
		return nil, ErrBookNotBorrowed
	}

	book.Available = true
	book.BorrowedBy = ""
	book.BorrowedAt = time.Time{}
	book.UpdatedAt = time.Now()

	return book, nil
}
