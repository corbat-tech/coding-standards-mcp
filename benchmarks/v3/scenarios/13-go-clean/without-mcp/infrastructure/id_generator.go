package infrastructure

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"sync/atomic"
	"time"
)

// UUIDGenerator generates unique identifiers using a UUID-like format
type UUIDGenerator struct{}

// NewUUIDGenerator creates a new UUIDGenerator
func NewUUIDGenerator() *UUIDGenerator {
	return &UUIDGenerator{}
}

// Generate generates a unique identifier
func (g *UUIDGenerator) Generate() string {
	b := make([]byte, 16)
	_, err := rand.Read(b)
	if err != nil {
		// Fallback to timestamp-based ID if random fails
		return fmt.Sprintf("user-%d", time.Now().UnixNano())
	}
	return fmt.Sprintf("%x-%x-%x-%x-%x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}

// SequentialIDGenerator generates sequential identifiers (useful for testing)
type SequentialIDGenerator struct {
	counter uint64
	prefix  string
}

// NewSequentialIDGenerator creates a new SequentialIDGenerator with the given prefix
func NewSequentialIDGenerator(prefix string) *SequentialIDGenerator {
	return &SequentialIDGenerator{
		counter: 0,
		prefix:  prefix,
	}
}

// Generate generates a sequential unique identifier
func (g *SequentialIDGenerator) Generate() string {
	id := atomic.AddUint64(&g.counter, 1)
	return fmt.Sprintf("%s-%d", g.prefix, id)
}

// RandomIDGenerator generates random hex-based identifiers
type RandomIDGenerator struct {
	length int
}

// NewRandomIDGenerator creates a new RandomIDGenerator with the specified length
func NewRandomIDGenerator(length int) *RandomIDGenerator {
	if length < 4 {
		length = 4
	}
	return &RandomIDGenerator{
		length: length,
	}
}

// Generate generates a random hex identifier
func (g *RandomIDGenerator) Generate() string {
	b := make([]byte, g.length/2)
	_, err := rand.Read(b)
	if err != nil {
		return fmt.Sprintf("id-%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(b)
}
