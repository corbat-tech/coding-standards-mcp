// Package middleware provides HTTP middleware components.
package middleware

import (
	"log"
	"net/http"
	"time"
)

// Logger defines the interface for logging operations.
type Logger interface {
	Printf(format string, v ...interface{})
}

// DefaultLogger wraps the standard log package.
type DefaultLogger struct{}

// Printf logs a formatted message.
func (l *DefaultLogger) Printf(format string, v ...interface{}) {
	log.Printf(format, v...)
}

// responseWriter wraps http.ResponseWriter to capture the status code.
type responseWriter struct {
	http.ResponseWriter
	statusCode int
}

// newResponseWriter creates a new responseWriter.
func newResponseWriter(w http.ResponseWriter) *responseWriter {
	return &responseWriter{ResponseWriter: w, statusCode: http.StatusOK}
}

// WriteHeader captures the status code and delegates to the wrapped writer.
func (rw *responseWriter) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

// Logging returns middleware that logs HTTP requests.
func Logging(logger Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()
			wrapped := newResponseWriter(w)

			next.ServeHTTP(wrapped, r)

			logger.Printf(
				"%s %s %d %s",
				r.Method,
				r.URL.Path,
				wrapped.statusCode,
				time.Since(start),
			)
		})
	}
}
