from typing import List
from ..domain.user import User, UserCreate, UserUpdate
from ..domain.repository import UserRepository
from ..domain.exceptions import UserNotFoundException, UserAlreadyExistsException


class UserService:
    def __init__(self, repository: UserRepository):
        self.repository = repository

    async def create_user(self, data: UserCreate) -> User:
        existing = await self.repository.get_by_email(data.email)
        if existing:
            raise UserAlreadyExistsException(data.email)
        return await self.repository.create(data)

    async def get_user(self, user_id: int) -> User:
        user = await self.repository.get_by_id(user_id)
        if not user:
            raise UserNotFoundException(user_id)
        return user

    async def get_all_users(self) -> List[User]:
        return await self.repository.get_all()

    async def update_user(self, user_id: int, data: UserUpdate) -> User:
        if data.email:
            existing = await self.repository.get_by_email(data.email)
            if existing and existing.id != user_id:
                raise UserAlreadyExistsException(data.email)

        user = await self.repository.update(user_id, data)
        if not user:
            raise UserNotFoundException(user_id)
        return user

    async def delete_user(self, user_id: int) -> None:
        deleted = await self.repository.delete(user_id)
        if not deleted:
            raise UserNotFoundException(user_id)
