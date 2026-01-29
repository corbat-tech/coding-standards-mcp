package domain

import (
	"regexp"
	"strings"
	"time"
)

// User represents the user entity in the domain layer
type User struct {
	ID        string    `json:"id"`
	Name      string    `json:"name"`
	Email     string    `json:"email"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// emailRegex is a simple regex for email validation
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)

// NewUser creates a new User with validation
func NewUser(id, name, email string) (*User, error) {
	user := &User{
		ID:        id,
		Name:      strings.TrimSpace(name),
		Email:     strings.ToLower(strings.TrimSpace(email)),
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	if err := user.Validate(); err != nil {
		return nil, err
	}

	return user, nil
}

// Validate validates the user entity
func (u *User) Validate() error {
	if u.ID == "" {
		return ErrInvalidID
	}

	if strings.TrimSpace(u.Name) == "" {
		return ErrInvalidName
	}

	if !isValidEmail(u.Email) {
		return ErrInvalidEmail
	}

	return nil
}

// Update updates user fields and sets UpdatedAt timestamp
func (u *User) Update(name, email string) error {
	trimmedName := strings.TrimSpace(name)
	trimmedEmail := strings.ToLower(strings.TrimSpace(email))

	if trimmedName == "" {
		return ErrInvalidName
	}

	if !isValidEmail(trimmedEmail) {
		return ErrInvalidEmail
	}

	u.Name = trimmedName
	u.Email = trimmedEmail
	u.UpdatedAt = time.Now()

	return nil
}

// isValidEmail checks if an email address is valid
func isValidEmail(email string) bool {
	return emailRegex.MatchString(email)
}
