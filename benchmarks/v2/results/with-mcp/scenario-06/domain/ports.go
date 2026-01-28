package domain

import "time"

type IDGenerator interface {
	Generate() string
}

type Clock interface {
	Now() time.Time
}
