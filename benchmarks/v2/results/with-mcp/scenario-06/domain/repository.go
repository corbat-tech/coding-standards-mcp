package domain

type BookRepository interface {
	FindByID(id string) (*Book, error)
	FindAll() ([]*Book, error)
	Save(book *Book) error
	Delete(id string) error
}
