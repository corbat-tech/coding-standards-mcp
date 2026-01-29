package com.notification.domain

/**
 * Base sealed class for notification errors.
 * Provides type-safe error handling with exhaustive when expressions.
 */
sealed class NotificationError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /**
     * Error when a requested channel is not supported.
     */
    data class ChannelNotSupported(
        val channelType: ChannelType,
        override val message: String = "Channel $channelType is not supported"
    ) : NotificationError(message)

    /**
     * Error when channel delivery fails.
     */
    data class DeliveryFailed(
        val channelType: ChannelType,
        override val message: String,
        override val cause: Throwable? = null
    ) : NotificationError(message, cause)

    /**
     * Error when request validation fails.
     */
    data class ValidationFailed(
        val violations: List<String>,
        override val message: String = "Validation failed: ${violations.joinToString(", ")}"
    ) : NotificationError(message)

    /**
     * Error when recipient address is invalid for the channel.
     */
    data class InvalidRecipient(
        val channelType: ChannelType,
        val recipient: String,
        override val message: String = "Invalid recipient '$recipient' for channel $channelType"
    ) : NotificationError(message)

    /**
     * Error when rate limit is exceeded.
     */
    data class RateLimitExceeded(
        val channelType: ChannelType,
        val retryAfterSeconds: Long,
        override val message: String = "Rate limit exceeded for $channelType, retry after $retryAfterSeconds seconds"
    ) : NotificationError(message)

    /**
     * Error when channel is temporarily unavailable.
     */
    data class ChannelUnavailable(
        val channelType: ChannelType,
        override val message: String = "Channel $channelType is temporarily unavailable",
        override val cause: Throwable? = null
    ) : NotificationError(message, cause)

    /**
     * Error when retry attempts are exhausted.
     */
    data class RetryExhausted(
        val channelType: ChannelType,
        val attempts: Int,
        override val message: String = "Retry exhausted after $attempts attempts for channel $channelType",
        override val cause: Throwable? = null
    ) : NotificationError(message, cause)

    /**
     * Check if this error is retryable.
     */
    fun isRetryable(): Boolean = when (this) {
        is ChannelUnavailable -> true
        is RateLimitExceeded -> true
        is DeliveryFailed -> cause != null
        is ChannelNotSupported -> false
        is ValidationFailed -> false
        is InvalidRecipient -> false
        is RetryExhausted -> false
    }
}

/**
 * Exception wrapper for NotificationError.
 * Use when integration with exception-based APIs is required.
 */
class NotificationException(
    val error: NotificationError
) : Exception(error.message, error.cause)
