pub mod error;
pub mod handlers;
pub mod model;
pub mod repository;

use axum::{
    routing::{delete, get, post, put},
    Router,
};
use repository::NoteRepository;

pub fn create_app<R: NoteRepository + Clone + 'static>(repo: R) -> Router {
    Router::new()
        .route("/health", get(handlers::health_check))
        .route("/notes", post(handlers::create_note::<R>))
        .route("/notes", get(handlers::list_notes::<R>))
        .route("/notes/{id}", get(handlers::get_note::<R>))
        .route("/notes/{id}", put(handlers::update_note::<R>))
        .route("/notes/{id}", delete(handlers::delete_note::<R>))
        .with_state(repo)
}
