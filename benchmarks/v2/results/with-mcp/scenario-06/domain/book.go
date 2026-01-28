package domain

import (
	"errors"
	"time"
)

var (
	ErrBookNotFound      = errors.New("book not found")
	ErrBookNotAvailable  = errors.New("book not available for borrowing")
	ErrBookNotBorrowed   = errors.New("book is not currently borrowed")
	ErrInvalidBookInput  = errors.New("invalid book input")
)

type BookStatus string

const (
	BookStatusAvailable BookStatus = "available"
	BookStatusBorrowed  BookStatus = "borrowed"
)

type Book struct {
	ID          string
	Title       string
	Author      string
	ISBN        string
	Status      BookStatus
	BorrowedBy  string
	BorrowedAt  *time.Time
	CreatedAt   time.Time
	UpdatedAt   time.Time
}

func NewBook(id, title, author, isbn string, createdAt time.Time) (*Book, error) {
	if title == "" {
		return nil, ErrInvalidBookInput
	}
	if author == "" {
		return nil, ErrInvalidBookInput
	}

	return &Book{
		ID:        id,
		Title:     title,
		Author:    author,
		ISBN:      isbn,
		Status:    BookStatusAvailable,
		CreatedAt: createdAt,
		UpdatedAt: createdAt,
	}, nil
}

func (b *Book) Borrow(borrowerID string, borrowedAt time.Time) error {
	if b.Status == BookStatusBorrowed {
		return ErrBookNotAvailable
	}

	b.Status = BookStatusBorrowed
	b.BorrowedBy = borrowerID
	b.BorrowedAt = &borrowedAt
	b.UpdatedAt = borrowedAt
	return nil
}

func (b *Book) Return(returnedAt time.Time) error {
	if b.Status != BookStatusBorrowed {
		return ErrBookNotBorrowed
	}

	b.Status = BookStatusAvailable
	b.BorrowedBy = ""
	b.BorrowedAt = nil
	b.UpdatedAt = returnedAt
	return nil
}

func (b *Book) Update(title, author, isbn string, updatedAt time.Time) error {
	if title == "" {
		return ErrInvalidBookInput
	}
	if author == "" {
		return ErrInvalidBookInput
	}

	b.Title = title
	b.Author = author
	b.ISBN = isbn
	b.UpdatedAt = updatedAt
	return nil
}
