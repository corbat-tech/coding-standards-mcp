"""
Pytest Configuration and Fixtures.

Provides shared fixtures for testing.
"""
import pytest
from datetime import datetime
from typing import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from fastapi.testclient import TestClient

from core.database import Base, get_db
from models.task import Task
from schemas.task_schema import TaskStatus, TaskPriority
from main import app

# Use in-memory SQLite for tests
TEST_DATABASE_URL = "sqlite:///:memory:"

engine = create_engine(
    TEST_DATABASE_URL,
    connect_args={"check_same_thread": False}
)

TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db_session() -> Generator[Session, None, None]:
    """Create a fresh database session for each test."""
    Base.metadata.create_all(bind=engine)
    session = TestSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="function")
def client(db_session: Session) -> Generator[TestClient, None, None]:
    """Create a test client with database dependency override."""

    def override_get_db() -> Generator[Session, None, None]:
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


@pytest.fixture
def sample_task(db_session: Session) -> Task:
    """Create a sample task in the database."""
    task = Task(
        title="Test Task",
        description="Test description",
        status=TaskStatus.PENDING,
        priority=TaskPriority.MEDIUM,
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow()
    )
    db_session.add(task)
    db_session.commit()
    db_session.refresh(task)
    return task


@pytest.fixture
def sample_tasks(db_session: Session) -> list[Task]:
    """Create multiple sample tasks."""
    tasks = []
    for i in range(5):
        task = Task(
            title=f"Task {i + 1}",
            description=f"Description {i + 1}",
            status=TaskStatus.PENDING if i % 2 == 0 else TaskStatus.COMPLETED,
            priority=TaskPriority.MEDIUM,
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        db_session.add(task)
        tasks.append(task)
    db_session.commit()
    for task in tasks:
        db_session.refresh(task)
    return tasks
