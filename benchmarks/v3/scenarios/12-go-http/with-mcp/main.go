// Package main is the entry point for the bookstore API server.
package main

import (
	"log"
	"net/http"

	"bookstore/handler"
	"bookstore/middleware"
	"bookstore/repository"
)

func main() {
	// Initialize dependencies
	repo := repository.NewInMemoryBookRepository()
	bookHandler := handler.NewBookHandler(repo)
	logger := &middleware.DefaultLogger{}

	// Setup routes
	mux := http.NewServeMux()
	setupRoutes(mux, bookHandler)

	// Apply middleware
	loggingMiddleware := middleware.Logging(logger)
	server := loggingMiddleware(mux)

	// Start server
	addr := ":8080"
	log.Printf("Starting server on %s", addr)
	if err := http.ListenAndServe(addr, server); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}

// setupRoutes configures the HTTP routes for the bookstore API.
func setupRoutes(mux *http.ServeMux, h *handler.BookHandler) {
	mux.HandleFunc("/books", func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodGet:
			h.GetAll(w, r)
		case http.MethodPost:
			h.Create(w, r)
		default:
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		}
	})

	mux.HandleFunc("/books/", func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodGet:
			h.GetByID(w, r)
		case http.MethodPut:
			h.Update(w, r)
		case http.MethodDelete:
			h.Delete(w, r)
		default:
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		}
	})
}
