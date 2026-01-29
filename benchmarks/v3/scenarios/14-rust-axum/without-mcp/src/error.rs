use axum::{
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;
use thiserror::Error;
use uuid::Uuid;

#[derive(Error, Debug)]
pub enum NoteError {
    #[error("Note not found with id: {0}")]
    NotFound(Uuid),

    #[error("Invalid note data: {0}")]
    InvalidData(String),

    #[error("Internal server error: {0}")]
    Internal(String),
}

impl IntoResponse for NoteError {
    fn into_response(self) -> Response {
        let (status, message) = match &self {
            NoteError::NotFound(_) => (StatusCode::NOT_FOUND, self.to_string()),
            NoteError::InvalidData(_) => (StatusCode::BAD_REQUEST, self.to_string()),
            NoteError::Internal(_) => (StatusCode::INTERNAL_SERVER_ERROR, self.to_string()),
        };

        let body = Json(json!({
            "error": message
        }));

        (status, body).into_response()
    }
}

pub type Result<T> = std::result::Result<T, NoteError>;
