package infrastructure

import (
	"strings"
	"testing"
)

func TestUUIDGenerator_Generate(t *testing.T) {
	gen := NewUUIDGenerator()

	// Generate multiple IDs and ensure they're unique
	ids := make(map[string]bool)
	for i := 0; i < 1000; i++ {
		id := gen.Generate()
		if ids[id] {
			t.Errorf("UUIDGenerator.Generate() produced duplicate ID: %s", id)
		}
		ids[id] = true

		// Check format (should contain 4 hyphens)
		parts := strings.Split(id, "-")
		if len(parts) != 5 {
			t.Errorf("UUIDGenerator.Generate() = %s, want UUID-like format with 5 parts", id)
		}
	}
}

func TestSequentialIDGenerator_Generate(t *testing.T) {
	gen := NewSequentialIDGenerator("user")

	// Generate multiple IDs and verify sequence
	for i := 1; i <= 10; i++ {
		id := gen.Generate()

		// Just verify it starts with prefix and contains a number
		if !strings.HasPrefix(id, "user-") {
			t.Errorf("SequentialIDGenerator.Generate() = %s, want prefix 'user-'", id)
		}
	}

	// Verify uniqueness
	gen2 := NewSequentialIDGenerator("test")
	ids := make(map[string]bool)
	for i := 0; i < 100; i++ {
		id := gen2.Generate()
		if ids[id] {
			t.Errorf("SequentialIDGenerator.Generate() produced duplicate ID: %s", id)
		}
		ids[id] = true
	}
}

func TestRandomIDGenerator_Generate(t *testing.T) {
	tests := []struct {
		name   string
		length int
		want   int // expected output length
	}{
		{"length 8", 8, 8},
		{"length 16", 16, 16},
		{"length 32", 32, 32},
		{"minimum length", 2, 4}, // should be capped to minimum of 4
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gen := NewRandomIDGenerator(tt.length)
			id := gen.Generate()

			if len(id) != tt.want {
				t.Errorf("RandomIDGenerator.Generate() length = %d, want %d", len(id), tt.want)
			}
		})
	}

	// Test uniqueness
	gen := NewRandomIDGenerator(16)
	ids := make(map[string]bool)
	for i := 0; i < 1000; i++ {
		id := gen.Generate()
		if ids[id] {
			t.Errorf("RandomIDGenerator.Generate() produced duplicate ID: %s", id)
		}
		ids[id] = true
	}
}

func BenchmarkUUIDGenerator_Generate(b *testing.B) {
	gen := NewUUIDGenerator()
	for i := 0; i < b.N; i++ {
		gen.Generate()
	}
}

func BenchmarkSequentialIDGenerator_Generate(b *testing.B) {
	gen := NewSequentialIDGenerator("user")
	for i := 0; i < b.N; i++ {
		gen.Generate()
	}
}

func BenchmarkRandomIDGenerator_Generate(b *testing.B) {
	gen := NewRandomIDGenerator(16)
	for i := 0; i < b.N; i++ {
		gen.Generate()
	}
}
