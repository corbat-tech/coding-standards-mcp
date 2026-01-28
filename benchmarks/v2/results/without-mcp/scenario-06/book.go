package main

import "time"

type Book struct {
	ID            string    `json:"id"`
	Title         string    `json:"title"`
	Author        string    `json:"author"`
	ISBN          string    `json:"isbn"`
	Available     bool      `json:"available"`
	BorrowedBy    string    `json:"borrowed_by,omitempty"`
	BorrowedAt    time.Time `json:"borrowed_at,omitempty"`
	CreatedAt     time.Time `json:"created_at"`
	UpdatedAt     time.Time `json:"updated_at"`
}

type CreateBookRequest struct {
	Title  string `json:"title"`
	Author string `json:"author"`
	ISBN   string `json:"isbn"`
}

type UpdateBookRequest struct {
	Title  string `json:"title,omitempty"`
	Author string `json:"author,omitempty"`
}

type BorrowRequest struct {
	UserID string `json:"user_id"`
}
