package usecase

import (
	"time"

	"github.com/example/userservice/domain"
)

type UserUseCase interface {
	CreateUser(input domain.CreateUserInput) (*domain.User, error)
	GetUser(id string) (*domain.User, error)
	GetAllUsers() ([]*domain.User, error)
	UpdateUser(id string, input domain.UpdateUserInput) (*domain.User, error)
	DeleteUser(id string) error
}

type userUseCase struct {
	repo   domain.UserRepository
	idGen  func() string
}

func NewUserUseCase(repo domain.UserRepository, idGen func() string) UserUseCase {
	return &userUseCase{repo: repo, idGen: idGen}
}

func (uc *userUseCase) CreateUser(input domain.CreateUserInput) (*domain.User, error) {
	if err := input.Validate(); err != nil {
		return nil, err
	}

	existing, _ := uc.repo.GetByEmail(input.Email)
	if existing != nil {
		return nil, domain.ErrEmailAlreadyExists
	}

	now := time.Now()
	user := &domain.User{
		ID:        uc.idGen(),
		Email:     input.Email,
		Name:      input.Name,
		Role:      input.Role,
		CreatedAt: now,
		UpdatedAt: now,
	}

	if err := uc.repo.Create(user); err != nil {
		return nil, err
	}

	return user, nil
}

func (uc *userUseCase) GetUser(id string) (*domain.User, error) {
	return uc.repo.GetByID(id)
}

func (uc *userUseCase) GetAllUsers() ([]*domain.User, error) {
	return uc.repo.GetAll()
}

func (uc *userUseCase) UpdateUser(id string, input domain.UpdateUserInput) (*domain.User, error) {
	user, err := uc.repo.GetByID(id)
	if err != nil {
		return nil, err
	}

	if input.Email != nil && *input.Email != user.Email {
		existing, _ := uc.repo.GetByEmail(*input.Email)
		if existing != nil {
			return nil, domain.ErrEmailAlreadyExists
		}
		user.Email = *input.Email
	}

	if input.Name != nil {
		user.Name = *input.Name
	}

	if input.Role != nil {
		user.Role = *input.Role
	}

	user.UpdatedAt = time.Now()

	if err := uc.repo.Update(id, user); err != nil {
		return nil, err
	}

	return user, nil
}

func (uc *userUseCase) DeleteUser(id string) error {
	return uc.repo.Delete(id)
}
