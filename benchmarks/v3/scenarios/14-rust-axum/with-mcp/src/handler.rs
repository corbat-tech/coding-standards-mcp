use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use chrono::Utc;
use std::sync::Arc;
use uuid::Uuid;

use crate::error::AppError;
use crate::model::{CreateNoteRequest, Note, UpdateNoteRequest};
use crate::repository::{InMemoryNoteRepository, NoteRepository};

type AppState = Arc<InMemoryNoteRepository>;

pub async fn create_note(
    State(repo): State<AppState>,
    Json(req): Json<CreateNoteRequest>,
) -> Result<(StatusCode, Json<Note>), AppError> {
    if req.title.is_empty() {
        return Err(AppError::Validation("Title is required".to_string()));
    }

    let note = Note::new(req.title, req.content, req.tags);
    let created = repo.create(note)?;
    Ok((StatusCode::CREATED, Json(created)))
}

pub async fn get_note(
    State(repo): State<AppState>,
    Path(id): Path<Uuid>,
) -> Result<Json<Note>, AppError> {
    let note = repo.get_by_id(id)?;
    Ok(Json(note))
}

pub async fn get_all_notes(
    State(repo): State<AppState>,
) -> Result<Json<Vec<Note>>, AppError> {
    let notes = repo.get_all()?;
    Ok(Json(notes))
}

pub async fn update_note(
    State(repo): State<AppState>,
    Path(id): Path<Uuid>,
    Json(req): Json<UpdateNoteRequest>,
) -> Result<Json<Note>, AppError> {
    let mut note = repo.get_by_id(id)?;

    if let Some(title) = req.title {
        note.title = title;
    }
    if let Some(content) = req.content {
        note.content = content;
    }
    if let Some(tags) = req.tags {
        note.tags = tags;
    }
    note.updated_at = Utc::now();

    let updated = repo.update(id, note)?;
    Ok(Json(updated))
}

pub async fn delete_note(
    State(repo): State<AppState>,
    Path(id): Path<Uuid>,
) -> Result<StatusCode, AppError> {
    repo.delete(id)?;
    Ok(StatusCode::NO_CONTENT)
}
