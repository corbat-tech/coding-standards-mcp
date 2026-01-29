package com.notification.application

import com.notification.domain.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Implementation of the NotificationService.
 * Handles notification sending with coroutine-based async execution.
 */
@Service
class NotificationServiceImpl(
    private val strategy: NotificationStrategy,
    private val repository: NotificationRepository
) : NotificationService {

    private val logger = LoggerFactory.getLogger(NotificationServiceImpl::class.java)

    override suspend fun send(notification: Notification): NotificationResult {
        logger.info("Sending notification ${notification.id} via ${notification.channelType}")

        val savedNotification = repository.save(notification)
        val request = NotificationRequest(
            recipient = notification.recipient,
            subject = notification.subject,
            content = notification.content,
            channels = setOf(notification.channelType),
            priority = notification.priority,
            metadata = notification.metadata
        )

        val channels = strategy.selectChannels(request)
        val channel = channels.firstOrNull { it.channelType == notification.channelType }
            ?: return createChannelNotSupportedResult(savedNotification)

        return executeChannelSend(channel, savedNotification)
    }

    override suspend fun sendMultiChannel(
        request: NotificationRequest
    ): MultiChannelResult = coroutineScope {
        logger.info("Sending multi-channel notification to ${request.recipient}")

        val channels = strategy.selectChannels(request)
        val availableChannelTypes = channels.map { it.channelType }.toSet()

        val validChannels = request.channels.filter { it in availableChannelTypes }
        val unsupportedChannels = request.channels - availableChannelTypes

        val results = mutableListOf<NotificationResult>()

        // Add failures for unsupported channels
        unsupportedChannels.forEach { channelType ->
            val notification = request.toNotification(channelType)
            results.add(createChannelNotSupportedResult(notification))
        }

        // Send to valid channels concurrently
        val asyncResults = validChannels.map { channelType ->
            async {
                val notification = request.toNotification(channelType)
                val savedNotification = repository.save(notification)
                val channel = channels.first { it.channelType == channelType }
                executeChannelSend(channel, savedNotification)
            }
        }

        results.addAll(asyncResults.awaitAll())
        MultiChannelResult(results = results)
    }

    override suspend fun sendWithRetry(
        notification: Notification,
        maxRetries: Int
    ): NotificationResult {
        var lastResult: NotificationResult? = null
        var attempt = 0

        while (attempt <= maxRetries) {
            val result = send(notification)

            if (result.isSuccess()) {
                return result
            }

            lastResult = result

            if (!shouldRetry(result, attempt, maxRetries)) {
                break
            }

            attempt++
            delay(calculateBackoff(attempt))
            logger.info("Retrying notification ${notification.id}, attempt $attempt")
        }

        return createRetryExhaustedResult(notification, lastResult, maxRetries)
    }

    private suspend fun executeChannelSend(
        channel: NotificationChannel,
        notification: Notification
    ): NotificationResult {
        val result = channel.send(notification)
        updateNotificationStatus(notification, result)
        return result
    }

    private suspend fun updateNotificationStatus(
        notification: Notification,
        result: NotificationResult
    ) {
        val updated = when (result) {
            is NotificationResult.Success -> notification.markAsSent()
            is NotificationResult.Failure -> notification.markAsFailed()
        }
        repository.update(updated)
    }

    private fun createChannelNotSupportedResult(
        notification: Notification
    ): NotificationResult.Failure {
        return NotificationResult.Failure(
            notificationId = notification.id,
            channelType = notification.channelType,
            error = NotificationError.ChannelNotSupported(notification.channelType)
        )
    }

    private fun shouldRetry(
        result: NotificationResult,
        attempt: Int,
        maxRetries: Int
    ): Boolean {
        if (attempt >= maxRetries) return false
        return when (result) {
            is NotificationResult.Success -> false
            is NotificationResult.Failure -> result.error.isRetryable()
        }
    }

    private fun calculateBackoff(attempt: Int): Long {
        val baseDelayMs = 1000L
        val maxDelayMs = 30000L
        val delay = baseDelayMs * (1 shl (attempt - 1))
        return minOf(delay, maxDelayMs)
    }

    private fun createRetryExhaustedResult(
        notification: Notification,
        lastResult: NotificationResult?,
        maxRetries: Int
    ): NotificationResult.Failure {
        val cause = (lastResult as? NotificationResult.Failure)?.error?.cause
        return NotificationResult.Failure(
            notificationId = notification.id,
            channelType = notification.channelType,
            error = NotificationError.RetryExhausted(
                channelType = notification.channelType,
                attempts = maxRetries + 1,
                cause = cause
            )
        )
    }
}
