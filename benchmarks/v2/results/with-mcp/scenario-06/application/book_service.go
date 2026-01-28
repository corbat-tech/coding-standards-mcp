package application

import (
	"bookstore/domain"
)

type BookService struct {
	repo        domain.BookRepository
	idGenerator domain.IDGenerator
	clock       domain.Clock
}

func NewBookService(
	repo domain.BookRepository,
	idGenerator domain.IDGenerator,
	clock domain.Clock,
) *BookService {
	return &BookService{
		repo:        repo,
		idGenerator: idGenerator,
		clock:       clock,
	}
}

type CreateBookInput struct {
	Title  string
	Author string
	ISBN   string
}

type UpdateBookInput struct {
	Title  string
	Author string
	ISBN   string
}

func (s *BookService) CreateBook(input CreateBookInput) (*domain.Book, error) {
	book, err := domain.NewBook(
		s.idGenerator.Generate(),
		input.Title,
		input.Author,
		input.ISBN,
		s.clock.Now(),
	)
	if err != nil {
		return nil, err
	}

	if err := s.repo.Save(book); err != nil {
		return nil, err
	}

	return book, nil
}

func (s *BookService) GetBook(id string) (*domain.Book, error) {
	book, err := s.repo.FindByID(id)
	if err != nil {
		return nil, err
	}
	if book == nil {
		return nil, domain.ErrBookNotFound
	}
	return book, nil
}

func (s *BookService) ListBooks() ([]*domain.Book, error) {
	return s.repo.FindAll()
}

func (s *BookService) UpdateBook(id string, input UpdateBookInput) (*domain.Book, error) {
	book, err := s.GetBook(id)
	if err != nil {
		return nil, err
	}

	if err := book.Update(input.Title, input.Author, input.ISBN, s.clock.Now()); err != nil {
		return nil, err
	}

	if err := s.repo.Save(book); err != nil {
		return nil, err
	}

	return book, nil
}

func (s *BookService) DeleteBook(id string) error {
	if _, err := s.GetBook(id); err != nil {
		return err
	}
	return s.repo.Delete(id)
}

func (s *BookService) BorrowBook(id, borrowerID string) (*domain.Book, error) {
	book, err := s.GetBook(id)
	if err != nil {
		return nil, err
	}

	if err := book.Borrow(borrowerID, s.clock.Now()); err != nil {
		return nil, err
	}

	if err := s.repo.Save(book); err != nil {
		return nil, err
	}

	return book, nil
}

func (s *BookService) ReturnBook(id string) (*domain.Book, error) {
	book, err := s.GetBook(id)
	if err != nil {
		return nil, err
	}

	if err := book.Return(s.clock.Now()); err != nil {
		return nil, err
	}

	if err := s.repo.Save(book); err != nil {
		return nil, err
	}

	return book, nil
}
