package com.notification.domain

import java.time.Instant
import java.util.UUID

/**
 * Represents the different notification channels available.
 */
enum class ChannelType {
    EMAIL,
    SMS,
    PUSH
}

/**
 * Represents the status of a notification.
 */
enum class NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    DELIVERED
}

/**
 * Priority levels for notifications.
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Domain entity representing a notification.
 */
data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val recipient: String,
    val subject: String,
    val content: String,
    val channelType: ChannelType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val status: NotificationStatus = NotificationStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val sentAt: Instant? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(recipient.isNotBlank()) { "Recipient cannot be blank" }
        require(content.isNotBlank()) { "Content cannot be blank" }
    }

    fun markAsSent(): Notification = copy(
        status = NotificationStatus.SENT,
        sentAt = Instant.now()
    )

    fun markAsFailed(): Notification = copy(
        status = NotificationStatus.FAILED
    )

    fun markAsDelivered(): Notification = copy(
        status = NotificationStatus.DELIVERED
    )
}

/**
 * Request object for creating notifications.
 */
data class NotificationRequest(
    val recipient: String,
    val subject: String,
    val content: String,
    val channels: Set<ChannelType>,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(recipient.isNotBlank()) { "Recipient cannot be blank" }
        require(content.isNotBlank()) { "Content cannot be blank" }
        require(channels.isNotEmpty()) { "At least one channel must be specified" }
    }

    fun toNotification(channelType: ChannelType): Notification = Notification(
        recipient = recipient,
        subject = subject,
        content = content,
        channelType = channelType,
        priority = priority,
        metadata = metadata
    )
}

/**
 * Result of a notification send operation.
 */
sealed class NotificationResult {
    data class Success(
        val notificationId: String,
        val channelType: ChannelType,
        val message: String = "Notification sent successfully"
    ) : NotificationResult()

    data class Failure(
        val notificationId: String,
        val channelType: ChannelType,
        val error: NotificationError
    ) : NotificationResult()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
}

/**
 * Aggregated result for multi-channel notifications.
 */
data class MultiChannelResult(
    val requestId: String = UUID.randomUUID().toString(),
    val results: List<NotificationResult>
) {
    val successCount: Int get() = results.count { it.isSuccess() }
    val failureCount: Int get() = results.count { it.isFailure() }
    val isFullySuccessful: Boolean get() = results.all { it.isSuccess() }
    val isPartiallySuccessful: Boolean get() = successCount > 0 && failureCount > 0
    val isFullyFailed: Boolean get() = results.all { it.isFailure() }
}
