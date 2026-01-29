package com.example.notification.service

import com.example.notification.domain.*
import com.example.notification.strategy.NotificationStrategy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Main notification service that coordinates sending notifications
 * through various channels using the Strategy pattern.
 * Leverages Kotlin coroutines for async operations.
 */
@Service
class NotificationService(
    private val strategies: List<NotificationStrategy>
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    private val strategyMap: Map<NotificationChannel, NotificationStrategy> =
        strategies.associateBy { it.channel }

    /**
     * Sends a single notification through the appropriate channel.
     * This is a suspend function that can be called from coroutine context.
     *
     * @param request The notification request to send
     * @return The result of the notification attempt
     */
    suspend fun sendNotification(request: NotificationRequest): NotificationResult {
        logger.info("Processing notification request ${request.id} for channel ${request.channel}")

        val strategy = getStrategy(request.channel)

        return try {
            strategy.send(request)
        } catch (e: NotificationException) {
            logger.error("Notification failed for request ${request.id}: ${e.message}")
            NotificationResult(
                requestId = request.id,
                channel = request.channel,
                status = NotificationStatus.FAILED,
                errorDetails = e.message
            )
        }
    }

    /**
     * Sends multiple notifications concurrently.
     * Uses structured concurrency with coroutineScope.
     *
     * @param requests List of notification requests to send
     * @return List of results for each notification
     */
    suspend fun sendNotifications(requests: List<NotificationRequest>): List<NotificationResult> =
        coroutineScope {
            requests.map { request ->
                async {
                    sendNotification(request)
                }
            }.awaitAll()
        }

    /**
     * Sends a batch of notifications with optional fail-fast behavior.
     *
     * @param batch The batch notification request
     * @return The batch result containing all individual results
     */
    suspend fun sendBatch(batch: BatchNotificationRequest): BatchNotificationResult =
        coroutineScope {
            val results = mutableListOf<NotificationResult>()

            if (batch.failFast) {
                // Fail-fast: stop on first failure
                for (notification in batch.notifications) {
                    val result = sendNotification(notification)
                    results.add(result)
                    if (result.status == NotificationStatus.FAILED) {
                        break
                    }
                }
            } else {
                // Send all concurrently
                results.addAll(sendNotifications(batch.notifications))
            }

            val successCount = results.count { it.status == NotificationStatus.SENT }
            val failureCount = results.count { it.status == NotificationStatus.FAILED }

            BatchNotificationResult(
                batchId = batch.id,
                results = results,
                totalCount = results.size,
                successCount = successCount,
                failureCount = failureCount,
                completedAt = Instant.now()
            )
        }

    /**
     * Sends notifications as a Flow for reactive streaming.
     * Useful for processing large numbers of notifications.
     *
     * @param requests Flow of notification requests
     * @return Flow of notification results
     */
    fun sendNotificationsAsFlow(requests: Flow<NotificationRequest>): Flow<NotificationResult> =
        requests.map { request ->
            sendNotification(request)
        }

    /**
     * Sends notifications concurrently with a specified parallelism limit.
     *
     * @param requests List of notification requests
     * @param maxConcurrency Maximum number of concurrent sends
     * @return Flow of notification results
     */
    fun sendWithConcurrencyLimit(
        requests: List<NotificationRequest>,
        maxConcurrency: Int = 10
    ): Flow<NotificationResult> = flow {
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrency)
        coroutineScope {
            requests.map { request ->
                async {
                    semaphore.withPermit {
                        sendNotification(request)
                    }
                }
            }.forEach { deferred ->
                emit(deferred.await())
            }
        }
    }

    /**
     * Sends the same notification to multiple recipients through specified channel.
     *
     * @param recipients List of recipient addresses
     * @param subject Notification subject
     * @param message Notification message
     * @param channel Target channel
     * @param priority Notification priority
     * @return List of notification results
     */
    suspend fun broadcast(
        recipients: List<String>,
        subject: String,
        message: String,
        channel: NotificationChannel,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): List<NotificationResult> {
        val requests = recipients.map { recipient ->
            NotificationRequest(
                recipient = recipient,
                subject = subject,
                message = message,
                channel = channel,
                priority = priority
            )
        }
        return sendNotifications(requests)
    }

    /**
     * Sends a notification with retry logic.
     *
     * @param request The notification request
     * @param maxRetries Maximum number of retry attempts
     * @param delayBetweenRetries Delay between retries in milliseconds
     * @return The notification result
     */
    suspend fun sendWithRetry(
        request: NotificationRequest,
        maxRetries: Int = 3,
        delayBetweenRetries: Long = 1000
    ): NotificationResult {
        var lastResult: NotificationResult? = null
        var attempt = 0

        while (attempt <= maxRetries) {
            val result = sendNotification(request)
            lastResult = result

            if (result.status == NotificationStatus.SENT) {
                return result
            }

            attempt++
            if (attempt <= maxRetries) {
                logger.info("Retrying notification ${request.id}, attempt $attempt of $maxRetries")
                delay(delayBetweenRetries * attempt) // Exponential backoff
            }
        }

        return lastResult ?: NotificationResult(
            requestId = request.id,
            channel = request.channel,
            status = NotificationStatus.FAILED,
            errorDetails = "Max retries exceeded"
        )
    }

    /**
     * Sends a notification through multiple channels (fan-out).
     *
     * @param recipient The recipient
     * @param subject The subject
     * @param message The message
     * @param channels List of channels to send through
     * @return Map of channel to result
     */
    suspend fun sendMultiChannel(
        recipient: String,
        subject: String,
        message: String,
        channels: List<NotificationChannel>
    ): Map<NotificationChannel, NotificationResult> = coroutineScope {
        channels.map { channel ->
            async {
                val request = NotificationRequest(
                    recipient = recipient,
                    subject = subject,
                    message = message,
                    channel = channel
                )
                channel to sendNotification(request)
            }
        }.awaitAll().toMap()
    }

    /**
     * Checks if a specific channel is available.
     *
     * @param channel The channel to check
     * @return true if available, false otherwise
     */
    suspend fun isChannelAvailable(channel: NotificationChannel): Boolean {
        return try {
            getStrategy(channel).isAvailable()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the availability status of all channels.
     *
     * @return Map of channel to availability status
     */
    suspend fun getChannelStatus(): Map<NotificationChannel, Boolean> = coroutineScope {
        strategyMap.map { (channel, strategy) ->
            async {
                channel to try {
                    strategy.isAvailable()
                } catch (e: Exception) {
                    false
                }
            }
        }.awaitAll().toMap()
    }

    /**
     * Validates a recipient for a specific channel.
     *
     * @param recipient The recipient to validate
     * @param channel The target channel
     * @return true if valid, false otherwise
     */
    suspend fun validateRecipient(recipient: String, channel: NotificationChannel): Boolean {
        return getStrategy(channel).validateRecipient(recipient)
    }

    private fun getStrategy(channel: NotificationChannel): NotificationStrategy {
        return strategyMap[channel]
            ?: throw ChannelUnavailableException(channel, "No strategy registered for channel $channel")
    }
}
