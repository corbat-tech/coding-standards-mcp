from fastapi import FastAPI, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from typing import List
from contextlib import asynccontextmanager

from .domain.user import User, UserCreate, UserUpdate
from .domain.exceptions import UserNotFoundException, UserAlreadyExistsException
from .application.user_service import UserService
from .infrastructure.database import create_tables, get_session
from .infrastructure.user_repository import SQLAlchemyUserRepository


@asynccontextmanager
async def lifespan(app: FastAPI):
    await create_tables()
    yield


app = FastAPI(title="User API", lifespan=lifespan)


def get_user_service(session: AsyncSession = Depends(get_session)) -> UserService:
    repository = SQLAlchemyUserRepository(session)
    return UserService(repository)


@app.post("/users", response_model=User, status_code=status.HTTP_201_CREATED)
async def create_user(data: UserCreate, service: UserService = Depends(get_user_service)):
    try:
        return await service.create_user(data)
    except UserAlreadyExistsException as e:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(e))


@app.get("/users", response_model=List[User])
async def get_users(service: UserService = Depends(get_user_service)):
    return await service.get_all_users()


@app.get("/users/{user_id}", response_model=User)
async def get_user(user_id: int, service: UserService = Depends(get_user_service)):
    try:
        return await service.get_user(user_id)
    except UserNotFoundException as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@app.put("/users/{user_id}", response_model=User)
async def update_user(user_id: int, data: UserUpdate, service: UserService = Depends(get_user_service)):
    try:
        return await service.update_user(user_id, data)
    except UserNotFoundException as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
    except UserAlreadyExistsException as e:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(e))


@app.delete("/users/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_user(user_id: int, service: UserService = Depends(get_user_service)):
    try:
        await service.delete_user(user_id)
    except UserNotFoundException as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
