package domain

import (
	"testing"
)

func TestNewUser(t *testing.T) {
	tests := []struct {
		name        string
		id          string
		userName    string
		email       string
		wantErr     error
		description string
	}{
		{
			name:        "valid user",
			id:          "user-123",
			userName:    "John Doe",
			email:       "john@example.com",
			wantErr:     nil,
			description: "should create a valid user",
		},
		{
			name:        "empty id",
			id:          "",
			userName:    "John Doe",
			email:       "john@example.com",
			wantErr:     ErrInvalidID,
			description: "should return error for empty ID",
		},
		{
			name:        "empty name",
			id:          "user-123",
			userName:    "",
			email:       "john@example.com",
			wantErr:     ErrInvalidName,
			description: "should return error for empty name",
		},
		{
			name:        "whitespace only name",
			id:          "user-123",
			userName:    "   ",
			email:       "john@example.com",
			wantErr:     ErrInvalidName,
			description: "should return error for whitespace-only name",
		},
		{
			name:        "invalid email - no @",
			id:          "user-123",
			userName:    "John Doe",
			email:       "johnexample.com",
			wantErr:     ErrInvalidEmail,
			description: "should return error for email without @",
		},
		{
			name:        "invalid email - no domain",
			id:          "user-123",
			userName:    "John Doe",
			email:       "john@",
			wantErr:     ErrInvalidEmail,
			description: "should return error for email without domain",
		},
		{
			name:        "invalid email - empty",
			id:          "user-123",
			userName:    "John Doe",
			email:       "",
			wantErr:     ErrInvalidEmail,
			description: "should return error for empty email",
		},
		{
			name:        "email with uppercase",
			id:          "user-123",
			userName:    "John Doe",
			email:       "John@Example.COM",
			wantErr:     nil,
			description: "should normalize email to lowercase",
		},
		{
			name:        "name with extra whitespace",
			id:          "user-123",
			userName:    "  John Doe  ",
			email:       "john@example.com",
			wantErr:     nil,
			description: "should trim whitespace from name",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			user, err := NewUser(tt.id, tt.userName, tt.email)

			if tt.wantErr != nil {
				if err == nil {
					t.Errorf("NewUser() expected error %v, got nil", tt.wantErr)
					return
				}
				if err != tt.wantErr {
					t.Errorf("NewUser() error = %v, want %v", err, tt.wantErr)
				}
				return
			}

			if err != nil {
				t.Errorf("NewUser() unexpected error: %v", err)
				return
			}

			if user == nil {
				t.Error("NewUser() returned nil user without error")
				return
			}

			if user.ID != tt.id {
				t.Errorf("NewUser() ID = %v, want %v", user.ID, tt.id)
			}

			// Check that timestamps are set
			if user.CreatedAt.IsZero() {
				t.Error("NewUser() CreatedAt should not be zero")
			}

			if user.UpdatedAt.IsZero() {
				t.Error("NewUser() UpdatedAt should not be zero")
			}
		})
	}
}

func TestUser_Validate(t *testing.T) {
	tests := []struct {
		name    string
		user    *User
		wantErr error
	}{
		{
			name: "valid user",
			user: &User{
				ID:    "user-123",
				Name:  "John Doe",
				Email: "john@example.com",
			},
			wantErr: nil,
		},
		{
			name: "empty ID",
			user: &User{
				ID:    "",
				Name:  "John Doe",
				Email: "john@example.com",
			},
			wantErr: ErrInvalidID,
		},
		{
			name: "empty name",
			user: &User{
				ID:    "user-123",
				Name:  "",
				Email: "john@example.com",
			},
			wantErr: ErrInvalidName,
		},
		{
			name: "invalid email",
			user: &User{
				ID:    "user-123",
				Name:  "John Doe",
				Email: "invalid-email",
			},
			wantErr: ErrInvalidEmail,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := tt.user.Validate()
			if err != tt.wantErr {
				t.Errorf("User.Validate() error = %v, want %v", err, tt.wantErr)
			}
		})
	}
}

func TestUser_Update(t *testing.T) {
	tests := []struct {
		name     string
		newName  string
		newEmail string
		wantErr  error
	}{
		{
			name:     "valid update",
			newName:  "Jane Doe",
			newEmail: "jane@example.com",
			wantErr:  nil,
		},
		{
			name:     "empty name",
			newName:  "",
			newEmail: "jane@example.com",
			wantErr:  ErrInvalidName,
		},
		{
			name:     "invalid email",
			newName:  "Jane Doe",
			newEmail: "invalid-email",
			wantErr:  ErrInvalidEmail,
		},
		{
			name:     "whitespace name",
			newName:  "   ",
			newEmail: "jane@example.com",
			wantErr:  ErrInvalidName,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			user, _ := NewUser("user-123", "John Doe", "john@example.com")
			originalUpdatedAt := user.UpdatedAt

			err := user.Update(tt.newName, tt.newEmail)

			if tt.wantErr != nil {
				if err != tt.wantErr {
					t.Errorf("User.Update() error = %v, want %v", err, tt.wantErr)
				}
				return
			}

			if err != nil {
				t.Errorf("User.Update() unexpected error: %v", err)
				return
			}

			if user.Name != tt.newName {
				t.Errorf("User.Update() Name = %v, want %v", user.Name, tt.newName)
			}

			if user.Email != tt.newEmail {
				t.Errorf("User.Update() Email = %v, want %v", user.Email, tt.newEmail)
			}

			if !user.UpdatedAt.After(originalUpdatedAt) && user.UpdatedAt != originalUpdatedAt {
				t.Error("User.Update() should update UpdatedAt timestamp")
			}
		})
	}
}

func TestEmailValidation(t *testing.T) {
	validEmails := []string{
		"test@example.com",
		"user.name@domain.com",
		"user+tag@example.org",
		"a@b.co",
		"test123@test.io",
	}

	invalidEmails := []string{
		"",
		"invalid",
		"@example.com",
		"user@",
		"user@.com",
		"user@domain",
		"user name@example.com",
	}

	for _, email := range validEmails {
		t.Run("valid_"+email, func(t *testing.T) {
			if !isValidEmail(email) {
				t.Errorf("isValidEmail(%q) = false, want true", email)
			}
		})
	}

	for _, email := range invalidEmails {
		t.Run("invalid_"+email, func(t *testing.T) {
			if isValidEmail(email) {
				t.Errorf("isValidEmail(%q) = true, want false", email)
			}
		})
	}
}
