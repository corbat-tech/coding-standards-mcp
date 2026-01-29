package com.example.notification.service

import com.example.notification.domain.*
import com.example.notification.strategy.NotificationStrategy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.time.Instant

class NotificationServiceTest : BehaviorSpec({

    lateinit var emailStrategy: NotificationStrategy
    lateinit var smsStrategy: NotificationStrategy
    lateinit var pushStrategy: NotificationStrategy
    lateinit var service: NotificationService

    beforeEach {
        emailStrategy = mockk()
        smsStrategy = mockk()
        pushStrategy = mockk()

        every { emailStrategy.channel } returns NotificationChannel.EMAIL
        every { smsStrategy.channel } returns NotificationChannel.SMS
        every { pushStrategy.channel } returns NotificationChannel.PUSH

        service = NotificationService(listOf(emailStrategy, smsStrategy, pushStrategy))
    }

    afterEach {
        clearAllMocks()
    }

    Given("a notification service") {

        When("sending a single email notification") {
            Then("it should use the email strategy") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "test@example.com",
                        subject = "Test",
                        message = "Test message",
                        channel = NotificationChannel.EMAIL
                    )

                    val expectedResult = NotificationResult(
                        requestId = request.id,
                        channel = NotificationChannel.EMAIL,
                        status = NotificationStatus.SENT,
                        deliveredAt = Instant.now()
                    )

                    coEvery { emailStrategy.send(request) } returns expectedResult

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.channel shouldBe NotificationChannel.EMAIL
                    coVerify(exactly = 1) { emailStrategy.send(request) }
                }
            }
        }

        When("sending a single SMS notification") {
            Then("it should use the SMS strategy") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "+14155551234",
                        subject = "Alert",
                        message = "Test message",
                        channel = NotificationChannel.SMS
                    )

                    val expectedResult = NotificationResult(
                        requestId = request.id,
                        channel = NotificationChannel.SMS,
                        status = NotificationStatus.SENT,
                        deliveredAt = Instant.now()
                    )

                    coEvery { smsStrategy.send(request) } returns expectedResult

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.channel shouldBe NotificationChannel.SMS
                    coVerify(exactly = 1) { smsStrategy.send(request) }
                }
            }
        }

        When("sending multiple notifications concurrently") {
            Then("it should process all in parallel") {
                runTest {
                    val requests = listOf(
                        NotificationRequest(
                            recipient = "user1@example.com",
                            subject = "Test 1",
                            message = "Message 1",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            recipient = "user2@example.com",
                            subject = "Test 2",
                            message = "Message 2",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            recipient = "+14155551234",
                            subject = "Test 3",
                            message = "Message 3",
                            channel = NotificationChannel.SMS
                        )
                    )

                    coEvery { emailStrategy.send(any()) } answers {
                        NotificationResult(
                            requestId = firstArg<NotificationRequest>().id,
                            channel = NotificationChannel.EMAIL,
                            status = NotificationStatus.SENT,
                            deliveredAt = Instant.now()
                        )
                    }

                    coEvery { smsStrategy.send(any()) } answers {
                        NotificationResult(
                            requestId = firstArg<NotificationRequest>().id,
                            channel = NotificationChannel.SMS,
                            status = NotificationStatus.SENT,
                            deliveredAt = Instant.now()
                        )
                    }

                    val results = service.sendNotifications(requests)

                    results shouldHaveSize 3
                    results.count { it.status == NotificationStatus.SENT } shouldBe 3
                    coVerify(exactly = 2) { emailStrategy.send(any()) }
                    coVerify(exactly = 1) { smsStrategy.send(any()) }
                }
            }
        }

        When("sending a batch with fail-fast disabled") {
            Then("it should continue even if some fail") {
                runTest {
                    val requests = listOf(
                        NotificationRequest(
                            id = "1",
                            recipient = "user1@example.com",
                            subject = "Test 1",
                            message = "Message 1",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            id = "2",
                            recipient = "fail@example.com",
                            subject = "Test 2",
                            message = "Message 2",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            id = "3",
                            recipient = "user3@example.com",
                            subject = "Test 3",
                            message = "Message 3",
                            channel = NotificationChannel.EMAIL
                        )
                    )

                    val batch = BatchNotificationRequest(
                        notifications = requests,
                        failFast = false
                    )

                    coEvery { emailStrategy.send(match { it.id == "1" }) } returns NotificationResult(
                        requestId = "1",
                        channel = NotificationChannel.EMAIL,
                        status = NotificationStatus.SENT
                    )

                    coEvery { emailStrategy.send(match { it.id == "2" }) } returns NotificationResult(
                        requestId = "2",
                        channel = NotificationChannel.EMAIL,
                        status = NotificationStatus.FAILED,
                        errorDetails = "Delivery failed"
                    )

                    coEvery { emailStrategy.send(match { it.id == "3" }) } returns NotificationResult(
                        requestId = "3",
                        channel = NotificationChannel.EMAIL,
                        status = NotificationStatus.SENT
                    )

                    val result = service.sendBatch(batch)

                    result.totalCount shouldBe 3
                    result.successCount shouldBe 2
                    result.failureCount shouldBe 1
                }
            }
        }

        When("sending a batch with fail-fast enabled") {
            Then("it should stop on first failure") {
                runTest {
                    val requests = listOf(
                        NotificationRequest(
                            id = "1",
                            recipient = "user1@example.com",
                            subject = "Test 1",
                            message = "Message 1",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            id = "2",
                            recipient = "fail@example.com",
                            subject = "Test 2",
                            message = "Message 2",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            id = "3",
                            recipient = "user3@example.com",
                            subject = "Test 3",
                            message = "Message 3",
                            channel = NotificationChannel.EMAIL
                        )
                    )

                    val batch = BatchNotificationRequest(
                        notifications = requests,
                        failFast = true
                    )

                    coEvery { emailStrategy.send(match { it.id == "1" }) } returns NotificationResult(
                        requestId = "1",
                        channel = NotificationChannel.EMAIL,
                        status = NotificationStatus.SENT
                    )

                    coEvery { emailStrategy.send(match { it.id == "2" }) } returns NotificationResult(
                        requestId = "2",
                        channel = NotificationChannel.EMAIL,
                        status = NotificationStatus.FAILED,
                        errorDetails = "Delivery failed"
                    )

                    val result = service.sendBatch(batch)

                    result.totalCount shouldBe 2 // Should stop after failure
                    result.successCount shouldBe 1
                    result.failureCount shouldBe 1
                    coVerify(exactly = 0) { emailStrategy.send(match { it.id == "3" }) }
                }
            }
        }

        When("broadcasting to multiple recipients") {
            Then("it should send to all recipients") {
                runTest {
                    val recipients = listOf("user1@example.com", "user2@example.com", "user3@example.com")

                    coEvery { emailStrategy.send(any()) } answers {
                        NotificationResult(
                            requestId = firstArg<NotificationRequest>().id,
                            channel = NotificationChannel.EMAIL,
                            status = NotificationStatus.SENT,
                            deliveredAt = Instant.now()
                        )
                    }

                    val results = service.broadcast(
                        recipients = recipients,
                        subject = "Announcement",
                        message = "Important announcement",
                        channel = NotificationChannel.EMAIL
                    )

                    results shouldHaveSize 3
                    results.all { it.status == NotificationStatus.SENT } shouldBe true
                    coVerify(exactly = 3) { emailStrategy.send(any()) }
                }
            }
        }

        When("sending as a flow") {
            Then("it should emit results as they complete") {
                runTest {
                    val requests = listOf(
                        NotificationRequest(
                            recipient = "user1@example.com",
                            subject = "Test 1",
                            message = "Message 1",
                            channel = NotificationChannel.EMAIL
                        ),
                        NotificationRequest(
                            recipient = "user2@example.com",
                            subject = "Test 2",
                            message = "Message 2",
                            channel = NotificationChannel.EMAIL
                        )
                    )

                    coEvery { emailStrategy.send(any()) } answers {
                        NotificationResult(
                            requestId = firstArg<NotificationRequest>().id,
                            channel = NotificationChannel.EMAIL,
                            status = NotificationStatus.SENT,
                            deliveredAt = Instant.now()
                        )
                    }

                    val results = service.sendNotificationsAsFlow(requests.asFlow()).toList()

                    results shouldHaveSize 2
                    results.all { it.status == NotificationStatus.SENT } shouldBe true
                }
            }
        }

        When("checking channel availability") {
            Then("it should return status for all channels") {
                runTest {
                    coEvery { emailStrategy.isAvailable() } returns true
                    coEvery { smsStrategy.isAvailable() } returns false
                    coEvery { pushStrategy.isAvailable() } returns true

                    val status = service.getChannelStatus()

                    status[NotificationChannel.EMAIL] shouldBe true
                    status[NotificationChannel.SMS] shouldBe false
                    status[NotificationChannel.PUSH] shouldBe true
                }
            }
        }

        When("validating recipients") {
            Then("it should delegate to the appropriate strategy") {
                runTest {
                    coEvery { emailStrategy.validateRecipient("test@example.com") } returns true
                    coEvery { smsStrategy.validateRecipient("+14155551234") } returns true
                    coEvery { emailStrategy.validateRecipient("invalid") } returns false

                    service.validateRecipient("test@example.com", NotificationChannel.EMAIL) shouldBe true
                    service.validateRecipient("+14155551234", NotificationChannel.SMS) shouldBe true
                    service.validateRecipient("invalid", NotificationChannel.EMAIL) shouldBe false
                }
            }
        }

        When("sending multi-channel notification") {
            Then("it should send through all specified channels") {
                runTest {
                    coEvery { emailStrategy.send(any()) } answers {
                        NotificationResult(
                            requestId = firstArg<NotificationRequest>().id,
                            channel = NotificationChannel.EMAIL,
                            status = NotificationStatus.SENT,
                            deliveredAt = Instant.now()
                        )
                    }

                    coEvery { smsStrategy.send(any()) } answers {
                        NotificationResult(
                            requestId = firstArg<NotificationRequest>().id,
                            channel = NotificationChannel.SMS,
                            status = NotificationStatus.SENT,
                            deliveredAt = Instant.now()
                        )
                    }

                    val results = service.sendMultiChannel(
                        recipient = "user@example.com",
                        subject = "Alert",
                        message = "Important message",
                        channels = listOf(NotificationChannel.EMAIL, NotificationChannel.SMS)
                    )

                    results.size shouldBe 2
                    results[NotificationChannel.EMAIL]?.status shouldBe NotificationStatus.SENT
                    results[NotificationChannel.SMS]?.status shouldBe NotificationStatus.SENT
                }
            }
        }

        When("handling strategy exceptions") {
            Then("it should return failed result") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "test@example.com",
                        subject = "Test",
                        message = "Test message",
                        channel = NotificationChannel.EMAIL
                    )

                    coEvery { emailStrategy.send(request) } throws DeliveryFailedException(
                        request.id,
                        NotificationChannel.EMAIL,
                        "SMTP connection failed"
                    )

                    val result = service.sendNotification(request)

                    result.status shouldBe NotificationStatus.FAILED
                    result.errorDetails shouldBe "SMTP connection failed"
                }
            }
        }
    }
})
