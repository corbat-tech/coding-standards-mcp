"""
FastAPI Application Entry Point.

Task Management API with Clean Architecture.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from core.config import get_settings
from core.database import create_tables
from api.task_routes import router as task_router

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Task Management API with FastAPI and SQLAlchemy"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(task_router)


@app.get("/health", tags=["health"])
def health_check() -> dict:
    """Health check endpoint."""
    return {"status": "healthy", "version": settings.app_version}


@app.on_event("startup")
def startup_event() -> None:
    """Initialize database on startup."""
    create_tables()
