package domain

import (
	"errors"
	"time"
)

var (
	ErrUserNotFound      = errors.New("user not found")
	ErrEmailAlreadyExists = errors.New("email already exists")
	ErrInvalidEmail      = errors.New("invalid email format")
	ErrInvalidName       = errors.New("name must be at least 2 characters")
)

type User struct {
	ID        string    `json:"id"`
	Email     string    `json:"email"`
	Name      string    `json:"name"`
	Role      string    `json:"role"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

type CreateUserInput struct {
	Email string `json:"email"`
	Name  string `json:"name"`
	Role  string `json:"role"`
}

type UpdateUserInput struct {
	Email *string `json:"email,omitempty"`
	Name  *string `json:"name,omitempty"`
	Role  *string `json:"role,omitempty"`
}

func (c *CreateUserInput) Validate() error {
	if len(c.Email) < 5 || !containsAt(c.Email) {
		return ErrInvalidEmail
	}
	if len(c.Name) < 2 {
		return ErrInvalidName
	}
	if c.Role == "" {
		c.Role = "user"
	}
	return nil
}

func containsAt(s string) bool {
	for _, c := range s {
		if c == '@' {
			return true
		}
	}
	return false
}
