package http

import (
	"net/http"
	"strings"
)

// Router provides simple HTTP routing for the user service.
type Router struct {
	handler *UserHandler
}

// NewRouter creates a new Router with the given handler.
func NewRouter(handler *UserHandler) *Router {
	return &Router{handler: handler}
}

// ServeHTTP implements the http.Handler interface.
func (r *Router) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	path := strings.TrimSuffix(req.URL.Path, "/")

	switch {
	case path == "/users" && req.Method == http.MethodPost:
		r.handler.CreateUser(w, req)
	case path == "/users" && req.Method == http.MethodGet:
		r.handler.ListUsers(w, req)
	case strings.HasPrefix(path, "/users/") && req.Method == http.MethodGet:
		id := strings.TrimPrefix(path, "/users/")
		r.handler.GetUser(w, req, id)
	case strings.HasPrefix(path, "/users/") && req.Method == http.MethodPut:
		id := strings.TrimPrefix(path, "/users/")
		r.handler.UpdateUser(w, req, id)
	case strings.HasPrefix(path, "/users/") && req.Method == http.MethodDelete:
		id := strings.TrimPrefix(path, "/users/")
		r.handler.DeleteUser(w, req, id)
	default:
		w.WriteHeader(http.StatusNotFound)
	}
}
