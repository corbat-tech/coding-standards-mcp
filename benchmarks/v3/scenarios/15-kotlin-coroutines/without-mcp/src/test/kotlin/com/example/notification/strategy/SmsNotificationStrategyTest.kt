package com.example.notification.strategy

import com.example.notification.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest

class SmsNotificationStrategyTest : BehaviorSpec({

    lateinit var strategy: SmsNotificationStrategy

    beforeEach {
        strategy = SmsNotificationStrategy()
    }

    Given("an SMS notification strategy") {

        When("validating a valid phone number") {
            Then("it should return true") {
                runTest {
                    strategy.validateRecipient("+14155551234") shouldBe true
                    strategy.validateRecipient("+442071234567") shouldBe true
                    strategy.validateRecipient("14155551234") shouldBe true
                    strategy.validateRecipient("+1 415 555 1234") shouldBe true
                }
            }
        }

        When("validating an invalid phone number") {
            Then("it should return false") {
                runTest {
                    strategy.validateRecipient("invalid") shouldBe false
                    strategy.validateRecipient("+0123") shouldBe false
                    strategy.validateRecipient("") shouldBe false
                    strategy.validateRecipient("abc123") shouldBe false
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
                }
            }
        }

        When("sending a valid notification") {
            Then("it should succeed") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "+14155551234",
                        subject = "Alert",
                        message = "Your verification code is 123456",
                        channel = NotificationChannel.SMS
                    )

                    val result = strategy.send(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.requestId shouldBe request.id
                    result.channel shouldBe NotificationChannel.SMS
                    result.deliveredAt shouldNotBe null
                }
            }
        }

        When("sending to an invalid recipient") {
            Then("it should throw InvalidRecipientException") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "invalid-phone",
                        subject = "Alert",
                        message = "Test message",
                        channel = NotificationChannel.SMS
                    )

                    shouldThrow<InvalidRecipientException> {
                        strategy.send(request)
                    }
                }
            }
        }

        When("message exceeds maximum length") {
            Then("it should throw InvalidContentException") {
                runTest {
                    val longMessage = "A".repeat(200)
                    val request = NotificationRequest(
                        recipient = "+14155551234",
                        subject = "Alert",
                        message = longMessage,
                        channel = NotificationChannel.SMS
                    )

                    shouldThrow<InvalidContentException> {
                        strategy.send(request)
                    }
                }
            }
        }

        When("rate limit is exceeded") {
            Then("it should throw RateLimitExceededException") {
                runTest {
                    strategy.resetRateLimit(0)

                    val request = NotificationRequest(
                        recipient = "+14155551234",
                        subject = "Alert",
                        message = "Test message",
                        channel = NotificationChannel.SMS
                    )

                    shouldThrow<RateLimitExceededException> {
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
                        recipient = "+14155551234",
                        subject = "Alert",
                        message = "Test message",
                        channel = NotificationChannel.SMS
                    )

                    shouldThrow<ChannelUnavailableException> {
                        strategy.send(request)
                    }
                }
            }
        }
    }
})
