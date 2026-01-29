//! Domain layer containing entities and repository traits.

mod note;
mod repository;

pub use note::{Note, CreateNoteRequest, UpdateNoteRequest};
pub use repository::NoteRepository;
