package main

import (
	"log"
	nethttp "net/http"

	"github.com/example/userservice/adapter/http"
	"github.com/example/userservice/adapter/repository"
	"github.com/example/userservice/usecase"
)

func main() {
	repo := repository.NewInMemoryUserRepository()
	idGen := func() string { return "id-" + randomString(8) }
	userUseCase := usecase.NewUserUseCase(repo, idGen)
	handler := http.NewUserHandler(userUseCase)

	mux := nethttp.NewServeMux()
	mux.Handle("/users", handler)
	mux.Handle("/users/", handler)

	log.Println("Server starting on :8080")
	log.Fatal(nethttp.ListenAndServe(":8080", mux))
}

func randomString(n int) string {
	const letters = "abcdefghijklmnopqrstuvwxyz0123456789"
	b := make([]byte, n)
	for i := range b {
		b[i] = letters[i%len(letters)]
	}
	return string(b)
}
