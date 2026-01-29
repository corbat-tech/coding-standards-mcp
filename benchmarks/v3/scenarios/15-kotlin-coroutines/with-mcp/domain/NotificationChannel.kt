package com.notification.domain

/**
 * Interface for notification channel implementations.
 * Each channel (Email, SMS, Push) implements this interface.
 */
interface NotificationChannel {
    /**
     * The type of channel this implementation handles.
     */
    val channelType: ChannelType

    /**
     * Send a notification through this channel.
     * This is a suspend function for async execution.
     *
     * @param notification The notification to send
     * @return NotificationResult indicating success or failure
     */
    suspend fun send(notification: Notification): NotificationResult

    /**
     * Validate if the recipient is valid for this channel.
     *
     * @param recipient The recipient address/identifier
     * @return true if valid, false otherwise
     */
    fun validateRecipient(recipient: String): Boolean

    /**
     * Check if the channel is currently available.
     *
     * @return true if available, false otherwise
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Interface for channel health monitoring.
 */
interface ChannelHealthMonitor {
    /**
     * Get the current health status of a channel.
     */
    suspend fun getHealth(channelType: ChannelType): ChannelHealth
}

/**
 * Represents the health status of a channel.
 */
data class ChannelHealth(
    val channelType: ChannelType,
    val isHealthy: Boolean,
    val successRate: Double,
    val averageLatencyMs: Long,
    val lastChecked: java.time.Instant = java.time.Instant.now()
)
