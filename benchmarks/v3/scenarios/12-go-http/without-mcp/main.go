package main

import (
	"log"
	"net/http"

	"bookstore/handlers"
	"bookstore/middleware"
	"bookstore/store"
)

func main() {
	// Initialize the store
	bookStore := store.NewBookStore()

	// Initialize handlers
	bookHandler := handlers.NewBookHandler(bookStore)

	// Setup router
	mux := http.NewServeMux()

	// Register routes
	mux.Handle("/api/books", bookHandler)
	mux.Handle("/api/books/", bookHandler)

	// Health check endpoint
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"ok"}`))
	})

	// Apply middleware
	handler := middleware.Chain(mux, middleware.Logging, middleware.ContentType)

	// Start server
	addr := ":8080"
	log.Printf("Starting bookstore API server on %s", addr)
	if err := http.ListenAndServe(addr, handler); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
