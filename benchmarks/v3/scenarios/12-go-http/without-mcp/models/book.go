package models

import (
	"errors"
	"time"
)

// Book represents a book in the bookstore
type Book struct {
	ID        string    `json:"id"`
	Title     string    `json:"title"`
	Author    string    `json:"author"`
	ISBN      string    `json:"isbn"`
	Price     float64   `json:"price"`
	Quantity  int       `json:"quantity"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// Validation errors
var (
	ErrEmptyTitle    = errors.New("title cannot be empty")
	ErrEmptyAuthor   = errors.New("author cannot be empty")
	ErrEmptyISBN     = errors.New("ISBN cannot be empty")
	ErrInvalidPrice  = errors.New("price must be greater than zero")
	ErrInvalidQty    = errors.New("quantity cannot be negative")
)

// Validate validates the book fields
func (b *Book) Validate() error {
	if b.Title == "" {
		return ErrEmptyTitle
	}
	if b.Author == "" {
		return ErrEmptyAuthor
	}
	if b.ISBN == "" {
		return ErrEmptyISBN
	}
	if b.Price <= 0 {
		return ErrInvalidPrice
	}
	if b.Quantity < 0 {
		return ErrInvalidQty
	}
	return nil
}

// CreateBookRequest represents the request body for creating a book
type CreateBookRequest struct {
	Title    string  `json:"title"`
	Author   string  `json:"author"`
	ISBN     string  `json:"isbn"`
	Price    float64 `json:"price"`
	Quantity int     `json:"quantity"`
}

// UpdateBookRequest represents the request body for updating a book
type UpdateBookRequest struct {
	Title    *string  `json:"title,omitempty"`
	Author   *string  `json:"author,omitempty"`
	ISBN     *string  `json:"isbn,omitempty"`
	Price    *float64 `json:"price,omitempty"`
	Quantity *int     `json:"quantity,omitempty"`
}
