package http

import (
	"net/http"
	"strings"
)

// Router sets up HTTP routes for the user service
type Router struct {
	handler *UserHandler
}

// NewRouter creates a new Router with the provided handler
func NewRouter(handler *UserHandler) *Router {
	return &Router{
		handler: handler,
	}
}

// SetupRoutes configures the HTTP routes and returns the router
func (rt *Router) SetupRoutes() http.Handler {
	mux := http.NewServeMux()

	// Health check endpoint
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"healthy"}`))
	})

	// User routes
	mux.HandleFunc("/users", rt.handleUsers)
	mux.HandleFunc("/users/", rt.handleUserByID)

	return mux
}

// handleUsers handles requests to /users
func (rt *Router) handleUsers(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		rt.handler.ListUsers(w, r)
	case http.MethodPost:
		rt.handler.CreateUser(w, r)
	default:
		w.Header().Set("Allow", "GET, POST")
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
	}
}

// handleUserByID handles requests to /users/{id}
func (rt *Router) handleUserByID(w http.ResponseWriter, r *http.Request) {
	// Ensure we have an ID in the path
	path := strings.TrimPrefix(r.URL.Path, "/users/")
	if path == "" || path == "/" {
		http.Error(w, "User ID is required", http.StatusBadRequest)
		return
	}

	switch r.Method {
	case http.MethodGet:
		rt.handler.GetUser(w, r)
	case http.MethodPut:
		rt.handler.UpdateUser(w, r)
	case http.MethodDelete:
		rt.handler.DeleteUser(w, r)
	default:
		w.Header().Set("Allow", "GET, PUT, DELETE")
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
	}
}
