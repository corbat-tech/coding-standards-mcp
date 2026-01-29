package com.example.notification.strategy

import com.example.notification.domain.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Strategy implementation for sending email notifications.
 * Uses coroutines for async email delivery.
 */
@Component
class EmailNotificationStrategy : NotificationStrategy {

    private val logger = LoggerFactory.getLogger(EmailNotificationStrategy::class.java)

    override val channel: NotificationChannel = NotificationChannel.EMAIL

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // Simulated availability state
    @Volatile
    private var available: Boolean = true

    override suspend fun send(request: NotificationRequest): NotificationResult {
        logger.info("Sending email notification to ${request.recipient}")

        if (!isAvailable()) {
            throw ChannelUnavailableException(channel)
        }

        if (!validateRecipient(request.recipient)) {
            throw InvalidRecipientException(request.recipient, channel)
        }

        return try {
            // Simulate async email sending
            delay(simulateNetworkLatency())

            // Simulate occasional failures for testing
            if (shouldSimulateFailure(request)) {
                throw DeliveryFailedException(
                    request.id,
                    channel,
                    "SMTP server connection failed"
                )
            }

            logger.info("Email sent successfully to ${request.recipient}")

            NotificationResult(
                requestId = request.id,
                channel = channel,
                status = NotificationStatus.SENT,
                message = "Email delivered to ${request.recipient}",
                deliveredAt = Instant.now()
            )
        } catch (e: DeliveryFailedException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to send email to ${request.recipient}", e)
            NotificationResult(
                requestId = request.id,
                channel = channel,
                status = NotificationStatus.FAILED,
                errorDetails = e.message
            )
        }
    }

    override suspend fun validateRecipient(recipient: String): Boolean {
        // Async validation - could involve checking against a blocklist service
        delay(10)
        return emailRegex.matches(recipient)
    }

    override suspend fun isAvailable(): Boolean {
        // Could perform health check to SMTP server
        delay(5)
        return available
    }

    /**
     * Set availability for testing purposes
     */
    fun setAvailable(available: Boolean) {
        this.available = available
    }

    private fun simulateNetworkLatency(): Long {
        return (50..200).random().toLong()
    }

    private fun shouldSimulateFailure(request: NotificationRequest): Boolean {
        // Simulate 5% failure rate, or fail if email contains "fail" for testing
        return request.metadata["simulateFailure"] == "true" ||
                request.recipient.contains("fail", ignoreCase = true)
    }
}
