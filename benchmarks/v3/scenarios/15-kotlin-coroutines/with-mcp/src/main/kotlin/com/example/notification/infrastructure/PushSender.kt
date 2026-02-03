package com.example.notification.infrastructure

import com.example.notification.domain.Notification
import com.example.notification.domain.NotificationChannel
import com.example.notification.domain.NotificationSender
import com.example.notification.domain.NotificationStatus
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PushSender : NotificationSender {
    override val channel = NotificationChannel.PUSH

    override suspend fun send(notification: Notification): Result<Notification> {
        return try {
            delay(30) // Simulate push notification
            println("Push notification sent to ${notification.recipient}: ${notification.subject}")
            Result.success(notification.copy(status = NotificationStatus.SENT, sentAt = Instant.now()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
