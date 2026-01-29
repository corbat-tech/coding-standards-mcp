package com.notification.infrastructure

import com.notification.domain.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Email notification channel implementation.
 * Handles sending notifications via email.
 */
@Component
class EmailChannel(
    private val emailConfig: EmailConfig = EmailConfig()
) : NotificationChannel {

    private val logger = LoggerFactory.getLogger(EmailChannel::class.java)

    override val channelType: ChannelType = ChannelType.EMAIL

    override suspend fun send(notification: Notification): NotificationResult {
        logger.info("Sending email to ${notification.recipient}")

        if (!validateRecipient(notification.recipient)) {
            return NotificationResult.Failure(
                notificationId = notification.id,
                channelType = channelType,
                error = NotificationError.InvalidRecipient(
                    channelType = channelType,
                    recipient = notification.recipient
                )
            )
        }

        return try {
            // Simulate async email sending
            delay(emailConfig.sendDelayMs)
            sendEmail(notification)
        } catch (e: Exception) {
            logger.error("Failed to send email: ${e.message}", e)
            NotificationResult.Failure(
                notificationId = notification.id,
                channelType = channelType,
                error = NotificationError.DeliveryFailed(
                    channelType = channelType,
                    message = "Email delivery failed: ${e.message}",
                    cause = e
                )
            )
        }
    }

    override fun validateRecipient(recipient: String): Boolean {
        return EMAIL_REGEX.matches(recipient)
    }

    override suspend fun isAvailable(): Boolean {
        return true // Email is always available in this implementation
    }

    private fun sendEmail(notification: Notification): NotificationResult.Success {
        logger.debug(
            "Email sent - To: {}, Subject: {}",
            notification.recipient,
            notification.subject
        )
        return NotificationResult.Success(
            notificationId = notification.id,
            channelType = channelType,
            message = "Email sent to ${notification.recipient}"
        )
    }

    companion object {
        private val EMAIL_REGEX = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        )
    }
}

/**
 * Configuration for email channel.
 */
data class EmailConfig(
    val smtpHost: String = "localhost",
    val smtpPort: Int = 587,
    val useTls: Boolean = true,
    val sendDelayMs: Long = 100,
    val maxRecipientsPerBatch: Int = 50
)
