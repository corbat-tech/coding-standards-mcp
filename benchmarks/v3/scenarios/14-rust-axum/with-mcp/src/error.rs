//! Custom error types using thiserror.

use axum::{
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;
use thiserror::Error;
use uuid::Uuid;

/// Custom error types for the Notes API.
#[derive(Debug, Error)]
pub enum NoteError {
    #[error("Note not found: {0}")]
    NotFound(Uuid),

    #[error("Invalid input: {0}")]
    InvalidInput(String),

    #[error("Internal error: {0}")]
    Internal(String),
}

impl IntoResponse for NoteError {
    fn into_response(self) -> Response {
        let (status, message) = match &self {
            NoteError::NotFound(id) => {
                (StatusCode::NOT_FOUND, format!("Note not found: {}", id))
            }
            NoteError::InvalidInput(msg) => {
                (StatusCode::BAD_REQUEST, msg.clone())
            }
            NoteError::Internal(msg) => {
                (StatusCode::INTERNAL_SERVER_ERROR, msg.clone())
            }
        };

        let body = Json(json!({
            "error": message,
            "code": status.as_u16()
        }));

        (status, body).into_response()
    }
}
