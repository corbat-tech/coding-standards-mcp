package main

import (
	"errors"
	"regexp"
	"time"
)

type Book struct {
	ID            string    `json:"id"`
	Title         string    `json:"title"`
	Author        string    `json:"author"`
	ISBN          string    `json:"isbn"`
	PublishedYear int       `json:"published_year"`
	Genre         string    `json:"genre"`
	Available     bool      `json:"available"`
	CreatedAt     time.Time `json:"created_at"`
	UpdatedAt     time.Time `json:"updated_at"`
}

type CreateBookRequest struct {
	Title         string `json:"title"`
	Author        string `json:"author"`
	ISBN          string `json:"isbn"`
	PublishedYear int    `json:"published_year"`
	Genre         string `json:"genre"`
}

type UpdateBookRequest struct {
	Title         *string `json:"title,omitempty"`
	Author        *string `json:"author,omitempty"`
	ISBN          *string `json:"isbn,omitempty"`
	PublishedYear *int    `json:"published_year,omitempty"`
	Genre         *string `json:"genre,omitempty"`
}

var (
	ErrBookNotFound     = errors.New("book not found")
	ErrBookUnavailable  = errors.New("book is not available for borrowing")
	ErrBookNotBorrowed  = errors.New("book is not currently borrowed")
	ErrInvalidISBN      = errors.New("invalid ISBN format (must be 10 or 13 digits)")
	ErrInvalidYear      = errors.New("invalid published year (must be between 1450 and current year)")
	ErrTitleRequired    = errors.New("title is required")
	ErrAuthorRequired   = errors.New("author is required")
	ErrISBNRequired     = errors.New("ISBN is required")
)

func ValidateISBN(isbn string) bool {
	// Remove hyphens and spaces
	cleaned := regexp.MustCompile(`[-\s]`).ReplaceAllString(isbn, "")

	// Check if it's 10 or 13 digits
	if len(cleaned) != 10 && len(cleaned) != 13 {
		return false
	}

	// Check if all characters are digits (except last char for ISBN-10 which can be X)
	for i, c := range cleaned {
		if c < '0' || c > '9' {
			if !(len(cleaned) == 10 && i == 9 && (c == 'X' || c == 'x')) {
				return false
			}
		}
	}

	return true
}

func ValidateYear(year int) bool {
	currentYear := time.Now().Year()
	return year >= 1450 && year <= currentYear
}

func (r *CreateBookRequest) Validate() error {
	if r.Title == "" {
		return ErrTitleRequired
	}
	if r.Author == "" {
		return ErrAuthorRequired
	}
	if r.ISBN == "" {
		return ErrISBNRequired
	}
	if !ValidateISBN(r.ISBN) {
		return ErrInvalidISBN
	}
	if !ValidateYear(r.PublishedYear) {
		return ErrInvalidYear
	}
	return nil
}

func (r *UpdateBookRequest) Validate() error {
	if r.ISBN != nil && !ValidateISBN(*r.ISBN) {
		return ErrInvalidISBN
	}
	if r.PublishedYear != nil && !ValidateYear(*r.PublishedYear) {
		return ErrInvalidYear
	}
	return nil
}
