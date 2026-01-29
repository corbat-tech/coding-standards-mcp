package com.notification.application

import com.notification.domain.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Strategy implementation for selecting notification channels.
 * Implements priority-based channel selection with health awareness.
 */
@Component
class ChannelStrategy(
    private val channels: List<NotificationChannel>,
    private val healthMonitor: ChannelHealthMonitor? = null
) : NotificationStrategy {

    private val logger = LoggerFactory.getLogger(ChannelStrategy::class.java)

    private val channelMap: Map<ChannelType, NotificationChannel> by lazy {
        channels.associateBy { it.channelType }
    }

    override suspend fun selectChannels(
        request: NotificationRequest
    ): List<NotificationChannel> {
        val requestedChannels = request.channels.mapNotNull { channelMap[it] }
        val sortedChannels = sortByPriority(requestedChannels, request.priority)

        return filterAvailableChannels(sortedChannels)
    }

    override suspend fun shouldUseChannel(
        notification: Notification,
        channel: NotificationChannel
    ): Boolean {
        if (!channel.validateRecipient(notification.recipient)) {
            logger.warn(
                "Invalid recipient {} for channel {}",
                notification.recipient,
                channel.channelType
            )
            return false
        }

        if (!channel.isAvailable()) {
            logger.warn("Channel {} is not available", channel.channelType)
            return false
        }

        return isChannelHealthy(channel.channelType)
    }

    private suspend fun sortByPriority(
        channels: List<NotificationChannel>,
        priority: NotificationPriority
    ): List<NotificationChannel> {
        return when (priority) {
            NotificationPriority.URGENT -> sortBySpeed(channels)
            NotificationPriority.HIGH -> sortByReliability(channels)
            NotificationPriority.NORMAL -> channels
            NotificationPriority.LOW -> sortByCost(channels)
        }
    }

    private suspend fun sortBySpeed(
        channels: List<NotificationChannel>
    ): List<NotificationChannel> {
        val speedOrder = listOf(ChannelType.PUSH, ChannelType.SMS, ChannelType.EMAIL)
        return channels.sortedBy { speedOrder.indexOf(it.channelType) }
    }

    private suspend fun sortByReliability(
        channels: List<NotificationChannel>
    ): List<NotificationChannel> {
        if (healthMonitor == null) return channels

        return channels.sortedByDescending { channel ->
            healthMonitor.getHealth(channel.channelType).successRate
        }
    }

    private fun sortByCost(
        channels: List<NotificationChannel>
    ): List<NotificationChannel> {
        val costOrder = listOf(ChannelType.EMAIL, ChannelType.PUSH, ChannelType.SMS)
        return channels.sortedBy { costOrder.indexOf(it.channelType) }
    }

    private suspend fun filterAvailableChannels(
        channels: List<NotificationChannel>
    ): List<NotificationChannel> {
        return channels.filter { it.isAvailable() }
    }

    private suspend fun isChannelHealthy(channelType: ChannelType): Boolean {
        if (healthMonitor == null) return true

        val health = healthMonitor.getHealth(channelType)
        return health.isHealthy && health.successRate >= MINIMUM_SUCCESS_RATE
    }

    companion object {
        private const val MINIMUM_SUCCESS_RATE = 0.5
    }
}
