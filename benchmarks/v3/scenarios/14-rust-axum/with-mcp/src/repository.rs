use crate::error::AppError;
use crate::model::Note;
use std::collections::HashMap;
use std::sync::RwLock;
use uuid::Uuid;

pub trait NoteRepository: Send + Sync {
    fn create(&self, note: Note) -> Result<Note, AppError>;
    fn get_by_id(&self, id: Uuid) -> Result<Note, AppError>;
    fn get_all(&self) -> Result<Vec<Note>, AppError>;
    fn update(&self, id: Uuid, note: Note) -> Result<Note, AppError>;
    fn delete(&self, id: Uuid) -> Result<(), AppError>;
}

pub struct InMemoryNoteRepository {
    notes: RwLock<HashMap<Uuid, Note>>,
}

impl InMemoryNoteRepository {
    pub fn new() -> Self {
        Self {
            notes: RwLock::new(HashMap::new()),
        }
    }
}

impl NoteRepository for InMemoryNoteRepository {
    fn create(&self, note: Note) -> Result<Note, AppError> {
        let mut notes = self.notes.write().map_err(|_| AppError::Internal)?;
        notes.insert(note.id, note.clone());
        Ok(note)
    }

    fn get_by_id(&self, id: Uuid) -> Result<Note, AppError> {
        let notes = self.notes.read().map_err(|_| AppError::Internal)?;
        notes.get(&id).cloned().ok_or(AppError::NotFound)
    }

    fn get_all(&self) -> Result<Vec<Note>, AppError> {
        let notes = self.notes.read().map_err(|_| AppError::Internal)?;
        Ok(notes.values().cloned().collect())
    }

    fn update(&self, id: Uuid, note: Note) -> Result<Note, AppError> {
        let mut notes = self.notes.write().map_err(|_| AppError::Internal)?;
        if !notes.contains_key(&id) {
            return Err(AppError::NotFound);
        }
        notes.insert(id, note.clone());
        Ok(note)
    }

    fn delete(&self, id: Uuid) -> Result<(), AppError> {
        let mut notes = self.notes.write().map_err(|_| AppError::Internal)?;
        if notes.remove(&id).is_none() {
            return Err(AppError::NotFound);
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_and_get_note() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Test".to_string(), "Content".to_string(), vec![]);

        let created = repo.create(note.clone()).unwrap();
        let retrieved = repo.get_by_id(created.id).unwrap();

        assert_eq!(retrieved.title, "Test");
    }

    #[test]
    fn test_get_not_found() {
        let repo = InMemoryNoteRepository::new();
        let result = repo.get_by_id(Uuid::new_v4());

        assert!(matches!(result, Err(AppError::NotFound)));
    }

    #[test]
    fn test_delete_note() {
        let repo = InMemoryNoteRepository::new();
        let note = Note::new("Delete Me".to_string(), "Content".to_string(), vec![]);
        let created = repo.create(note).unwrap();

        repo.delete(created.id).unwrap();
        let result = repo.get_by_id(created.id);

        assert!(matches!(result, Err(AppError::NotFound)));
    }
}
