mod error;
mod model;
mod repository;
mod handler;

use axum::{
    routing::{get, post, put, delete},
    Router,
};
use std::sync::Arc;
use repository::InMemoryNoteRepository;

#[tokio::main]
async fn main() {
    let repo = Arc::new(InMemoryNoteRepository::new());

    let app = Router::new()
        .route("/notes", get(handler::get_all_notes))
        .route("/notes", post(handler::create_note))
        .route("/notes/:id", get(handler::get_note))
        .route("/notes/:id", put(handler::update_note))
        .route("/notes/:id", delete(handler::delete_note))
        .with_state(repo);

    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    println!("Server running on http://localhost:3000");
    axum::serve(listener, app).await.unwrap();
}
