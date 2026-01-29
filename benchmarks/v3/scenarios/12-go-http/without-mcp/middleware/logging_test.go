package middleware

import (
	"bytes"
	"log"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestLoggingMiddleware(t *testing.T) {
	tests := []struct {
		name           string
		method         string
		path           string
		expectedStatus int
	}{
		{
			name:           "GET request",
			method:         http.MethodGet,
			path:           "/api/books",
			expectedStatus: http.StatusOK,
		},
		{
			name:           "POST request",
			method:         http.MethodPost,
			path:           "/api/books",
			expectedStatus: http.StatusCreated,
		},
		{
			name:           "DELETE request",
			method:         http.MethodDelete,
			path:           "/api/books/123",
			expectedStatus: http.StatusNoContent,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Capture log output
			var logBuf bytes.Buffer
			log.SetOutput(&logBuf)
			defer log.SetOutput(nil)

			// Create a simple handler that returns the expected status
			handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(tt.expectedStatus)
			})

			// Wrap with logging middleware
			loggedHandler := Logging(handler)

			// Make request
			req := httptest.NewRequest(tt.method, tt.path, nil)
			rec := httptest.NewRecorder()
			loggedHandler.ServeHTTP(rec, req)

			// Verify status code
			if rec.Code != tt.expectedStatus {
				t.Errorf("expected status %d, got %d", tt.expectedStatus, rec.Code)
			}

			// Verify log output contains request info
			logOutput := logBuf.String()
			if !strings.Contains(logOutput, tt.method) {
				t.Errorf("log output should contain method %s", tt.method)
			}
			if !strings.Contains(logOutput, tt.path) {
				t.Errorf("log output should contain path %s", tt.path)
			}
		})
	}
}

func TestContentTypeMiddleware(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	wrappedHandler := ContentType(handler)

	req := httptest.NewRequest(http.MethodGet, "/test", nil)
	rec := httptest.NewRecorder()
	wrappedHandler.ServeHTTP(rec, req)

	contentType := rec.Header().Get("Content-Type")
	if contentType != "application/json" {
		t.Errorf("expected Content-Type 'application/json', got %q", contentType)
	}
}

func TestChainMiddleware(t *testing.T) {
	// Track middleware execution order
	var order []string

	middleware1 := func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			order = append(order, "m1-before")
			next.ServeHTTP(w, r)
			order = append(order, "m1-after")
		})
	}

	middleware2 := func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			order = append(order, "m2-before")
			next.ServeHTTP(w, r)
			order = append(order, "m2-after")
		})
	}

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		order = append(order, "handler")
		w.WriteHeader(http.StatusOK)
	})

	chainedHandler := Chain(handler, middleware1, middleware2)

	req := httptest.NewRequest(http.MethodGet, "/test", nil)
	rec := httptest.NewRecorder()
	chainedHandler.ServeHTTP(rec, req)

	// Middleware should be applied in order: m1 -> m2 -> handler -> m2 -> m1
	expectedOrder := []string{"m1-before", "m2-before", "handler", "m2-after", "m1-after"}
	if len(order) != len(expectedOrder) {
		t.Fatalf("expected %d middleware calls, got %d", len(expectedOrder), len(order))
	}

	for i, expected := range expectedOrder {
		if order[i] != expected {
			t.Errorf("expected order[%d] = %q, got %q", i, expected, order[i])
		}
	}
}

func TestResponseWriterStatusCapture(t *testing.T) {
	tests := []struct {
		name           string
		writeStatus    int
		expectedStatus int
	}{
		{
			name:           "200 OK",
			writeStatus:    http.StatusOK,
			expectedStatus: http.StatusOK,
		},
		{
			name:           "404 Not Found",
			writeStatus:    http.StatusNotFound,
			expectedStatus: http.StatusNotFound,
		},
		{
			name:           "500 Internal Server Error",
			writeStatus:    http.StatusInternalServerError,
			expectedStatus: http.StatusInternalServerError,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var capturedStatus int

			handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(tt.writeStatus)
			})

			loggingHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				wrapped := &responseWriter{
					ResponseWriter: w,
					statusCode:     http.StatusOK,
				}
				handler.ServeHTTP(wrapped, r)
				capturedStatus = wrapped.statusCode
			})

			req := httptest.NewRequest(http.MethodGet, "/test", nil)
			rec := httptest.NewRecorder()
			loggingHandler.ServeHTTP(rec, req)

			if capturedStatus != tt.expectedStatus {
				t.Errorf("expected captured status %d, got %d", tt.expectedStatus, capturedStatus)
			}
		})
	}
}
