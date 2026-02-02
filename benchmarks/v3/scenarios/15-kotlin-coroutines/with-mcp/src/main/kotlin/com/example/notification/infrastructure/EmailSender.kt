package com.example.notification.infrastructure

import com.example.notification.domain.Notification
import com.example.notification.domain.NotificationChannel
import com.example.notification.domain.NotificationSender
import com.example.notification.domain.NotificationStatus
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EmailSender : NotificationSender {
    override val channel = NotificationChannel.EMAIL

    override suspend fun send(notification: Notification): Result<Notification> {
        return try {
            delay(100) // Simulate email sending
            println("Email sent to ${notification.recipient}: ${notification.subject}")
            Result.success(notification.copy(status = NotificationStatus.SENT, sentAt = Instant.now()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
