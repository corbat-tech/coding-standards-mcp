package domain

import (
	"testing"
)

func TestNewUser_ValidInput_ReturnsUser(t *testing.T) {
	user, err := NewUser("John Doe", "john@example.com")

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if user == nil {
		t.Fatal("expected user, got nil")
	}
	if user.Name != "John Doe" {
		t.Errorf("expected name 'John Doe', got '%s'", user.Name)
	}
	if user.Email != "john@example.com" {
		t.Errorf("expected email 'john@example.com', got '%s'", user.Email)
	}
	if user.ID == "" {
		t.Error("expected non-empty ID")
	}
}

func TestNewUser_EmptyName_ReturnsError(t *testing.T) {
	_, err := NewUser("", "john@example.com")

	if err != ErrInvalidName {
		t.Errorf("expected ErrInvalidName, got %v", err)
	}
}

func TestNewUser_WhitespaceName_ReturnsError(t *testing.T) {
	_, err := NewUser("   ", "john@example.com")

	if err != ErrInvalidName {
		t.Errorf("expected ErrInvalidName, got %v", err)
	}
}

func TestNewUser_InvalidEmail_ReturnsError(t *testing.T) {
	testCases := []struct {
		name  string
		email string
	}{
		{"empty email", ""},
		{"no at sign", "johnexample.com"},
		{"no domain", "john@"},
		{"no user", "@example.com"},
		{"invalid format", "john@.com"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := NewUser("John Doe", tc.email)
			if err != ErrInvalidEmail {
				t.Errorf("expected ErrInvalidEmail for '%s', got %v", tc.email, err)
			}
		})
	}
}

func TestNewUser_EmailNormalization(t *testing.T) {
	user, err := NewUser("John", "  JOHN@EXAMPLE.COM  ")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if user.Email != "john@example.com" {
		t.Errorf("expected normalized email 'john@example.com', got '%s'", user.Email)
	}
}

func TestNewUser_NameTrimmed(t *testing.T) {
	user, err := NewUser("  John Doe  ", "john@example.com")

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if user.Name != "John Doe" {
		t.Errorf("expected trimmed name 'John Doe', got '%s'", user.Name)
	}
}

func TestUser_Update_ValidInput_Success(t *testing.T) {
	user, _ := NewUser("John Doe", "john@example.com")
	originalUpdatedAt := user.UpdatedAt

	err := user.Update("Jane Doe", "jane@example.com")

	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}
	if user.Name != "Jane Doe" {
		t.Errorf("expected name 'Jane Doe', got '%s'", user.Name)
	}
	if user.Email != "jane@example.com" {
		t.Errorf("expected email 'jane@example.com', got '%s'", user.Email)
	}
	if !user.UpdatedAt.After(originalUpdatedAt) && user.UpdatedAt != originalUpdatedAt {
		t.Error("expected UpdatedAt to be updated")
	}
}

func TestUser_Update_InvalidName_ReturnsError(t *testing.T) {
	user, _ := NewUser("John Doe", "john@example.com")

	err := user.Update("", "jane@example.com")

	if err != ErrInvalidName {
		t.Errorf("expected ErrInvalidName, got %v", err)
	}
}

func TestUser_Update_InvalidEmail_ReturnsError(t *testing.T) {
	user, _ := NewUser("John Doe", "john@example.com")

	err := user.Update("Jane Doe", "invalid-email")

	if err != ErrInvalidEmail {
		t.Errorf("expected ErrInvalidEmail, got %v", err)
	}
}
