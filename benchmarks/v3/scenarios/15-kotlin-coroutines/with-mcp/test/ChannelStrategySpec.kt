package com.notification.test

import com.notification.application.ChannelStrategy
import com.notification.domain.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class ChannelStrategySpec : DescribeSpec({

    describe("ChannelStrategy") {

        describe("selectChannels") {

            it("should return requested channels when all available") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL)
                val smsChannel = createMockChannel(ChannelType.SMS)
                val pushChannel = createMockChannel(ChannelType.PUSH)

                val strategy = ChannelStrategy(
                    listOf(emailChannel, smsChannel, pushChannel)
                )

                val request = NotificationRequest(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Content",
                    channels = setOf(ChannelType.EMAIL, ChannelType.SMS)
                )

                // When
                val channels = strategy.selectChannels(request)

                // Then
                channels shouldHaveSize 2
                channels.map { it.channelType } shouldContainExactly
                    listOf(ChannelType.EMAIL, ChannelType.SMS)
            }

            it("should filter out unavailable channels") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL, available = true)
                val smsChannel = createMockChannel(ChannelType.SMS, available = false)

                val strategy = ChannelStrategy(listOf(emailChannel, smsChannel))

                val request = NotificationRequest(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Content",
                    channels = setOf(ChannelType.EMAIL, ChannelType.SMS)
                )

                // When
                val channels = strategy.selectChannels(request)

                // Then
                channels shouldHaveSize 1
                channels.first().channelType shouldBe ChannelType.EMAIL
            }

            it("should return empty list when no channels match") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL)
                val strategy = ChannelStrategy(listOf(emailChannel))

                val request = NotificationRequest(
                    recipient = "+1234567890",
                    subject = "Test",
                    content = "Content",
                    channels = setOf(ChannelType.SMS, ChannelType.PUSH)
                )

                // When
                val channels = strategy.selectChannels(request)

                // Then
                channels.shouldBeEmpty()
            }

            it("should sort channels by speed for urgent priority") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL)
                val smsChannel = createMockChannel(ChannelType.SMS)
                val pushChannel = createMockChannel(ChannelType.PUSH)

                val strategy = ChannelStrategy(
                    listOf(emailChannel, smsChannel, pushChannel)
                )

                val request = NotificationRequest(
                    recipient = "test@example.com",
                    subject = "Urgent",
                    content = "Content",
                    channels = setOf(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH),
                    priority = NotificationPriority.URGENT
                )

                // When
                val channels = strategy.selectChannels(request)

                // Then
                channels shouldHaveSize 3
                // PUSH is fastest, then SMS, then EMAIL
                channels[0].channelType shouldBe ChannelType.PUSH
                channels[1].channelType shouldBe ChannelType.SMS
                channels[2].channelType shouldBe ChannelType.EMAIL
            }

            it("should sort channels by cost for low priority") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL)
                val smsChannel = createMockChannel(ChannelType.SMS)
                val pushChannel = createMockChannel(ChannelType.PUSH)

                val strategy = ChannelStrategy(
                    listOf(smsChannel, pushChannel, emailChannel) // Intentionally unordered
                )

                val request = NotificationRequest(
                    recipient = "test@example.com",
                    subject = "Low Priority",
                    content = "Content",
                    channels = setOf(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH),
                    priority = NotificationPriority.LOW
                )

                // When
                val channels = strategy.selectChannels(request)

                // Then
                channels shouldHaveSize 3
                // EMAIL is cheapest, then PUSH, then SMS
                channels[0].channelType shouldBe ChannelType.EMAIL
                channels[1].channelType shouldBe ChannelType.PUSH
                channels[2].channelType shouldBe ChannelType.SMS
            }
        }

        describe("shouldUseChannel") {

            it("should return true for valid recipient and available channel") {
                // Given
                val emailChannel = createMockChannel(
                    ChannelType.EMAIL,
                    available = true,
                    validRecipient = true
                )

                val strategy = ChannelStrategy(listOf(emailChannel))

                val notification = Notification(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Content",
                    channelType = ChannelType.EMAIL
                )

                // When
                val shouldUse = strategy.shouldUseChannel(notification, emailChannel)

                // Then
                shouldUse.shouldBeTrue()
            }

            it("should return false for invalid recipient") {
                // Given
                val emailChannel = createMockChannel(
                    ChannelType.EMAIL,
                    available = true,
                    validRecipient = false
                )

                val strategy = ChannelStrategy(listOf(emailChannel))

                val notification = Notification(
                    recipient = "invalid-email",
                    subject = "Test",
                    content = "Content",
                    channelType = ChannelType.EMAIL
                )

                // When
                val shouldUse = strategy.shouldUseChannel(notification, emailChannel)

                // Then
                shouldUse.shouldBeFalse()
            }

            it("should return false when channel is unavailable") {
                // Given
                val emailChannel = createMockChannel(
                    ChannelType.EMAIL,
                    available = false,
                    validRecipient = true
                )

                val strategy = ChannelStrategy(listOf(emailChannel))

                val notification = Notification(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Content",
                    channelType = ChannelType.EMAIL
                )

                // When
                val shouldUse = strategy.shouldUseChannel(notification, emailChannel)

                // Then
                shouldUse.shouldBeFalse()
            }

            it("should consider health monitor when available") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL)

                val healthMonitor = mockk<ChannelHealthMonitor>()
                coEvery { healthMonitor.getHealth(ChannelType.EMAIL) } returns ChannelHealth(
                    channelType = ChannelType.EMAIL,
                    isHealthy = false,
                    successRate = 0.3,
                    averageLatencyMs = 5000
                )

                val strategy = ChannelStrategy(
                    channels = listOf(emailChannel),
                    healthMonitor = healthMonitor
                )

                val notification = Notification(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Content",
                    channelType = ChannelType.EMAIL
                )

                // When
                val shouldUse = strategy.shouldUseChannel(notification, emailChannel)

                // Then
                shouldUse.shouldBeFalse()
            }
        }
    }
})

private fun createMockChannel(
    type: ChannelType,
    available: Boolean = true,
    validRecipient: Boolean = true
): NotificationChannel {
    val channel = mockk<NotificationChannel>()

    coEvery { channel.channelType } returns type
    coEvery { channel.isAvailable() } returns available
    coEvery { channel.validateRecipient(any()) } returns validRecipient

    return channel
}
