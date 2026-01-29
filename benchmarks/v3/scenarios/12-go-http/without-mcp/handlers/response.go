package handlers

import (
	"encoding/json"
	"net/http"
)

// ErrorResponse represents an error response
type ErrorResponse struct {
	Error   string `json:"error"`
	Message string `json:"message,omitempty"`
}

// SuccessResponse represents a success response with data
type SuccessResponse struct {
	Data interface{} `json:"data"`
}

// ListResponse represents a list response
type ListResponse struct {
	Data  interface{} `json:"data"`
	Count int         `json:"count"`
}

// writeJSON writes a JSON response with the given status code
func writeJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if data != nil {
		json.NewEncoder(w).Encode(data)
	}
}

// writeError writes an error response
func writeError(w http.ResponseWriter, status int, err string, message string) {
	writeJSON(w, status, ErrorResponse{
		Error:   err,
		Message: message,
	})
}

// writeSuccess writes a success response with data
func writeSuccess(w http.ResponseWriter, status int, data interface{}) {
	writeJSON(w, status, SuccessResponse{Data: data})
}

// writeList writes a list response
func writeList(w http.ResponseWriter, data interface{}, count int) {
	writeJSON(w, http.StatusOK, ListResponse{
		Data:  data,
		Count: count,
	})
}
