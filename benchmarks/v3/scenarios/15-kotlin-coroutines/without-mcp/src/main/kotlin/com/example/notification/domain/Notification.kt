package com.example.notification.domain

import java.time.Instant
import java.util.UUID

/**
 * Represents the type of notification channel
 */
enum class NotificationChannel {
    EMAIL,
    SMS,
    PUSH
}

/**
 * Represents the status of a notification
 */
enum class NotificationStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    DELIVERED
}

/**
 * Represents a notification priority
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Domain model representing a notification request
 */
data class NotificationRequest(
    val id: String = UUID.randomUUID().toString(),
    val recipient: String,
    val subject: String,
    val message: String,
    val channel: NotificationChannel,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.now()
)

/**
 * Domain model representing the result of sending a notification
 */
data class NotificationResult(
    val requestId: String,
    val channel: NotificationChannel,
    val status: NotificationStatus,
    val message: String? = null,
    val deliveredAt: Instant? = null,
    val errorDetails: String? = null
)

/**
 * Domain model for batch notification requests
 */
data class BatchNotificationRequest(
    val id: String = UUID.randomUUID().toString(),
    val notifications: List<NotificationRequest>,
    val failFast: Boolean = false
)

/**
 * Domain model for batch notification results
 */
data class BatchNotificationResult(
    val batchId: String,
    val results: List<NotificationResult>,
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val completedAt: Instant = Instant.now()
)
