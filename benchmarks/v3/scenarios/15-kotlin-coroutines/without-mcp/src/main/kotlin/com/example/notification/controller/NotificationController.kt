package com.example.notification.controller

import com.example.notification.domain.*
import com.example.notification.service.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * REST controller for notification operations.
 * Uses suspend functions for async request handling.
 */
@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    /**
     * Send a single notification
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun sendNotification(@RequestBody request: NotificationRequestDto): NotificationResultDto {
        val domainRequest = request.toDomain()
        val result = notificationService.sendNotification(domainRequest)
        return result.toDto()
    }

    /**
     * Send a batch of notifications
     */
    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun sendBatchNotifications(@RequestBody request: BatchNotificationRequestDto): BatchNotificationResultDto {
        val domainRequest = BatchNotificationRequest(
            notifications = request.notifications.map { it.toDomain() },
            failFast = request.failFast
        )
        val result = notificationService.sendBatch(domainRequest)
        return result.toDto()
    }

    /**
     * Broadcast a message to multiple recipients
     */
    @PostMapping("/broadcast")
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun broadcastNotification(@RequestBody request: BroadcastRequestDto): List<NotificationResultDto> {
        val results = notificationService.broadcast(
            recipients = request.recipients,
            subject = request.subject,
            message = request.message,
            channel = NotificationChannel.valueOf(request.channel),
            priority = NotificationPriority.valueOf(request.priority)
        )
        return results.map { it.toDto() }
    }

    /**
     * Stream notifications using Server-Sent Events
     */
    @PostMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamNotifications(@RequestBody requests: List<NotificationRequestDto>): Flow<NotificationResultDto> {
        return notificationService.sendNotificationsAsFlow(
            requests.map { it.toDomain() }.asFlow()
        ).let { flow ->
            kotlinx.coroutines.flow.map(flow) { it.toDto() }
        }
    }

    /**
     * Get channel availability status
     */
    @GetMapping("/channels/status")
    suspend fun getChannelStatus(): Map<String, Boolean> {
        return notificationService.getChannelStatus().mapKeys { it.key.name }
    }

    /**
     * Validate a recipient for a specific channel
     */
    @GetMapping("/validate")
    suspend fun validateRecipient(
        @RequestParam recipient: String,
        @RequestParam channel: String
    ): ValidationResultDto {
        val valid = notificationService.validateRecipient(
            recipient,
            NotificationChannel.valueOf(channel)
        )
        return ValidationResultDto(recipient, channel, valid)
    }
}

// DTOs

data class NotificationRequestDto(
    val recipient: String,
    val subject: String,
    val message: String,
    val channel: String,
    val priority: String = "NORMAL",
    val metadata: Map<String, String> = emptyMap()
) {
    fun toDomain() = NotificationRequest(
        recipient = recipient,
        subject = subject,
        message = message,
        channel = NotificationChannel.valueOf(channel),
        priority = NotificationPriority.valueOf(priority),
        metadata = metadata
    )
}

data class NotificationResultDto(
    val requestId: String,
    val channel: String,
    val status: String,
    val message: String?,
    val deliveredAt: String?,
    val errorDetails: String?
)

fun NotificationResult.toDto() = NotificationResultDto(
    requestId = requestId,
    channel = channel.name,
    status = status.name,
    message = message,
    deliveredAt = deliveredAt?.toString(),
    errorDetails = errorDetails
)

data class BatchNotificationRequestDto(
    val notifications: List<NotificationRequestDto>,
    val failFast: Boolean = false
)

data class BatchNotificationResultDto(
    val batchId: String,
    val results: List<NotificationResultDto>,
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val completedAt: String
)

fun BatchNotificationResult.toDto() = BatchNotificationResultDto(
    batchId = batchId,
    results = results.map { it.toDto() },
    totalCount = totalCount,
    successCount = successCount,
    failureCount = failureCount,
    completedAt = completedAt.toString()
)

data class BroadcastRequestDto(
    val recipients: List<String>,
    val subject: String,
    val message: String,
    val channel: String,
    val priority: String = "NORMAL"
)

data class ValidationResultDto(
    val recipient: String,
    val channel: String,
    val valid: Boolean
)
