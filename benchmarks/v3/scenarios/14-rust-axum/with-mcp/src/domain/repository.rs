//! Repository trait (interface) for Note persistence.

use async_trait::async_trait;
use uuid::Uuid;

use super::Note;
use crate::error::NoteError;

/// Repository trait defining the contract for Note persistence.
/// This trait acts as an interface for dependency injection.
#[async_trait]
pub trait NoteRepository: Send + Sync {
    /// Creates a new note and returns it.
    async fn create(&self, note: Note) -> Result<Note, NoteError>;

    /// Retrieves a note by its ID.
    async fn get_by_id(&self, id: Uuid) -> Result<Note, NoteError>;

    /// Lists all notes.
    async fn list(&self) -> Result<Vec<Note>, NoteError>;

    /// Updates an existing note.
    async fn update(&self, id: Uuid, note: Note) -> Result<Note, NoteError>;

    /// Deletes a note by its ID.
    async fn delete(&self, id: Uuid) -> Result<(), NoteError>;
}
