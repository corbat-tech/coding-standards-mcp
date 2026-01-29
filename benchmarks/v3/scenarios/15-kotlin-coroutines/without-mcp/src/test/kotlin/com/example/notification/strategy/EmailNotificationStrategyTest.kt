package com.example.notification.strategy

import com.example.notification.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest

class EmailNotificationStrategyTest : BehaviorSpec({

    val strategy = EmailNotificationStrategy()

    Given("an email notification strategy") {

        When("validating a valid email address") {
            Then("it should return true") {
                runTest {
                    strategy.validateRecipient("test@example.com") shouldBe true
                    strategy.validateRecipient("user.name@domain.org") shouldBe true
                    strategy.validateRecipient("user+tag@example.co.uk") shouldBe true
                }
            }
        }

        When("validating an invalid email address") {
            Then("it should return false") {
                runTest {
                    strategy.validateRecipient("invalid-email") shouldBe false
                    strategy.validateRecipient("@nodomain.com") shouldBe false
                    strategy.validateRecipient("user@") shouldBe false
                    strategy.validateRecipient("") shouldBe false
                }
            }
        }

        When("checking availability") {
            Then("it should return true by default") {
                runTest {
                    strategy.isAvailable() shouldBe true
                }
            }

            Then("it should return false when set unavailable") {
                runTest {
                    strategy.setAvailable(false)
                    strategy.isAvailable() shouldBe false
                    strategy.setAvailable(true) // Reset
                }
            }
        }

        When("sending a valid notification") {
            Then("it should succeed") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "test@example.com",
                        subject = "Test Subject",
                        message = "Test message content",
                        channel = NotificationChannel.EMAIL
                    )

                    val result = strategy.send(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.requestId shouldBe request.id
                    result.channel shouldBe NotificationChannel.EMAIL
                    result.deliveredAt shouldNotBe null
                }
            }
        }

        When("sending to an invalid recipient") {
            Then("it should throw InvalidRecipientException") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "invalid-email",
                        subject = "Test Subject",
                        message = "Test message",
                        channel = NotificationChannel.EMAIL
                    )

                    shouldThrow<InvalidRecipientException> {
                        strategy.send(request)
                    }
                }
            }
        }

        When("channel is unavailable") {
            Then("it should throw ChannelUnavailableException") {
                runTest {
                    strategy.setAvailable(false)

                    val request = NotificationRequest(
                        recipient = "test@example.com",
                        subject = "Test Subject",
                        message = "Test message",
                        channel = NotificationChannel.EMAIL
                    )

                    shouldThrow<ChannelUnavailableException> {
                        strategy.send(request)
                    }

                    strategy.setAvailable(true) // Reset
                }
            }
        }

        When("simulating a failure") {
            Then("it should throw DeliveryFailedException") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "fail@example.com",
                        subject = "Test Subject",
                        message = "Test message",
                        channel = NotificationChannel.EMAIL
                    )

                    shouldThrow<DeliveryFailedException> {
                        strategy.send(request)
                    }
                }
            }
        }
    }
})
