use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use uuid::Uuid;

use crate::error::{NoteError, Result};
use crate::model::{CreateNoteRequest, Note, UpdateNoteRequest};
use crate::repository::NoteRepository;

pub async fn create_note<R: NoteRepository>(
    State(repo): State<R>,
    Json(payload): Json<CreateNoteRequest>,
) -> Result<(StatusCode, Json<Note>)> {
    if payload.title.trim().is_empty() {
        return Err(NoteError::InvalidData("Title cannot be empty".to_string()));
    }

    let note = Note::new(payload.title, payload.content);
    let created = repo.create(note).await?;
    Ok((StatusCode::CREATED, Json(created)))
}

pub async fn get_note<R: NoteRepository>(
    State(repo): State<R>,
    Path(id): Path<Uuid>,
) -> Result<Json<Note>> {
    let note = repo.get_by_id(id).await?;
    Ok(Json(note))
}

pub async fn list_notes<R: NoteRepository>(State(repo): State<R>) -> Result<Json<Vec<Note>>> {
    let notes = repo.get_all().await?;
    Ok(Json(notes))
}

pub async fn update_note<R: NoteRepository>(
    State(repo): State<R>,
    Path(id): Path<Uuid>,
    Json(payload): Json<UpdateNoteRequest>,
) -> Result<Json<Note>> {
    if let Some(ref title) = payload.title {
        if title.trim().is_empty() {
            return Err(NoteError::InvalidData("Title cannot be empty".to_string()));
        }
    }

    let mut note = repo.get_by_id(id).await?;
    note.update(payload.title, payload.content);
    let updated = repo.update(note).await?;
    Ok(Json(updated))
}

pub async fn delete_note<R: NoteRepository>(
    State(repo): State<R>,
    Path(id): Path<Uuid>,
) -> Result<StatusCode> {
    repo.delete(id).await?;
    Ok(StatusCode::NO_CONTENT)
}

pub async fn health_check() -> &'static str {
    "OK"
}
