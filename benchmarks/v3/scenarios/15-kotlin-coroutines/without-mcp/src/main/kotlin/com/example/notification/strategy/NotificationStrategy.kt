package com.example.notification.strategy

import com.example.notification.domain.NotificationChannel
import com.example.notification.domain.NotificationRequest
import com.example.notification.domain.NotificationResult

/**
 * Strategy interface for sending notifications through different channels.
 * Implementations should use suspend functions for async operations.
 */
interface NotificationStrategy {
    /**
     * The channel this strategy handles
     */
    val channel: NotificationChannel

    /**
     * Sends a notification asynchronously
     * @param request The notification request to send
     * @return The result of the notification attempt
     */
    suspend fun send(request: NotificationRequest): NotificationResult

    /**
     * Validates the recipient for this channel
     * @param recipient The recipient to validate
     * @return true if valid, false otherwise
     */
    suspend fun validateRecipient(recipient: String): Boolean

    /**
     * Checks if this channel is currently available
     * @return true if available, false otherwise
     */
    suspend fun isAvailable(): Boolean
}
