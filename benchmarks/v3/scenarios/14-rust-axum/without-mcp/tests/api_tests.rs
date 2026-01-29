use axum::{
    body::Body,
    http::{Request, StatusCode},
};
use http_body_util::BodyExt;
use notes_api::{create_app, model::Note, repository::InMemoryNoteRepository};
use serde_json::{json, Value};
use tower::ServiceExt;

fn setup_app() -> axum::Router {
    let repo = InMemoryNoteRepository::new();
    create_app(repo)
}

async fn body_to_json(body: Body) -> Value {
    let bytes = body.collect().await.unwrap().to_bytes();
    serde_json::from_slice(&bytes).unwrap()
}

#[tokio::test]
async fn test_health_check() {
    let app = setup_app();

    let response = app
        .oneshot(Request::builder().uri("/health").body(Body::empty()).unwrap())
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::OK);
}

#[tokio::test]
async fn test_create_note() {
    let app = setup_app();

    let response = app
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/notes")
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({
                        "title": "Test Note",
                        "content": "Test Content"
                    })
                    .to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::CREATED);

    let body = body_to_json(response.into_body()).await;
    assert_eq!(body["title"], "Test Note");
    assert_eq!(body["content"], "Test Content");
    assert!(body["id"].is_string());
}

#[tokio::test]
async fn test_create_note_empty_title() {
    let app = setup_app();

    let response = app
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/notes")
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({
                        "title": "   ",
                        "content": "Test Content"
                    })
                    .to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::BAD_REQUEST);
}

#[tokio::test]
async fn test_get_note() {
    let repo = InMemoryNoteRepository::new();
    let note = Note::new("Test".to_string(), "Content".to_string());
    let note_id = note.id;
    repo.create(note).await.unwrap();

    let app = create_app(repo);

    let response = app
        .oneshot(
            Request::builder()
                .uri(format!("/notes/{}", note_id))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::OK);

    let body = body_to_json(response.into_body()).await;
    assert_eq!(body["title"], "Test");
    assert_eq!(body["content"], "Content");
}

#[tokio::test]
async fn test_get_note_not_found() {
    let app = setup_app();

    let response = app
        .oneshot(
            Request::builder()
                .uri("/notes/00000000-0000-0000-0000-000000000000")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn test_list_notes() {
    let repo = InMemoryNoteRepository::new();
    repo.create(Note::new("Note 1".to_string(), "Content 1".to_string()))
        .await
        .unwrap();
    repo.create(Note::new("Note 2".to_string(), "Content 2".to_string()))
        .await
        .unwrap();

    let app = create_app(repo);

    let response = app
        .oneshot(Request::builder().uri("/notes").body(Body::empty()).unwrap())
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::OK);

    let body = body_to_json(response.into_body()).await;
    assert!(body.is_array());
    assert_eq!(body.as_array().unwrap().len(), 2);
}

#[tokio::test]
async fn test_update_note() {
    let repo = InMemoryNoteRepository::new();
    let note = Note::new("Original".to_string(), "Content".to_string());
    let note_id = note.id;
    repo.create(note).await.unwrap();

    let app = create_app(repo);

    let response = app
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri(format!("/notes/{}", note_id))
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({
                        "title": "Updated Title",
                        "content": "Updated Content"
                    })
                    .to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::OK);

    let body = body_to_json(response.into_body()).await;
    assert_eq!(body["title"], "Updated Title");
    assert_eq!(body["content"], "Updated Content");
}

#[tokio::test]
async fn test_update_note_partial() {
    let repo = InMemoryNoteRepository::new();
    let note = Note::new("Original".to_string(), "Content".to_string());
    let note_id = note.id;
    repo.create(note).await.unwrap();

    let app = create_app(repo);

    let response = app
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri(format!("/notes/{}", note_id))
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({
                        "title": "Updated Title"
                    })
                    .to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::OK);

    let body = body_to_json(response.into_body()).await;
    assert_eq!(body["title"], "Updated Title");
    assert_eq!(body["content"], "Content");
}

#[tokio::test]
async fn test_update_note_not_found() {
    let app = setup_app();

    let response = app
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri("/notes/00000000-0000-0000-0000-000000000000")
                .header("Content-Type", "application/json")
                .body(Body::from(
                    json!({
                        "title": "Updated"
                    })
                    .to_string(),
                ))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::NOT_FOUND);
}

#[tokio::test]
async fn test_delete_note() {
    let repo = InMemoryNoteRepository::new();
    let note = Note::new("Test".to_string(), "Content".to_string());
    let note_id = note.id;
    repo.create(note).await.unwrap();

    let app = create_app(repo.clone());

    let response = app
        .oneshot(
            Request::builder()
                .method("DELETE")
                .uri(format!("/notes/{}", note_id))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::NO_CONTENT);

    // Verify note is deleted
    use notes_api::repository::NoteRepository;
    let result = repo.get_by_id(note_id).await;
    assert!(result.is_err());
}

#[tokio::test]
async fn test_delete_note_not_found() {
    let app = setup_app();

    let response = app
        .oneshot(
            Request::builder()
                .method("DELETE")
                .uri("/notes/00000000-0000-0000-0000-000000000000")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::NOT_FOUND);
}
