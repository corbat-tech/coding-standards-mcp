package com.notification.infrastructure

import com.notification.domain.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * SMS notification channel implementation.
 * Handles sending notifications via SMS.
 */
@Component
class SmsChannel(
    private val smsConfig: SmsConfig = SmsConfig()
) : NotificationChannel {

    private val logger = LoggerFactory.getLogger(SmsChannel::class.java)

    override val channelType: ChannelType = ChannelType.SMS

    override suspend fun send(notification: Notification): NotificationResult {
        logger.info("Sending SMS to ${notification.recipient}")

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
            // Simulate async SMS sending
            delay(smsConfig.sendDelayMs)
            sendSms(notification)
        } catch (e: Exception) {
            logger.error("Failed to send SMS: ${e.message}", e)
            NotificationResult.Failure(
                notificationId = notification.id,
                channelType = channelType,
                error = NotificationError.DeliveryFailed(
                    channelType = channelType,
                    message = "SMS delivery failed: ${e.message}",
                    cause = e
                )
            )
        }
    }

    override fun validateRecipient(recipient: String): Boolean {
        return PHONE_REGEX.matches(recipient)
    }

    override suspend fun isAvailable(): Boolean {
        return true // SMS is always available in this implementation
    }

    private fun sendSms(notification: Notification): NotificationResult.Success {
        val truncatedContent = truncateContent(notification.content)
        logger.debug("SMS sent - To: {}, Content: {}", notification.recipient, truncatedContent)

        return NotificationResult.Success(
            notificationId = notification.id,
            channelType = channelType,
            message = "SMS sent to ${notification.recipient}"
        )
    }

    private fun truncateContent(content: String): String {
        return if (content.length > smsConfig.maxMessageLength) {
            content.take(smsConfig.maxMessageLength - 3) + "..."
        } else {
            content
        }
    }

    companion object {
        private val PHONE_REGEX = Regex("^\\+?[1-9]\\d{7,14}\$")
    }
}

/**
 * Configuration for SMS channel.
 */
data class SmsConfig(
    val provider: String = "twilio",
    val sendDelayMs: Long = 50,
    val maxMessageLength: Int = 160,
    val supportedCountryCodes: List<String> = listOf("+1", "+44", "+34", "+49")
)
