package com.example.notification.strategy

import com.example.notification.domain.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Strategy implementation for sending SMS notifications.
 * Uses coroutines for async SMS delivery via external provider.
 */
@Component
class SmsNotificationStrategy : NotificationStrategy {

    private val logger = LoggerFactory.getLogger(SmsNotificationStrategy::class.java)

    override val channel: NotificationChannel = NotificationChannel.SMS

    // Phone number regex - supports international format
    private val phoneRegex = Regex("^\\+?[1-9]\\d{1,14}$")

    // Maximum SMS message length
    private val maxMessageLength = 160

    @Volatile
    private var available: Boolean = true

    @Volatile
    private var rateLimitRemaining: Int = 100

    override suspend fun send(request: NotificationRequest): NotificationResult {
        logger.info("Sending SMS notification to ${request.recipient}")

        if (!isAvailable()) {
            throw ChannelUnavailableException(channel)
        }

        if (!validateRecipient(request.recipient)) {
            throw InvalidRecipientException(request.recipient, channel)
        }

        if (rateLimitRemaining <= 0) {
            throw RateLimitExceededException(channel, 60)
        }

        validateContent(request)

        return try {
            // Simulate async SMS gateway call
            delay(simulateNetworkLatency())
            rateLimitRemaining--

            // Simulate occasional failures
            if (shouldSimulateFailure(request)) {
                throw DeliveryFailedException(
                    request.id,
                    channel,
                    "SMS gateway returned error code 503"
                )
            }

            logger.info("SMS sent successfully to ${request.recipient}")

            NotificationResult(
                requestId = request.id,
                channel = channel,
                status = NotificationStatus.SENT,
                message = "SMS delivered to ${request.recipient}",
                deliveredAt = Instant.now()
            )
        } catch (e: DeliveryFailedException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to send SMS to ${request.recipient}", e)
            NotificationResult(
                requestId = request.id,
                channel = channel,
                status = NotificationStatus.FAILED,
                errorDetails = e.message
            )
        }
    }

    override suspend fun validateRecipient(recipient: String): Boolean {
        // Async validation - could involve carrier lookup
        delay(15)
        return phoneRegex.matches(recipient.replace(" ", "").replace("-", ""))
    }

    override suspend fun isAvailable(): Boolean {
        // Could perform health check to SMS gateway
        delay(5)
        return available
    }

    /**
     * Set availability for testing purposes
     */
    fun setAvailable(available: Boolean) {
        this.available = available
    }

    /**
     * Reset rate limit for testing purposes
     */
    fun resetRateLimit(limit: Int = 100) {
        this.rateLimitRemaining = limit
    }

    private fun validateContent(request: NotificationRequest) {
        if (request.message.length > maxMessageLength) {
            throw InvalidContentException(
                "message",
                "SMS message exceeds maximum length of $maxMessageLength characters"
            )
        }
    }

    private fun simulateNetworkLatency(): Long {
        return (100..300).random().toLong()
    }

    private fun shouldSimulateFailure(request: NotificationRequest): Boolean {
        return request.metadata["simulateFailure"] == "true" ||
                request.recipient.contains("000000")
    }
}
