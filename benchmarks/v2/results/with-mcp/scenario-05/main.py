from contextlib import asynccontextmanager

from fastapi import FastAPI

from .api.routes import router
from .infrastructure.database import init_db


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield


app = FastAPI(
    title="Task Management API",
    version="1.0.0",
    lifespan=lifespan
)

app.include_router(router)


@app.get("/health")
async def health():
    return {"status": "ok"}
