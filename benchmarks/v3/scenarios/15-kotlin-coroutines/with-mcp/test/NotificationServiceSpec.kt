package com.notification.test

import com.notification.application.ChannelStrategy
import com.notification.application.NotificationServiceImpl
import com.notification.domain.*
import com.notification.infrastructure.InMemoryNotificationRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class NotificationServiceSpec : DescribeSpec({

    describe("NotificationService") {

        describe("send single notification") {

            it("should send email notification successfully") {
                // Given
                val mockEmailChannel = createMockChannel(ChannelType.EMAIL, true)
                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(mockEmailChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "test@example.com",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = service.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                result.isSuccess().shouldBeTrue()
                (result as NotificationResult.Success).channelType shouldBe ChannelType.EMAIL
            }

            it("should send SMS notification successfully") {
                // Given
                val mockSmsChannel = createMockChannel(ChannelType.SMS, true)
                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(mockSmsChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "+1234567890",
                    channelType = ChannelType.SMS
                )

                // When
                val result = service.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                (result as NotificationResult.Success).channelType shouldBe ChannelType.SMS
            }

            it("should send push notification successfully") {
                // Given
                val mockPushChannel = createMockChannel(ChannelType.PUSH, true)
                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(mockPushChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "a".repeat(64), // Valid device token
                    channelType = ChannelType.PUSH
                )

                // When
                val result = service.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                (result as NotificationResult.Success).channelType shouldBe ChannelType.PUSH
            }

            it("should fail when channel not supported") {
                // Given
                val mockEmailChannel = createMockChannel(ChannelType.EMAIL, true)
                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(mockEmailChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "+1234567890",
                    channelType = ChannelType.SMS // SMS channel not available
                )

                // When
                val result = service.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Failure>()
                (result as NotificationResult.Failure).error
                    .shouldBeInstanceOf<NotificationError.ChannelNotSupported>()
            }

            it("should handle channel failure gracefully") {
                // Given
                val failingChannel = createMockChannel(
                    ChannelType.EMAIL,
                    false,
                    NotificationError.DeliveryFailed(
                        channelType = ChannelType.EMAIL,
                        message = "SMTP connection failed"
                    )
                )
                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(failingChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "test@example.com",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = service.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Failure>()
                result.isFailure().shouldBeTrue()
            }
        }

        describe("sendMultiChannel") {

            it("should send to multiple channels concurrently") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL, true)
                val smsChannel = createMockChannel(ChannelType.SMS, true)
                val pushChannel = createMockChannel(ChannelType.PUSH, true)

                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(emailChannel, smsChannel, pushChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val request = NotificationRequest(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Test content",
                    channels = setOf(ChannelType.EMAIL, ChannelType.SMS, ChannelType.PUSH)
                )

                // When
                val result = service.sendMultiChannel(request)

                // Then
                result.results shouldHaveSize 3
                result.isFullySuccessful.shouldBeTrue()
                result.successCount shouldBe 3
                result.failureCount shouldBe 0
            }

            it("should handle partial failures in multi-channel send") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL, true)
                val failingSmsChannel = createMockChannel(
                    ChannelType.SMS,
                    false,
                    NotificationError.DeliveryFailed(ChannelType.SMS, "SMS provider down")
                )

                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(emailChannel, failingSmsChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val request = NotificationRequest(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Test content",
                    channels = setOf(ChannelType.EMAIL, ChannelType.SMS)
                )

                // When
                val result = service.sendMultiChannel(request)

                // Then
                result.results shouldHaveSize 2
                result.isPartiallySuccessful.shouldBeTrue()
                result.successCount shouldBe 1
                result.failureCount shouldBe 1
            }

            it("should report unsupported channels as failures") {
                // Given
                val emailChannel = createMockChannel(ChannelType.EMAIL, true)
                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(emailChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val request = NotificationRequest(
                    recipient = "test@example.com",
                    subject = "Test",
                    content = "Test content",
                    channels = setOf(ChannelType.EMAIL, ChannelType.SMS) // SMS not available
                )

                // When
                val result = service.sendMultiChannel(request)

                // Then
                result.results shouldHaveSize 2
                result.successCount shouldBe 1
                result.failureCount shouldBe 1

                val smsResult = result.results.find {
                    it is NotificationResult.Failure &&
                        it.channelType == ChannelType.SMS
                }
                smsResult.shouldBeInstanceOf<NotificationResult.Failure>()
                (smsResult as NotificationResult.Failure).error
                    .shouldBeInstanceOf<NotificationError.ChannelNotSupported>()
            }
        }

        describe("sendWithRetry") {

            it("should succeed on first attempt without retry") {
                // Given
                val mockChannel = createMockChannel(ChannelType.EMAIL, true)
                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(mockChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "test@example.com",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = service.sendWithRetry(notification, maxRetries = 3)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                coVerify(exactly = 1) { mockChannel.send(any()) }
            }

            it("should retry on transient failure and succeed") {
                // Given
                val mockChannel = mockk<NotificationChannel>()
                coEvery { mockChannel.channelType } returns ChannelType.EMAIL
                coEvery { mockChannel.isAvailable() } returns true
                coEvery { mockChannel.validateRecipient(any()) } returns true

                var attempts = 0
                coEvery { mockChannel.send(any()) } answers {
                    attempts++
                    if (attempts < 2) {
                        NotificationResult.Failure(
                            notificationId = "test-id",
                            channelType = ChannelType.EMAIL,
                            error = NotificationError.ChannelUnavailable(ChannelType.EMAIL)
                        )
                    } else {
                        NotificationResult.Success(
                            notificationId = "test-id",
                            channelType = ChannelType.EMAIL
                        )
                    }
                }

                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(mockChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "test@example.com",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = service.sendWithRetry(notification, maxRetries = 3)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                attempts shouldBe 2
            }

            it("should return retry exhausted after max attempts") {
                // Given
                val failingChannel = createMockChannel(
                    ChannelType.EMAIL,
                    false,
                    NotificationError.ChannelUnavailable(ChannelType.EMAIL)
                )

                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(failingChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "test@example.com",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = service.sendWithRetry(notification, maxRetries = 2)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Failure>()
                (result as NotificationResult.Failure).error
                    .shouldBeInstanceOf<NotificationError.RetryExhausted>()

                val retryError = result.error as NotificationError.RetryExhausted
                retryError.attempts shouldBe 3 // initial + 2 retries
            }

            it("should not retry on non-retryable errors") {
                // Given
                val failingChannel = createMockChannel(
                    ChannelType.EMAIL,
                    false,
                    NotificationError.InvalidRecipient(
                        channelType = ChannelType.EMAIL,
                        recipient = "invalid"
                    )
                )

                val repository = InMemoryNotificationRepository()
                val strategy = ChannelStrategy(listOf(failingChannel))
                val service = NotificationServiceImpl(strategy, repository)

                val notification = createNotification(
                    recipient = "invalid",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = service.sendWithRetry(notification, maxRetries = 3)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Failure>()
                coVerify(exactly = 1) { failingChannel.send(any()) }
            }
        }
    }
})

// Helper functions for creating test data

private fun createNotification(
    recipient: String,
    channelType: ChannelType,
    subject: String = "Test Subject",
    content: String = "Test Content"
): Notification {
    return Notification(
        id = "test-notification-id",
        recipient = recipient,
        subject = subject,
        content = content,
        channelType = channelType
    )
}

private fun createMockChannel(
    type: ChannelType,
    shouldSucceed: Boolean,
    error: NotificationError? = null
): NotificationChannel {
    val channel = mockk<NotificationChannel>()

    coEvery { channel.channelType } returns type
    coEvery { channel.isAvailable() } returns true
    coEvery { channel.validateRecipient(any()) } returns true

    coEvery { channel.send(any()) } answers {
        val notification = firstArg<Notification>()
        if (shouldSucceed) {
            NotificationResult.Success(
                notificationId = notification.id,
                channelType = type
            )
        } else {
            NotificationResult.Failure(
                notificationId = notification.id,
                channelType = type,
                error = error ?: NotificationError.DeliveryFailed(type, "Mock failure")
            )
        }
    }

    return channel
}
