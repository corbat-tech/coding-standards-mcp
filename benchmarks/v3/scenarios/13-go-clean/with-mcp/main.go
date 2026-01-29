// Package main is the entry point for the user service.
package main

import (
	"log"
	"net/http"
	"os"

	httpAdapter "github.com/corbat/userservice/adapter/http"
	"github.com/corbat/userservice/adapter/repository"
	"github.com/corbat/userservice/usecase"
)

func main() {
	// Dependency injection: wire up all dependencies
	userRepo := repository.NewInMemoryUserRepository()
	userUseCase := usecase.NewUserUseCase(userRepo)
	userHandler := httpAdapter.NewUserHandler(userUseCase)
	router := httpAdapter.NewRouter(userHandler)

	// Get port from environment or use default
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("Starting user service on port %s", port)
	if err := http.ListenAndServe(":"+port, router); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
