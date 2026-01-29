//! In-memory implementation of NoteRepository.

use async_trait::async_trait;
use std::collections::HashMap;
use std::sync::RwLock;
use uuid::Uuid;

use crate::domain::{Note, NoteRepository};
use crate::error::NoteError;

/// In-memory implementation of NoteRepository using RwLock for thread safety.
pub struct InMemoryNoteRepository {
    notes: RwLock<HashMap<Uuid, Note>>,
}

impl InMemoryNoteRepository {
    /// Creates a new empty in-memory repository.
    pub fn new() -> Self {
        Self {
            notes: RwLock::new(HashMap::new()),
        }
    }
}

impl Default for InMemoryNoteRepository {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl NoteRepository for InMemoryNoteRepository {
    async fn create(&self, note: Note) -> Result<Note, NoteError> {
        let mut notes = self.notes.write().map_err(|e| {
            NoteError::Internal(format!("Lock poisoned: {}", e))
        })?;
        notes.insert(note.id, note.clone());
        Ok(note)
    }

    async fn get_by_id(&self, id: Uuid) -> Result<Note, NoteError> {
        let notes = self.notes.read().map_err(|e| {
            NoteError::Internal(format!("Lock poisoned: {}", e))
        })?;
        notes.get(&id).cloned().ok_or(NoteError::NotFound(id))
    }

    async fn list(&self) -> Result<Vec<Note>, NoteError> {
        let notes = self.notes.read().map_err(|e| {
            NoteError::Internal(format!("Lock poisoned: {}", e))
        })?;
        Ok(notes.values().cloned().collect())
    }

    async fn update(&self, id: Uuid, note: Note) -> Result<Note, NoteError> {
        let mut notes = self.notes.write().map_err(|e| {
            NoteError::Internal(format!("Lock poisoned: {}", e))
        })?;

        if !notes.contains_key(&id) {
            return Err(NoteError::NotFound(id));
        }

        notes.insert(id, note.clone());
        Ok(note)
    }

    async fn delete(&self, id: Uuid) -> Result<(), NoteError> {
        let mut notes = self.notes.write().map_err(|e| {
            NoteError::Internal(format!("Lock poisoned: {}", e))
        })?;

        if notes.remove(&id).is_none() {
            return Err(NoteError::NotFound(id));
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn should_create_note_successfully() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string());
        let id = note.id;

        let result = repo.create(note).await;

        assert!(result.is_ok());
        let created = result.unwrap();
        assert_eq!(created.id, id);
        assert_eq!(created.title, "Test");
    }

    #[tokio::test]
    async fn should_get_note_by_id() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string());
        let id = note.id;
        repo.create(note).await.unwrap();

        let result = repo.get_by_id(id).await;

        assert!(result.is_ok());
        assert_eq!(result.unwrap().id, id);
    }

    #[tokio::test]
    async fn should_return_error_when_note_not_found() {
        let repo = InMemoryNoteRepository::new();
        let id = Uuid::new_v4();

        let result = repo.get_by_id(id).await;

        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), NoteError::NotFound(_)));
    }

    #[tokio::test]
    async fn should_list_all_notes() {
        let repo = InMemoryNoteRepository::new();
        repo.create(Note::new("Note 1".to_string(), "Content 1".to_string()))
            .await
            .unwrap();
        repo.create(Note::new("Note 2".to_string(), "Content 2".to_string()))
            .await
            .unwrap();

        let result = repo.list().await;

        assert!(result.is_ok());
        assert_eq!(result.unwrap().len(), 2);
    }

    #[tokio::test]
    async fn should_update_note_successfully() {
        let repo = InMemoryNoteRepository::new();
        let mut note = Note::new("Original".to_string(), "Content".to_string());
        let id = note.id;
        repo.create(note.clone()).await.unwrap();

        note.update(Some("Updated".to_string()), None);
        let result = repo.update(id, note).await;

        assert!(result.is_ok());
        assert_eq!(result.unwrap().title, "Updated");
    }

    #[tokio::test]
    async fn should_fail_update_when_note_not_found() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string());
        let fake_id = Uuid::new_v4();

        let result = repo.update(fake_id, note).await;

        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), NoteError::NotFound(_)));
    }

    #[tokio::test]
    async fn should_delete_note_successfully() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string());
        let id = note.id;
        repo.create(note).await.unwrap();

        let result = repo.delete(id).await;

        assert!(result.is_ok());
        assert!(repo.get_by_id(id).await.is_err());
    }

    #[tokio::test]
    async fn should_fail_delete_when_note_not_found() {
        let repo = InMemoryNoteRepository::new();
        let id = Uuid::new_v4();

        let result = repo.delete(id).await;

        assert!(result.is_err());
        assert!(matches!(result.unwrap_err(), NoteError::NotFound(_)));
    }
}
