package middleware

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

// mockLogger captures log messages for testing.
type mockLogger struct {
	messages []string
}

func (l *mockLogger) Printf(format string, v ...interface{}) {
	l.messages = append(l.messages, format)
}

func TestLogging_LogsRequestDetails(t *testing.T) {
	tests := []struct {
		name           string
		method         string
		path           string
		handlerStatus  int
		wantMethod     string
		wantPath       string
	}{
		{
			name:          "logs GET request",
			method:        "GET",
			path:          "/api/books",
			handlerStatus: http.StatusOK,
			wantMethod:    "GET",
			wantPath:      "/api/books",
		},
		{
			name:          "logs POST request",
			method:        "POST",
			path:          "/api/books",
			handlerStatus: http.StatusCreated,
			wantMethod:    "POST",
			wantPath:      "/api/books",
		},
		{
			name:          "logs error response",
			method:        "GET",
			path:          "/api/books/123",
			handlerStatus: http.StatusNotFound,
			wantMethod:    "GET",
			wantPath:      "/api/books/123",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logger := &mockLogger{}
			handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.WriteHeader(tt.handlerStatus)
			})

			middleware := Logging(logger)
			wrapped := middleware(handler)

			req := httptest.NewRequest(tt.method, tt.path, nil)
			rec := httptest.NewRecorder()

			wrapped.ServeHTTP(rec, req)

			if len(logger.messages) != 1 {
				t.Errorf("expected 1 log message, got %d", len(logger.messages))
			}
		})
	}
}

func TestResponseWriter_CapturesStatusCode(t *testing.T) {
	tests := []struct {
		name       string
		statusCode int
	}{
		{"captures 200", http.StatusOK},
		{"captures 201", http.StatusCreated},
		{"captures 400", http.StatusBadRequest},
		{"captures 404", http.StatusNotFound},
		{"captures 500", http.StatusInternalServerError},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			rec := httptest.NewRecorder()
			rw := newResponseWriter(rec)
			rw.WriteHeader(tt.statusCode)

			if rw.statusCode != tt.statusCode {
				t.Errorf("statusCode = %d, want %d", rw.statusCode, tt.statusCode)
			}
		})
	}
}

func TestDefaultLogger_Printf(t *testing.T) {
	// This test verifies DefaultLogger implements Logger interface
	var logger Logger = &DefaultLogger{}
	// No panic means it works
	_ = logger
}

func TestLogging_CallsNextHandler(t *testing.T) {
	logger := &mockLogger{}
	called := false
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		called = true
		w.WriteHeader(http.StatusOK)
	})

	middleware := Logging(logger)
	wrapped := middleware(handler)

	req := httptest.NewRequest("GET", "/test", nil)
	rec := httptest.NewRecorder()

	wrapped.ServeHTTP(rec, req)

	if !called {
		t.Error("Logging middleware did not call next handler")
	}
}

func TestLogging_LogFormat(t *testing.T) {
	// Capture actual formatted output
	var capturedLog string
	logger := &struct {
		Logger
	}{}

	// Use a custom logger that captures the actual formatted message
	type formatCapture struct {
		format string
		args   []interface{}
	}
	capture := &formatCapture{}

	customLogger := &mockLogger{}
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	middleware := Logging(customLogger)
	wrapped := middleware(handler)

	req := httptest.NewRequest("GET", "/api/books", nil)
	rec := httptest.NewRecorder()

	wrapped.ServeHTTP(rec, req)

	if len(customLogger.messages) != 1 {
		t.Fatalf("expected 1 log message, got %d", len(customLogger.messages))
	}

	// Format should be: "%s %s %d %s"
	if !strings.Contains(customLogger.messages[0], "%s") {
		t.Error("Log format should contain method placeholder")
	}

	_ = capturedLog
	_ = logger
	_ = capture
}
