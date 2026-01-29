package com.notification.infrastructure

import com.notification.domain.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Push notification channel implementation.
 * Handles sending push notifications to mobile devices.
 */
@Component
class PushChannel(
    private val pushConfig: PushConfig = PushConfig()
) : NotificationChannel {

    private val logger = LoggerFactory.getLogger(PushChannel::class.java)

    override val channelType: ChannelType = ChannelType.PUSH

    override suspend fun send(notification: Notification): NotificationResult {
        logger.info("Sending push notification to device ${notification.recipient}")

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
            // Simulate async push sending
            delay(pushConfig.sendDelayMs)
            sendPush(notification)
        } catch (e: Exception) {
            logger.error("Failed to send push: ${e.message}", e)
            NotificationResult.Failure(
                notificationId = notification.id,
                channelType = channelType,
                error = NotificationError.DeliveryFailed(
                    channelType = channelType,
                    message = "Push delivery failed: ${e.message}",
                    cause = e
                )
            )
        }
    }

    override fun validateRecipient(recipient: String): Boolean {
        return DEVICE_TOKEN_REGEX.matches(recipient)
    }

    override suspend fun isAvailable(): Boolean {
        return true // Push is always available in this implementation
    }

    private fun sendPush(notification: Notification): NotificationResult.Success {
        val payload = buildPayload(notification)
        logger.debug("Push sent - Token: {}, Payload: {}", notification.recipient, payload)

        return NotificationResult.Success(
            notificationId = notification.id,
            channelType = channelType,
            message = "Push notification sent to device"
        )
    }

    private fun buildPayload(notification: Notification): Map<String, Any> {
        return mapOf(
            "title" to notification.subject,
            "body" to truncateBody(notification.content),
            "data" to notification.metadata,
            "priority" to mapPriority(notification.priority)
        )
    }

    private fun truncateBody(content: String): String {
        return if (content.length > pushConfig.maxBodyLength) {
            content.take(pushConfig.maxBodyLength - 3) + "..."
        } else {
            content
        }
    }

    private fun mapPriority(priority: NotificationPriority): String {
        return when (priority) {
            NotificationPriority.URGENT -> "high"
            NotificationPriority.HIGH -> "high"
            NotificationPriority.NORMAL -> "normal"
            NotificationPriority.LOW -> "low"
        }
    }

    companion object {
        // Matches FCM/APNs device tokens (hex strings 64-200 chars)
        private val DEVICE_TOKEN_REGEX = Regex("^[a-fA-F0-9]{64,200}\$")
    }
}

/**
 * Configuration for push notification channel.
 */
data class PushConfig(
    val provider: String = "firebase",
    val sendDelayMs: Long = 30,
    val maxBodyLength: Int = 256,
    val timeToLiveSeconds: Int = 86400,
    val collapseKey: String? = null
)
