package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

	httpAdapter "github.com/example/userservice/adapter/http"
	"github.com/example/userservice/adapter/repository"
	"github.com/example/userservice/infrastructure"
	"github.com/example/userservice/usecase"
)

func main() {
	// Configuration
	port := getEnv("PORT", "8080")

	// Dependency Injection - Wire up all components

	// Infrastructure layer - ID generator
	idGenerator := infrastructure.NewUUIDGenerator()

	// Interface Adapters layer - Repository
	userRepo := repository.NewMemoryUserRepository()

	// Use Cases layer - Business logic
	userUseCase := usecase.NewUserUseCase(userRepo, idGenerator)

	// Interface Adapters layer - HTTP Handler
	userHandler := httpAdapter.NewUserHandler(userUseCase)

	// Router setup
	router := httpAdapter.NewRouter(userHandler)
	mux := router.SetupRoutes()

	// Start server
	addr := fmt.Sprintf(":%s", port)
	log.Printf("Starting user service on %s", addr)
	log.Printf("Endpoints:")
	log.Printf("  GET    /health       - Health check")
	log.Printf("  GET    /users        - List all users")
	log.Printf("  POST   /users        - Create a new user")
	log.Printf("  GET    /users/{id}   - Get a user by ID")
	log.Printf("  PUT    /users/{id}   - Update a user")
	log.Printf("  DELETE /users/{id}   - Delete a user")

	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

// getEnv returns the value of an environment variable or a default value
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
