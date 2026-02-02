package main

import (
	"log"
	"net/http"

	"github.com/example/bookstore/handler"
	"github.com/example/bookstore/middleware"
	"github.com/example/bookstore/repository"
)

func main() {
	repo := repository.NewInMemoryBookRepository()
	bookHandler := handler.NewBookHandler(repo)

	mux := http.NewServeMux()
	mux.Handle("/books", bookHandler)
	mux.Handle("/books/", bookHandler)

	server := middleware.Logging(mux)

	log.Println("Server starting on :8080")
	log.Fatal(http.ListenAndServe(":8080", server))
}
