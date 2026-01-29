//! Note entity and DTOs.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// Note entity representing a note in the system.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Note {
    pub id: Uuid,
    pub title: String,
    pub content: String,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}

impl Note {
    /// Creates a new Note with generated ID and timestamps.
    pub fn new(title: String, content: String) -> Self {
        let now = Utc::now();
        Self {
            id: Uuid::new_v4(),
            title,
            content,
            created_at: now,
            updated_at: now,
        }
    }

    /// Updates the note's title and content.
    pub fn update(&mut self, title: Option<String>, content: Option<String>) {
        if let Some(t) = title {
            self.title = t;
        }
        if let Some(c) = content {
            self.content = c;
        }
        self.updated_at = Utc::now();
    }
}

/// Request DTO for creating a new note.
#[derive(Debug, Clone, Deserialize)]
pub struct CreateNoteRequest {
    pub title: String,
    pub content: String,
}

/// Request DTO for updating an existing note.
#[derive(Debug, Clone, Deserialize)]
pub struct UpdateNoteRequest {
    pub title: Option<String>,
    pub content: Option<String>,
}
