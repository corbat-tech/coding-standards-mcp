package com.notification.infrastructure

import com.notification.domain.Notification
import com.notification.domain.NotificationRepository
import com.notification.domain.NotificationStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of NotificationRepository.
 * Useful for testing and development.
 */
@Repository
class InMemoryNotificationRepository : NotificationRepository {

    private val store = ConcurrentHashMap<String, Notification>()
    private val mutex = Mutex()

    override suspend fun save(notification: Notification): Notification {
        mutex.withLock {
            store[notification.id] = notification
        }
        return notification
    }

    override suspend fun findById(id: String): Notification? {
        return store[id]
    }

    override suspend fun findByRecipient(recipient: String): List<Notification> {
        return store.values.filter { it.recipient == recipient }
    }

    override suspend fun findByStatus(status: NotificationStatus): List<Notification> {
        return store.values.filter { it.status == status }
    }

    override suspend fun update(notification: Notification): Notification {
        mutex.withLock {
            if (!store.containsKey(notification.id)) {
                throw IllegalArgumentException(
                    "Notification with id ${notification.id} not found"
                )
            }
            store[notification.id] = notification
        }
        return notification
    }

    /**
     * Clear all notifications. Useful for testing.
     */
    suspend fun clear() {
        mutex.withLock {
            store.clear()
        }
    }

    /**
     * Get the count of stored notifications. Useful for testing.
     */
    fun count(): Int = store.size
}
