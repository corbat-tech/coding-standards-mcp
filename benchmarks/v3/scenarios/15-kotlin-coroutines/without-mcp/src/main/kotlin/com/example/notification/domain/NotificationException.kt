package com.example.notification.domain

/**
 * Base exception for notification-related errors
 */
sealed class NotificationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when a notification channel is not available
 */
class ChannelUnavailableException(
    val channel: NotificationChannel,
    message: String = "Channel $channel is currently unavailable",
    cause: Throwable? = null
) : NotificationException(message, cause)

/**
 * Exception thrown when notification delivery fails
 */
class DeliveryFailedException(
    val requestId: String,
    val channel: NotificationChannel,
    message: String = "Failed to deliver notification $requestId via $channel",
    cause: Throwable? = null
) : NotificationException(message, cause)

/**
 * Exception thrown when recipient validation fails
 */
class InvalidRecipientException(
    val recipient: String,
    val channel: NotificationChannel,
    message: String = "Invalid recipient '$recipient' for channel $channel"
) : NotificationException(message)

/**
 * Exception thrown when rate limit is exceeded
 */
class RateLimitExceededException(
    val channel: NotificationChannel,
    val retryAfterSeconds: Long,
    message: String = "Rate limit exceeded for channel $channel. Retry after $retryAfterSeconds seconds"
) : NotificationException(message)

/**
 * Exception thrown when notification content is invalid
 */
class InvalidContentException(
    val field: String,
    message: String = "Invalid notification content: $field"
) : NotificationException(message)
