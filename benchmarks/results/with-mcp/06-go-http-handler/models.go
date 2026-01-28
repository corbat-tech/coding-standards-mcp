package main

import (
	"errors"
	"regexp"
	"time"
)

var (
	ErrBookNotFound      = errors.New("book not found")
	ErrBookNotAvailable  = errors.New("book is not available for borrowing")
	ErrBookNotBorrowed   = errors.New("book is not currently borrowed")
	ErrInvalidISBN       = errors.New("invalid ISBN format")
	ErrInvalidYear       = errors.New("published year must be between 1450 and current year")
	ErrTitleRequired     = errors.New("title is required")
	ErrAuthorRequired    = errors.New("author is required")
)

var (
	isbn10Regex = regexp.MustCompile(`^\d{10}$`)
	isbn13Regex = regexp.MustCompile(`^\d{13}$`)
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

type BookResponse struct {
	ID            string `json:"id"`
	Title         string `json:"title"`
	Author        string `json:"author"`
	ISBN          string `json:"isbn"`
	PublishedYear int    `json:"published_year"`
	Genre         string `json:"genre"`
	Available     bool   `json:"available"`
}

type ErrorResponse struct {
	Error string `json:"error"`
}

func ValidateISBN(isbn string) error {
	if !isbn10Regex.MatchString(isbn) && !isbn13Regex.MatchString(isbn) {
		return ErrInvalidISBN
	}
	return nil
}

func ValidatePublishedYear(year int) error {
	currentYear := time.Now().Year()
	if year < 1450 || year > currentYear {
		return ErrInvalidYear
	}
	return nil
}

func (r *CreateBookRequest) Validate() error {
	if r.Title == "" {
		return ErrTitleRequired
	}
	if r.Author == "" {
		return ErrAuthorRequired
	}
	if err := ValidateISBN(r.ISBN); err != nil {
		return err
	}
	if err := ValidatePublishedYear(r.PublishedYear); err != nil {
		return err
	}
	return nil
}

func (r *UpdateBookRequest) Validate() error {
	if r.ISBN != nil {
		if err := ValidateISBN(*r.ISBN); err != nil {
			return err
		}
	}
	if r.PublishedYear != nil {
		if err := ValidatePublishedYear(*r.PublishedYear); err != nil {
			return err
		}
	}
	return nil
}

func ToBookResponse(b *Book) BookResponse {
	return BookResponse{
		ID:            b.ID,
		Title:         b.Title,
		Author:        b.Author,
		ISBN:          b.ISBN,
		PublishedYear: b.PublishedYear,
		Genre:         b.Genre,
		Available:     b.Available,
	}
}
