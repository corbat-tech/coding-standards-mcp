// Package domain contains the core business entities and interfaces.
package domain

import (
	"errors"
	"time"
)

// Custom errors for the domain layer.
var (
	ErrBookNotFound     = errors.New("book not found")
	ErrInvalidBook      = errors.New("invalid book data")
	ErrDuplicateISBN    = errors.New("book with this ISBN already exists")
	ErrEmptyTitle       = errors.New("book title cannot be empty")
	ErrEmptyAuthor      = errors.New("book author cannot be empty")
	ErrInvalidISBN      = errors.New("invalid ISBN format")
	ErrNegativePrice    = errors.New("book price cannot be negative")
)

// Book represents a book entity in the bookstore.
type Book struct {
	ID        string    `json:"id"`
	ISBN      string    `json:"isbn"`
	Title     string    `json:"title"`
	Author    string    `json:"author"`
	Price     float64   `json:"price"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// Validate checks if the book data is valid.
func (b *Book) Validate() error {
	if b.Title == "" {
		return ErrEmptyTitle
	}
	if b.Author == "" {
		return ErrEmptyAuthor
	}
	if b.ISBN == "" {
		return ErrInvalidISBN
	}
	if b.Price < 0 {
		return ErrNegativePrice
	}
	return nil
}

// BookRepository defines the interface for book persistence operations.
type BookRepository interface {
	GetAll() ([]Book, error)
	GetByID(id string) (*Book, error)
	Create(book *Book) error
	Update(book *Book) error
	Delete(id string) error
}
