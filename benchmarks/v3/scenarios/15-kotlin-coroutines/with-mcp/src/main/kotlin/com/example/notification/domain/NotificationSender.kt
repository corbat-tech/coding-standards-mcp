package com.example.notification.domain

interface NotificationSender {
    val channel: NotificationChannel
    suspend fun send(notification: Notification): Result<Notification>
}

class NotificationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
