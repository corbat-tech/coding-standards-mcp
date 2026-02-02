package usecase

import (
	"testing"

	"github.com/example/userservice/adapter/repository"
	"github.com/example/userservice/domain"
)

func TestUserUseCase(t *testing.T) {
	idCounter := 0
	idGen := func() string {
		idCounter++
		return "test-id-" + string(rune(idCounter+48))
	}

	t.Run("CreateUser success", func(t *testing.T) {
		repo := repository.NewInMemoryUserRepository()
		uc := NewUserUseCase(repo, idGen)

		user, err := uc.CreateUser(domain.CreateUserInput{
			Email: "test@test.com",
			Name:  "Test User",
			Role:  "admin",
		})

		if err != nil {
			t.Fatalf("expected no error, got %v", err)
		}
		if user.Email != "test@test.com" {
			t.Errorf("expected email 'test@test.com', got '%s'", user.Email)
		}
	})

	t.Run("CreateUser duplicate email", func(t *testing.T) {
		repo := repository.NewInMemoryUserRepository()
		uc := NewUserUseCase(repo, idGen)

		uc.CreateUser(domain.CreateUserInput{
			Email: "dup@test.com",
			Name:  "User One",
		})

		_, err := uc.CreateUser(domain.CreateUserInput{
			Email: "dup@test.com",
			Name:  "User Two",
		})

		if err != domain.ErrEmailAlreadyExists {
			t.Errorf("expected ErrEmailAlreadyExists, got %v", err)
		}
	})

	t.Run("CreateUser invalid email", func(t *testing.T) {
		repo := repository.NewInMemoryUserRepository()
		uc := NewUserUseCase(repo, idGen)

		_, err := uc.CreateUser(domain.CreateUserInput{
			Email: "invalid",
			Name:  "Test",
		})

		if err != domain.ErrInvalidEmail {
			t.Errorf("expected ErrInvalidEmail, got %v", err)
		}
	})

	t.Run("GetUser success", func(t *testing.T) {
		repo := repository.NewInMemoryUserRepository()
		uc := NewUserUseCase(repo, idGen)

		created, _ := uc.CreateUser(domain.CreateUserInput{
			Email: "get@test.com",
			Name:  "Get User",
		})

		user, err := uc.GetUser(created.ID)
		if err != nil {
			t.Fatalf("expected no error, got %v", err)
		}
		if user.Email != "get@test.com" {
			t.Errorf("expected email 'get@test.com', got '%s'", user.Email)
		}
	})

	t.Run("GetUser not found", func(t *testing.T) {
		repo := repository.NewInMemoryUserRepository()
		uc := NewUserUseCase(repo, idGen)

		_, err := uc.GetUser("nonexistent")
		if err != domain.ErrUserNotFound {
			t.Errorf("expected ErrUserNotFound, got %v", err)
		}
	})

	t.Run("UpdateUser success", func(t *testing.T) {
		repo := repository.NewInMemoryUserRepository()
		uc := NewUserUseCase(repo, idGen)

		created, _ := uc.CreateUser(domain.CreateUserInput{
			Email: "update@test.com",
			Name:  "Original",
		})

		newName := "Updated"
		user, err := uc.UpdateUser(created.ID, domain.UpdateUserInput{
			Name: &newName,
		})

		if err != nil {
			t.Fatalf("expected no error, got %v", err)
		}
		if user.Name != "Updated" {
			t.Errorf("expected name 'Updated', got '%s'", user.Name)
		}
	})

	t.Run("DeleteUser success", func(t *testing.T) {
		repo := repository.NewInMemoryUserRepository()
		uc := NewUserUseCase(repo, idGen)

		created, _ := uc.CreateUser(domain.CreateUserInput{
			Email: "delete@test.com",
			Name:  "Delete Me",
		})

		err := uc.DeleteUser(created.ID)
		if err != nil {
			t.Fatalf("expected no error, got %v", err)
		}

		_, err = uc.GetUser(created.ID)
		if err != domain.ErrUserNotFound {
			t.Errorf("expected ErrUserNotFound after delete, got %v", err)
		}
	})
}
