package domain

import (
	"testing"
)

func TestBook_Validate(t *testing.T) {
	tests := []struct {
		name    string
		book    Book
		wantErr error
	}{
		{
			name: "valid book",
			book: Book{
				ID:     "1",
				ISBN:   "978-0-13-468599-1",
				Title:  "The Go Programming Language",
				Author: "Alan A. A. Donovan",
				Price:  39.99,
			},
			wantErr: nil,
		},
		{
			name: "empty title",
			book: Book{
				ID:     "1",
				ISBN:   "978-0-13-468599-1",
				Title:  "",
				Author: "Alan A. A. Donovan",
				Price:  39.99,
			},
			wantErr: ErrEmptyTitle,
		},
		{
			name: "empty author",
			book: Book{
				ID:     "1",
				ISBN:   "978-0-13-468599-1",
				Title:  "The Go Programming Language",
				Author: "",
				Price:  39.99,
			},
			wantErr: ErrEmptyAuthor,
		},
		{
			name: "empty ISBN",
			book: Book{
				ID:     "1",
				ISBN:   "",
				Title:  "The Go Programming Language",
				Author: "Alan A. A. Donovan",
				Price:  39.99,
			},
			wantErr: ErrInvalidISBN,
		},
		{
			name: "negative price",
			book: Book{
				ID:     "1",
				ISBN:   "978-0-13-468599-1",
				Title:  "The Go Programming Language",
				Author: "Alan A. A. Donovan",
				Price:  -10.00,
			},
			wantErr: ErrNegativePrice,
		},
		{
			name: "zero price is valid",
			book: Book{
				ID:     "1",
				ISBN:   "978-0-13-468599-1",
				Title:  "The Go Programming Language",
				Author: "Alan A. A. Donovan",
				Price:  0,
			},
			wantErr: nil,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := tt.book.Validate()
			if err != tt.wantErr {
				t.Errorf("Book.Validate() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
