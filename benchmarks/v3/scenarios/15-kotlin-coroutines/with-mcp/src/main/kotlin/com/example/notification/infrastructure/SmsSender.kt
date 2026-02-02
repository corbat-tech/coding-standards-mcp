package com.example.notification.infrastructure

import com.example.notification.domain.Notification
import com.example.notification.domain.NotificationChannel
import com.example.notification.domain.NotificationSender
import com.example.notification.domain.NotificationStatus
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SmsSender : NotificationSender {
    override val channel = NotificationChannel.SMS

    override suspend fun send(notification: Notification): Result<Notification> {
        return try {
            delay(50) // Simulate SMS sending
            println("SMS sent to ${notification.recipient}: ${notification.message}")
            Result.success(notification.copy(status = NotificationStatus.SENT, sentAt = Instant.now()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
