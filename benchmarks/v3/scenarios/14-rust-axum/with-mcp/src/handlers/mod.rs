//! HTTP handlers for the Notes API.

mod notes;

pub use notes::{create_note, get_note, list_notes, update_note, delete_note};
