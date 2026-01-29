package domain

import (
	"regexp"
	"strings"
	"time"

	"github.com/google/uuid"
)

// User represents the user entity in the domain layer.
type User struct {
	ID        string
	Name      string
	Email     string
	CreatedAt time.Time
	UpdatedAt time.Time
}

// emailRegex is the regular expression for validating email format.
var emailRegex = regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)

// NewUser creates a new User with validation.
// Returns an error if email or name is invalid.
func NewUser(name, email string) (*User, error) {
	if err := validateName(name); err != nil {
		return nil, err
	}
	if err := validateEmail(email); err != nil {
		return nil, err
	}

	now := time.Now()
	return &User{
		ID:        uuid.New().String(),
		Name:      strings.TrimSpace(name),
		Email:     strings.ToLower(strings.TrimSpace(email)),
		CreatedAt: now,
		UpdatedAt: now,
	}, nil
}

// Update modifies user fields with validation.
func (u *User) Update(name, email string) error {
	if err := validateName(name); err != nil {
		return err
	}
	if err := validateEmail(email); err != nil {
		return err
	}

	u.Name = strings.TrimSpace(name)
	u.Email = strings.ToLower(strings.TrimSpace(email))
	u.UpdatedAt = time.Now()
	return nil
}

// validateEmail checks if the email format is valid.
func validateEmail(email string) error {
	email = strings.TrimSpace(email)
	if email == "" || !emailRegex.MatchString(email) {
		return ErrInvalidEmail
	}
	return nil
}

// validateName checks if the name is valid.
func validateName(name string) error {
	if strings.TrimSpace(name) == "" {
		return ErrInvalidName
	}
	return nil
}
