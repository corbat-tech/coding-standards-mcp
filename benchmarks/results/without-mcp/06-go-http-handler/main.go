package main

import (
	"log"
	"net/http"
)

func main() {
	repo := NewBookRepository()
	handler := NewBookHandler(repo)
	router := SetupRoutes(handler)

	log.Println("Server starting on :8080")
	if err := http.ListenAndServe(":8080", router); err != nil {
		log.Fatal(err)
	}
}
