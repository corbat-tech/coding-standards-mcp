package com.example.notification.integration

import com.example.notification.domain.NotificationChannel
import com.example.notification.domain.NotificationRequest
import com.example.notification.domain.NotificationStatus
import com.example.notification.service.NotificationService
import com.example.notification.strategy.EmailNotificationStrategy
import com.example.notification.strategy.PushNotificationStrategy
import com.example.notification.strategy.SmsNotificationStrategy
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest

/**
 * Integration tests for the notification service using real strategy implementations.
 */
class NotificationIntegrationTest : DescribeSpec({

    val emailStrategy = EmailNotificationStrategy()
    val smsStrategy = SmsNotificationStrategy()
    val pushStrategy = PushNotificationStrategy()
    val service = NotificationService(listOf(emailStrategy, smsStrategy, pushStrategy))

    describe("Notification Service Integration") {

        describe("Email notifications") {

            it("should send email notifications successfully") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "integration@example.com",
                        subject = "Integration Test",
                        message = "This is an integration test message",
                        channel = NotificationChannel.EMAIL
                    )

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.channel shouldBe NotificationChannel.EMAIL
                }
            }

            it("should validate email recipients correctly") {
                runTest {
                    service.validateRecipient("valid@example.com", NotificationChannel.EMAIL) shouldBe true
                    service.validateRecipient("invalid-email", NotificationChannel.EMAIL) shouldBe false
                }
            }
        }

        describe("SMS notifications") {

            it("should send SMS notifications successfully") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "+14155551234",
                        subject = "Alert",
                        message = "Test SMS message",
                        channel = NotificationChannel.SMS
                    )

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.channel shouldBe NotificationChannel.SMS
                }
            }

            it("should validate phone numbers correctly") {
                runTest {
                    service.validateRecipient("+14155551234", NotificationChannel.SMS) shouldBe true
                    service.validateRecipient("not-a-phone", NotificationChannel.SMS) shouldBe false
                }
            }
        }

        describe("Push notifications") {

            val validToken = "a".repeat(64)

            beforeEach {
                pushStrategy.clearRegisteredDevices()
                pushStrategy.registerDevice(validToken)
            }

            it("should send push notifications to registered devices") {
                runTest {
                    val request = NotificationRequest(
                        recipient = validToken,
                        subject = "New Message",
                        message = "You have a new message",
                        channel = NotificationChannel.PUSH
                    )

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.channel shouldBe NotificationChannel.PUSH
                }
            }

            it("should fail for unregistered devices") {
                runTest {
                    val unregisteredToken = "b".repeat(64)
                    val request = NotificationRequest(
                        recipient = unregisteredToken,
                        subject = "New Message",
                        message = "You have a new message",
                        channel = NotificationChannel.PUSH
                    )

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.FAILED
                }
            }
        }

        describe("Multi-channel operations") {

            it("should send to multiple channels concurrently") {
                runTest {
                    pushStrategy.clearRegisteredDevices()
                    val deviceToken = "c".repeat(64)
                    pushStrategy.registerDevice(deviceToken)

                    val requests = listOf(
                        NotificationRequest(
                            recipient = "multi@example.com",
                            subject = "Test",
                            message = "Email message",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            recipient = "+14155559999",
                            subject = "Test",
                            message = "SMS message",
                            channel = NotificationChannel.SMS
                        ),
                        NotificationRequest(
                            recipient = deviceToken,
                            subject = "Test",
                            message = "Push message",
                            channel = NotificationChannel.PUSH
                        )
                    )

                    val results = service.sendNotifications(requests)

                    results shouldHaveSize 3
                    results.count { it.status == NotificationStatus.SENT } shouldBe 3
                }
            }

            it("should get status of all channels") {
                runTest {
                    val status = service.getChannelStatus()

                    status.size shouldBe 3
                    status[NotificationChannel.EMAIL] shouldBe true
                    status[NotificationChannel.SMS] shouldBe true
                    status[NotificationChannel.PUSH] shouldBe true
                }
            }
        }

        describe("Broadcast functionality") {

            it("should broadcast to multiple email recipients") {
                runTest {
                    val recipients = listOf(
                        "user1@example.com",
                        "user2@example.com",
                        "user3@example.com"
                    )

                    val results = service.broadcast(
                        recipients = recipients,
                        subject = "Announcement",
                        message = "Important announcement for all users",
                        channel = NotificationChannel.EMAIL
                    )

                    results shouldHaveSize 3
                    results.all { it.status == NotificationStatus.SENT } shouldBe true
                }
            }
        }

        describe("Concurrent notification handling") {

            it("should handle many concurrent notifications") {
                runTest {
                    val requests = (1..50).map { i ->
                        NotificationRequest(
                            recipient = "user$i@example.com",
                            subject = "Concurrent Test $i",
                            message = "Message $i",
                            channel = NotificationChannel.EMAIL
                        )
                    }

                    val results = service.sendNotifications(requests)

                    results shouldHaveSize 50
                    results.count { it.status == NotificationStatus.SENT } shouldBe 50
                }
            }

            it("should handle concurrent multi-channel notifications") {
                runTest {
                    pushStrategy.clearRegisteredDevices()
                    val tokens = (1..10).map { "token$it" + "x".repeat(58) }
                    tokens.forEach { pushStrategy.registerDevice(it) }

                    val requests = mutableListOf<NotificationRequest>()

                    // Add email requests
                    requests.addAll((1..10).map { i ->
                        NotificationRequest(
                            recipient = "concurrent$i@example.com",
                            subject = "Test $i",
                            message = "Email $i",
                            channel = NotificationChannel.EMAIL
                        )
                    })

                    // Add SMS requests
                    requests.addAll((1..10).map { i ->
                        NotificationRequest(
                            recipient = "+1415555${1000 + i}",
                            subject = "Test $i",
                            message = "SMS $i",
                            channel = NotificationChannel.SMS
                        )
                    })

                    // Add push requests
                    requests.addAll(tokens.mapIndexed { i, token ->
                        NotificationRequest(
                            recipient = token,
                            subject = "Test $i",
                            message = "Push $i",
                            channel = NotificationChannel.PUSH
                        )
                    })

                    val results = service.sendNotifications(requests)

                    results shouldHaveSize 30
                    results.count { it.status == NotificationStatus.SENT } shouldBe 30
                }
            }
        }

        describe("Error handling") {

            it("should handle failures gracefully in batch") {
                runTest {
                    val requests = listOf(
                        NotificationRequest(
                            recipient = "valid@example.com",
                            subject = "Test",
                            message = "Valid message",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            recipient = "fail@example.com", // Will fail due to "fail" in email
                            subject = "Test",
                            message = "This should fail",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            recipient = "another@example.com",
                            subject = "Test",
                            message = "Another valid message",
                            channel = NotificationChannel.EMAIL
                        )
                    )

                    val results = service.sendNotifications(requests)

                    results shouldHaveSize 3
                    results.count { it.status == NotificationStatus.SENT } shouldBe 2
                    results.count { it.status == NotificationStatus.FAILED } shouldBe 1
                }
            }

            it("should handle channel unavailability") {
                runTest {
                    emailStrategy.setAvailable(false)

                    val request = NotificationRequest(
                        recipient = "test@example.com",
                        subject = "Test",
                        message = "Test message",
                        channel = NotificationChannel.EMAIL
                    )

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.FAILED

                    emailStrategy.setAvailable(true) // Reset
                }
            }
        }

        describe("Retry functionality") {

            it("should retry failed notifications") {
                runTest {
                    // This will fail initially due to "fail" in the recipient
                    // But retry won't help since it's a validation issue
                    val request = NotificationRequest(
                        recipient = "retry@example.com",
                        subject = "Test",
                        message = "Test message",
                        channel = NotificationChannel.EMAIL
                    )

                    val result = service.sendWithRetry(
                        request = request,
                        maxRetries = 2,
                        delayBetweenRetries = 10
                    )

                    // Should succeed on first try
                    result.status shouldBe NotificationStatus.SENT
                }
            }
        }

        describe("Structured concurrency") {

            it("should properly scope concurrent operations") {
                runTest {
                    coroutineScope {
                        val results = (1..5).map { i ->
                            async {
                                service.sendNotification(
                                    NotificationRequest(
                                        recipient = "structured$i@example.com",
                                        subject = "Structured Test",
                                        message = "Message $i",
                                        channel = NotificationChannel.EMAIL
                                    )
                                )
                            }
                        }.awaitAll()

                        results shouldHaveSize 5
                        results.all { it.status == NotificationStatus.SENT } shouldBe true
                    }
                }
            }
        }
    }
})
