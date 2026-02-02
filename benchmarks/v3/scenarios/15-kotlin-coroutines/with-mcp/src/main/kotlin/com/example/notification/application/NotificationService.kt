package com.example.notification.application

import com.example.notification.domain.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

interface NotificationService {
    suspend fun send(request: SendNotificationRequest): List<Notification>
    suspend fun sendToChannel(notification: Notification): Notification
}

@Service
class NotificationServiceImpl(
    private val senders: List<NotificationSender>
) : NotificationService {

    private val sendersByChannel: Map<NotificationChannel, NotificationSender> =
        senders.associateBy { it.channel }

    override suspend fun send(request: SendNotificationRequest): List<Notification> = coroutineScope {
        val notifications = request.channels.map { channel ->
            Notification(
                recipient = request.recipient,
                subject = request.subject,
                message = request.message,
                channel = channel
            )
        }

        notifications.map { notification ->
            async { sendToChannel(notification) }
        }.map { it.await() }
    }

    override suspend fun sendToChannel(notification: Notification): Notification {
        val sender = sendersByChannel[notification.channel]
            ?: throw NotificationException("No sender for channel ${notification.channel}")

        return sender.send(notification).getOrElse { ex ->
            notification.copy(status = NotificationStatus.FAILED)
        }
    }
}
