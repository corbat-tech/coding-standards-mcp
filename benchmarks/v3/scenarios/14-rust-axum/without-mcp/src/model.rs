use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Note {
    pub id: Uuid,
    pub title: String,
    pub content: String,
    pub created_at: i64,
    pub updated_at: i64,
}

impl Note {
    pub fn new(title: String, content: String) -> Self {
        let now = chrono_timestamp();
        Self {
            id: Uuid::new_v4(),
            title,
            content,
            created_at: now,
            updated_at: now,
        }
    }

    pub fn update(&mut self, title: Option<String>, content: Option<String>) {
        if let Some(t) = title {
            self.title = t;
        }
        if let Some(c) = content {
            self.content = c;
        }
        self.updated_at = chrono_timestamp();
    }
}

fn chrono_timestamp() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs() as i64
}

#[derive(Debug, Deserialize)]
pub struct CreateNoteRequest {
    pub title: String,
    pub content: String,
}

#[derive(Debug, Deserialize)]
pub struct UpdateNoteRequest {
    pub title: Option<String>,
    pub content: Option<String>,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_note_creation() {
        let note = Note::new("Test Title".to_string(), "Test Content".to_string());

        assert_eq!(note.title, "Test Title");
        assert_eq!(note.content, "Test Content");
        assert!(note.created_at > 0);
        assert_eq!(note.created_at, note.updated_at);
    }

    #[test]
    fn test_note_update() {
        let mut note = Note::new("Original".to_string(), "Content".to_string());
        let original_created = note.created_at;

        std::thread::sleep(std::time::Duration::from_millis(10));
        note.update(Some("Updated".to_string()), None);

        assert_eq!(note.title, "Updated");
        assert_eq!(note.content, "Content");
        assert_eq!(note.created_at, original_created);
    }
}
