from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from datetime import datetime

from ..domain.user import User, UserCreate, UserUpdate
from ..domain.repository import UserRepository
from .database import UserModel


class SQLAlchemyUserRepository(UserRepository):
    def __init__(self, session: AsyncSession):
        self.session = session

    async def create(self, data: UserCreate) -> User:
        user = UserModel(
            email=data.email,
            name=data.name,
            password_hash=self._hash_password(data.password),
            role=data.role.value
        )
        self.session.add(user)
        await self.session.commit()
        await self.session.refresh(user)
        return self._to_domain(user)

    async def get_by_id(self, user_id: int) -> Optional[User]:
        result = await self.session.execute(
            select(UserModel).where(UserModel.id == user_id)
        )
        user = result.scalar_one_or_none()
        return self._to_domain(user) if user else None

    async def get_by_email(self, email: str) -> Optional[User]:
        result = await self.session.execute(
            select(UserModel).where(UserModel.email == email)
        )
        user = result.scalar_one_or_none()
        return self._to_domain(user) if user else None

    async def get_all(self) -> List[User]:
        result = await self.session.execute(select(UserModel))
        return [self._to_domain(u) for u in result.scalars().all()]

    async def update(self, user_id: int, data: UserUpdate) -> Optional[User]:
        result = await self.session.execute(
            select(UserModel).where(UserModel.id == user_id)
        )
        user = result.scalar_one_or_none()
        if not user:
            return None

        update_data = data.model_dump(exclude_unset=True)
        if "role" in update_data and update_data["role"]:
            update_data["role"] = update_data["role"].value

        for field, value in update_data.items():
            setattr(user, field, value)

        user.updated_at = datetime.utcnow()
        await self.session.commit()
        await self.session.refresh(user)
        return self._to_domain(user)

    async def delete(self, user_id: int) -> bool:
        result = await self.session.execute(
            select(UserModel).where(UserModel.id == user_id)
        )
        user = result.scalar_one_or_none()
        if not user:
            return False
        await self.session.delete(user)
        await self.session.commit()
        return True

    def _to_domain(self, model: UserModel) -> User:
        return User(
            id=model.id,
            email=model.email,
            name=model.name,
            role=model.role,
            created_at=model.created_at,
            updated_at=model.updated_at
        )

    def _hash_password(self, password: str) -> str:
        import hashlib
        return hashlib.sha256(password.encode()).hexdigest()
