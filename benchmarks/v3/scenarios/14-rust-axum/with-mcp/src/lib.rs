//! Notes API library exposing all modules.

pub mod domain;
pub mod error;
pub mod handlers;
pub mod repository;

use axum::{routing::{get, post, put, delete}, Router};
use std::sync::Arc;

use domain::NoteRepository;
use handlers::{create_note, delete_note, get_note, list_notes, update_note};

/// Creates the Axum router with all routes configured.
pub fn create_router<R: NoteRepository + 'static>(repo: R) -> Router {
    let state = Arc::new(repo);

    Router::new()
        .route("/notes", post(create_note::<R>))
        .route("/notes", get(list_notes::<R>))
        .route("/notes/:id", get(get_note::<R>))
        .route("/notes/:id", put(update_note::<R>))
        .route("/notes/:id", delete(delete_note::<R>))
        .with_state(state)
}
