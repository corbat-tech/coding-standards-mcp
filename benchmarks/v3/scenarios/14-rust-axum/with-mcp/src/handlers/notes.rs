//! Note handlers implementing REST endpoints.

use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use std::sync::Arc;
use uuid::Uuid;

use crate::domain::{CreateNoteRequest, Note, NoteRepository, UpdateNoteRequest};
use crate::error::NoteError;

/// Application state containing the repository.
pub type AppState<R> = Arc<R>;

/// Creates a new note.
/// POST /notes
pub async fn create_note<R: NoteRepository>(
    State(repo): State<AppState<R>>,
    Json(request): Json<CreateNoteRequest>,
) -> Result<(StatusCode, Json<Note>), NoteError> {
    if request.title.trim().is_empty() {
        return Err(NoteError::InvalidInput("Title cannot be empty".to_string()));
    }

    let note = Note::new(request.title, request.content);
    let created = repo.create(note).await?;
    Ok((StatusCode::CREATED, Json(created)))
}

/// Gets a note by ID.
/// GET /notes/:id
pub async fn get_note<R: NoteRepository>(
    State(repo): State<AppState<R>>,
    Path(id): Path<Uuid>,
) -> Result<Json<Note>, NoteError> {
    let note = repo.get_by_id(id).await?;
    Ok(Json(note))
}

/// Lists all notes.
/// GET /notes
pub async fn list_notes<R: NoteRepository>(
    State(repo): State<AppState<R>>,
) -> Result<Json<Vec<Note>>, NoteError> {
    let notes = repo.list().await?;
    Ok(Json(notes))
}

/// Updates an existing note.
/// PUT /notes/:id
pub async fn update_note<R: NoteRepository>(
    State(repo): State<AppState<R>>,
    Path(id): Path<Uuid>,
    Json(request): Json<UpdateNoteRequest>,
) -> Result<Json<Note>, NoteError> {
    let mut note = repo.get_by_id(id).await?;
    note.update(request.title, request.content);
    let updated = repo.update(id, note).await?;
    Ok(Json(updated))
}

/// Deletes a note.
/// DELETE /notes/:id
pub async fn delete_note<R: NoteRepository>(
    State(repo): State<AppState<R>>,
    Path(id): Path<Uuid>,
) -> Result<StatusCode, NoteError> {
    repo.delete(id).await?;
    Ok(StatusCode::NO_CONTENT)
}
