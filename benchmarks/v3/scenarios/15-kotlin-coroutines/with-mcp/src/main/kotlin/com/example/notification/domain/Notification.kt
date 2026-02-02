package com.example.notification.domain

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val recipient: String,
    val subject: String,
    val message: String,
    val channel: NotificationChannel,
    val status: NotificationStatus = NotificationStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null
)

enum class NotificationChannel {
    EMAIL, SMS, PUSH
}

enum class NotificationStatus {
    PENDING, SENT, FAILED
}

data class SendNotificationRequest(
    val recipient: String,
    val subject: String,
    val message: String,
    val channels: List<NotificationChannel>
)
