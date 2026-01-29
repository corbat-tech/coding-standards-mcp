//! Integration tests for the Notes API.

use axum::{
    body::Body,
    http::{Request, StatusCode},
};
use http_body_util::BodyExt;
use serde_json::{json, Value};
use tower::ServiceExt;

use notes_api::{create_router, domain::Note, repository::InMemoryNoteRepository};

fn create_test_app() -> axum::Router {
    let repo = InMemoryNoteRepository::new();
    create_router(repo)
}

#[tokio::test]
async fn test_create_note_returns_201() {
    let app = create_test_app();

    let response = app
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/notes")
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({"title": "Test Note", "content": "Test content"}).to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::CREATED);

    let body = response.into_body().collect().await.unwrap().to_bytes();
    let note: Note = serde_json::from_slice(&body).unwrap();
    assert_eq!(note.title, "Test Note");
    assert_eq!(note.content, "Test content");
}

#[tokio::test]
async fn test_create_note_with_empty_title_returns_400() {
    let app = create_test_app();

    let response = app
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/notes")
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({"title": "  ", "content": "Test content"}).to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn test_get_note_not_found_returns_404() {
    let app = create_test_app();
    let fake_id = uuid::Uuid::new_v4();

    let response = app
        .oneshot(
            Request::builder()
                .method("GET")
                .uri(format!("/notes/{}", fake_id))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn test_list_notes_returns_empty_array() {
    let app = create_test_app();

    let response = app
        .oneshot(
            Request::builder()
                .method("GET")
                .uri("/notes")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::OK);

    let body = response.into_body().collect().await.unwrap().to_bytes();
    let notes: Vec<Value> = serde_json::from_slice(&body).unwrap();
    assert!(notes.is_empty());
}

#[tokio::test]
async fn test_delete_note_not_found_returns_404() {
    let app = create_test_app();
    let fake_id = uuid::Uuid::new_v4();

    let response = app
        .oneshot(
            Request::builder()
                .method("DELETE")
                .uri(format!("/notes/{}", fake_id))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn test_update_note_not_found_returns_404() {
    let app = create_test_app();
    let fake_id = uuid::Uuid::new_v4();

    let response = app
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri(format!("/notes/{}", fake_id))
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({"title": "Updated"}).to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::NOT_FOUND);
}
