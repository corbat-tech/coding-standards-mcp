package com.example.notification.strategy

import com.example.notification.domain.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Strategy implementation for sending push notifications.
 * Uses coroutines for async push delivery to mobile devices.
 */
@Component
class PushNotificationStrategy : NotificationStrategy {

    private val logger = LoggerFactory.getLogger(PushNotificationStrategy::class.java)

    override val channel: NotificationChannel = NotificationChannel.PUSH

    // Device token regex - alphanumeric, typically 64 characters for iOS, variable for Android
    private val deviceTokenRegex = Regex("^[a-zA-Z0-9_-]{32,256}$")

    // Maximum push notification title length
    private val maxTitleLength = 65

    // Maximum push notification body length
    private val maxBodyLength = 240

    @Volatile
    private var available: Boolean = true

    // Simulated registered device tokens
    private val registeredDevices = mutableSetOf<String>()

    override suspend fun send(request: NotificationRequest): NotificationResult {
        logger.info("Sending push notification to device ${request.recipient}")

        if (!isAvailable()) {
            throw ChannelUnavailableException(channel)
        }

        if (!validateRecipient(request.recipient)) {
            throw InvalidRecipientException(request.recipient, channel)
        }

        validateContent(request)

        return try {
            // Simulate async push notification service call (Firebase/APNs)
            delay(simulateNetworkLatency())

            // Check if device is registered
            if (!isDeviceRegistered(request.recipient) && !request.metadata.containsKey("skipRegistrationCheck")) {
                return NotificationResult(
                    requestId = request.id,
                    channel = channel,
                    status = NotificationStatus.FAILED,
                    errorDetails = "Device token not registered"
                )
            }

            // Simulate occasional failures
            if (shouldSimulateFailure(request)) {
                throw DeliveryFailedException(
                    request.id,
                    channel,
                    "Push notification service unavailable"
                )
            }

            logger.info("Push notification sent successfully to device ${request.recipient}")

            NotificationResult(
                requestId = request.id,
                channel = channel,
                status = NotificationStatus.SENT,
                message = "Push notification delivered to device",
                deliveredAt = Instant.now()
            )
        } catch (e: DeliveryFailedException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to send push notification to ${request.recipient}", e)
            NotificationResult(
                requestId = request.id,
                channel = channel,
                status = NotificationStatus.FAILED,
                errorDetails = e.message
            )
        }
    }

    override suspend fun validateRecipient(recipient: String): Boolean {
        // Async validation of device token format
        delay(5)
        return deviceTokenRegex.matches(recipient)
    }

    override suspend fun isAvailable(): Boolean {
        // Could perform health check to push notification service
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
     * Register a device token for testing purposes
     */
    fun registerDevice(deviceToken: String) {
        registeredDevices.add(deviceToken)
    }

    /**
     * Unregister a device token for testing purposes
     */
    fun unregisterDevice(deviceToken: String) {
        registeredDevices.remove(deviceToken)
    }

    /**
     * Clear all registered devices for testing purposes
     */
    fun clearRegisteredDevices() {
        registeredDevices.clear()
    }

    private suspend fun isDeviceRegistered(deviceToken: String): Boolean {
        delay(10)
        return registeredDevices.contains(deviceToken)
    }

    private fun validateContent(request: NotificationRequest) {
        if (request.subject.length > maxTitleLength) {
            throw InvalidContentException(
                "subject",
                "Push notification title exceeds maximum length of $maxTitleLength characters"
            )
        }
        if (request.message.length > maxBodyLength) {
            throw InvalidContentException(
                "message",
                "Push notification body exceeds maximum length of $maxBodyLength characters"
            )
        }
    }

    private fun simulateNetworkLatency(): Long {
        return (30..150).random().toLong()
    }

    private fun shouldSimulateFailure(request: NotificationRequest): Boolean {
        return request.metadata["simulateFailure"] == "true" ||
                request.recipient.startsWith("invalid")
    }
}
