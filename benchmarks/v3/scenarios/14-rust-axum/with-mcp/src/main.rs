//! Notes API entry point.

use notes_api::{create_router, repository::InMemoryNoteRepository};
use tokio::net::TcpListener;

#[tokio::main]
async fn main() {
    let repo = InMemoryNoteRepository::new();
    let app = create_router(repo);

    let listener = TcpListener::bind("0.0.0.0:3000").await.unwrap();
    println!("Notes API listening on http://localhost:3000");

    axum::serve(listener, app).await.unwrap();
}
