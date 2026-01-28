package main

type BookService struct {
	repo BookRepository
}

func NewBookService(repo BookRepository) *BookService {
	return &BookService{repo: repo}
}

func (s *BookService) CreateBook(req *CreateBookRequest) (*Book, error) {
	if err := req.Validate(); err != nil {
		return nil, err
	}

	book := &Book{
		Title:         req.Title,
		Author:        req.Author,
		ISBN:          req.ISBN,
		PublishedYear: req.PublishedYear,
		Genre:         req.Genre,
	}

	return s.repo.Create(book)
}

func (s *BookService) GetBook(id string) (*Book, error) {
	return s.repo.GetByID(id)
}

func (s *BookService) GetAllBooks() ([]*Book, error) {
	return s.repo.GetAll()
}

func (s *BookService) UpdateBook(id string, req *UpdateBookRequest) (*Book, error) {
	if err := req.Validate(); err != nil {
		return nil, err
	}

	book, err := s.repo.GetByID(id)
	if err != nil {
		return nil, err
	}

	if req.Title != nil {
		book.Title = *req.Title
	}
	if req.Author != nil {
		book.Author = *req.Author
	}
	if req.ISBN != nil {
		book.ISBN = *req.ISBN
	}
	if req.PublishedYear != nil {
		book.PublishedYear = *req.PublishedYear
	}
	if req.Genre != nil {
		book.Genre = *req.Genre
	}

	return s.repo.Update(book)
}

func (s *BookService) DeleteBook(id string) error {
	return s.repo.Delete(id)
}

func (s *BookService) BorrowBook(id string) (*Book, error) {
	book, err := s.repo.GetByID(id)
	if err != nil {
		return nil, err
	}

	if !book.Available {
		return nil, ErrBookNotAvailable
	}

	book.Available = false
	return s.repo.Update(book)
}

func (s *BookService) ReturnBook(id string) (*Book, error) {
	book, err := s.repo.GetByID(id)
	if err != nil {
		return nil, err
	}

	if book.Available {
		return nil, ErrBookNotBorrowed
	}

	book.Available = true
	return s.repo.Update(book)
}

func (s *BookService) FindByAuthor(author string) ([]*Book, error) {
	return s.repo.FindByAuthor(author)
}

func (s *BookService) FindByGenre(genre string) ([]*Book, error) {
	return s.repo.FindByGenre(genre)
}
