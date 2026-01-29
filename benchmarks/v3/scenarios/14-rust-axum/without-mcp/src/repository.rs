use crate::error::{NoteError, Result};
use crate::model::Note;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use uuid::Uuid;

pub trait NoteRepository: Send + Sync {
    fn create(&self, note: Note) -> impl std::future::Future<Output = Result<Note>> + Send;
    fn get_by_id(&self, id: Uuid) -> impl std::future::Future<Output = Result<Note>> + Send;
    fn get_all(&self) -> impl std::future::Future<Output = Result<Vec<Note>>> + Send;
    fn update(&self, note: Note) -> impl std::future::Future<Output = Result<Note>> + Send;
    fn delete(&self, id: Uuid) -> impl std::future::Future<Output = Result<()>> + Send;
}

#[derive(Debug, Clone, Default)]
pub struct InMemoryNoteRepository {
    notes: Arc<RwLock<HashMap<Uuid, Note>>>,
}

impl InMemoryNoteRepository {
    pub fn new() -> Self {
        Self {
            notes: Arc::new(RwLock::new(HashMap::new())),
        }
    }
}

impl NoteRepository for InMemoryNoteRepository {
    async fn create(&self, note: Note) -> Result<Note> {
        let mut notes = self.notes.write().await;
        notes.insert(note.id, note.clone());
        Ok(note)
    }

    async fn get_by_id(&self, id: Uuid) -> Result<Note> {
        let notes = self.notes.read().await;
        notes
            .get(&id)
            .cloned()
            .ok_or(NoteError::NotFound(id))
    }

    async fn get_all(&self) -> Result<Vec<Note>> {
        let notes = self.notes.read().await;
        let mut result: Vec<Note> = notes.values().cloned().collect();
        result.sort_by(|a, b| b.created_at.cmp(&a.created_at));
        Ok(result)
    }

    async fn update(&self, note: Note) -> Result<Note> {
        let mut notes = self.notes.write().await;
        if !notes.contains_key(&note.id) {
            return Err(NoteError::NotFound(note.id));
        }
        notes.insert(note.id, note.clone());
        Ok(note)
    }

    async fn delete(&self, id: Uuid) -> Result<()> {
        let mut notes = self.notes.write().await;
        notes
            .remove(&id)
            .ok_or(NoteError::NotFound(id))?;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_create_and_get_note() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string());
        let id = note.id;

        let created = repo.create(note).await.unwrap();
        assert_eq!(created.title, "Test");

        let fetched = repo.get_by_id(id).await.unwrap();
        assert_eq!(fetched.id, id);
        assert_eq!(fetched.title, "Test");
    }

    #[tokio::test]
    async fn test_get_nonexistent_note() {
        let repo = InMemoryNoteRepository::new();
        let id = Uuid::new_v4();

        let result = repo.get_by_id(id).await;
        assert!(matches!(result, Err(NoteError::NotFound(_))));
    }

    #[tokio::test]
    async fn test_get_all_notes() {
        let repo = InMemoryNoteRepository::new();

        repo.create(Note::new("Note 1".to_string(), "Content 1".to_string()))
            .await
            .unwrap();
        repo.create(Note::new("Note 2".to_string(), "Content 2".to_string()))
            .await
            .unwrap();

        let notes = repo.get_all().await.unwrap();
        assert_eq!(notes.len(), 2);
    }

    #[tokio::test]
    async fn test_update_note() {
        let repo = InMemoryNoteRepository::new();
        let mut note = Note::new("Original".to_string(), "Content".to_string());
        let id = note.id;

        repo.create(note.clone()).await.unwrap();

        note.title = "Updated".to_string();
        let updated = repo.update(note).await.unwrap();

        assert_eq!(updated.title, "Updated");

        let fetched = repo.get_by_id(id).await.unwrap();
        assert_eq!(fetched.title, "Updated");
    }

    #[tokio::test]
    async fn test_update_nonexistent_note() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string());

        let result = repo.update(note).await;
        assert!(matches!(result, Err(NoteError::NotFound(_))));
    }

    #[tokio::test]
    async fn test_delete_note() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string());
        let id = note.id;

        repo.create(note).await.unwrap();
        repo.delete(id).await.unwrap();

        let result = repo.get_by_id(id).await;
        assert!(matches!(result, Err(NoteError::NotFound(_))));
    }

    #[tokio::test]
    async fn test_delete_nonexistent_note() {
        let repo = InMemoryNoteRepository::new();
        let id = Uuid::new_v4();

        let result = repo.delete(id).await;
        assert!(matches!(result, Err(NoteError::NotFound(_))));
    }
}
