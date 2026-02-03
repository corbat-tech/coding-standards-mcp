package com.example.notification

import com.example.notification.application.NotificationServiceImpl
import com.example.notification.domain.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant

class NotificationServiceTest : DescribeSpec({

    describe("NotificationService") {
        val emailSender = mockk<NotificationSender>()
        val smsSender = mockk<NotificationSender>()
        val pushSender = mockk<NotificationSender>()

        beforeTest {
            coEvery { emailSender.channel } returns NotificationChannel.EMAIL
            coEvery { smsSender.channel } returns NotificationChannel.SMS
            coEvery { pushSender.channel } returns NotificationChannel.PUSH
        }

        it("should send notifications to all channels") {
            coEvery { emailSender.send(any()) } answers {
                val notification = firstArg<Notification>()
                Result.success(notification.copy(status = NotificationStatus.SENT, sentAt = Instant.now()))
            }
            coEvery { smsSender.send(any()) } answers {
                val notification = firstArg<Notification>()
                Result.success(notification.copy(status = NotificationStatus.SENT, sentAt = Instant.now()))
            }

            val service = NotificationServiceImpl(listOf(emailSender, smsSender, pushSender))

            val request = SendNotificationRequest(
                recipient = "user@test.com",
                subject = "Test",
                message = "Hello",
                channels = listOf(NotificationChannel.EMAIL, NotificationChannel.SMS)
            )

            val results = service.send(request)

            results shouldHaveSize 2
            results.all { it.status == NotificationStatus.SENT } shouldBe true
        }

        it("should mark notification as FAILED when sender fails") {
            coEvery { emailSender.send(any()) } answers {
                Result.failure(RuntimeException("Send failed"))
            }

            val service = NotificationServiceImpl(listOf(emailSender, smsSender, pushSender))

            val notification = Notification(
                recipient = "user@test.com",
                subject = "Test",
                message = "Hello",
                channel = NotificationChannel.EMAIL
            )

            val result = service.sendToChannel(notification)

            result.status shouldBe NotificationStatus.FAILED
        }

        it("should send to single channel") {
            coEvery { pushSender.send(any()) } answers {
                val notification = firstArg<Notification>()
                Result.success(notification.copy(status = NotificationStatus.SENT, sentAt = Instant.now()))
            }

            val service = NotificationServiceImpl(listOf(emailSender, smsSender, pushSender))

            val notification = Notification(
                recipient = "device-token",
                subject = "Push",
                message = "Hello",
                channel = NotificationChannel.PUSH
            )

            val result = service.sendToChannel(notification)

            result.status shouldBe NotificationStatus.SENT
            result.sentAt shouldBe { it != null }
        }
    }
})
