package com.example.notification.strategy

import com.example.notification.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest

class PushNotificationStrategyTest : BehaviorSpec({

    lateinit var strategy: PushNotificationStrategy

    beforeEach {
        strategy = PushNotificationStrategy()
        strategy.clearRegisteredDevices()
    }

    Given("a push notification strategy") {

        val validDeviceToken = "a".repeat(64) // Valid 64-char token

        When("validating a valid device token") {
            Then("it should return true") {
                runTest {
                    strategy.validateRecipient(validDeviceToken) shouldBe true
                    strategy.validateRecipient("abc123XYZ_-".repeat(6)) shouldBe true
                }
            }
        }

        When("validating an invalid device token") {
            Then("it should return false") {
                runTest {
                    strategy.validateRecipient("short") shouldBe false
                    strategy.validateRecipient("") shouldBe false
                    strategy.validateRecipient("invalid!token@#\$") shouldBe false
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

        When("sending to a registered device") {
            Then("it should succeed") {
                runTest {
                    strategy.registerDevice(validDeviceToken)

                    val request = NotificationRequest(
                        recipient = validDeviceToken,
                        subject = "New Message",
                        message = "You have a new message",
                        channel = NotificationChannel.PUSH
                    )

                    val result = strategy.send(request)

                    result.status shouldBe NotificationStatus.SENT
                    result.requestId shouldBe request.id
                    result.channel shouldBe NotificationChannel.PUSH
                    result.deliveredAt shouldNotBe null
                }
            }
        }

        When("sending with skip registration check") {
            Then("it should succeed even without registration") {
                runTest {
                    val request = NotificationRequest(
                        recipient = validDeviceToken,
                        subject = "New Message",
                        message = "You have a new message",
                        channel = NotificationChannel.PUSH,
                        metadata = mapOf("skipRegistrationCheck" to "true")
                    )

                    val result = strategy.send(request)

                    result.status shouldBe NotificationStatus.SENT
                }
            }
        }

        When("sending to an unregistered device") {
            Then("it should fail with appropriate error") {
                runTest {
                    val request = NotificationRequest(
                        recipient = validDeviceToken,
                        subject = "New Message",
                        message = "You have a new message",
                        channel = NotificationChannel.PUSH
                    )

                    val result = strategy.send(request)

                    result.status shouldBe NotificationStatus.FAILED
                    result.errorDetails shouldBe "Device token not registered"
                }
            }
        }

        When("sending to an invalid recipient") {
            Then("it should throw InvalidRecipientException") {
                runTest {
                    val request = NotificationRequest(
                        recipient = "short-token",
                        subject = "New Message",
                        message = "Test message",
                        channel = NotificationChannel.PUSH
                    )

                    shouldThrow<InvalidRecipientException> {
                        strategy.send(request)
                    }
                }
            }
        }

        When("title exceeds maximum length") {
            Then("it should throw InvalidContentException") {
                runTest {
                    strategy.registerDevice(validDeviceToken)

                    val request = NotificationRequest(
                        recipient = validDeviceToken,
                        subject = "A".repeat(100), // Exceeds 65 char limit
                        message = "Test message",
                        channel = NotificationChannel.PUSH
                    )

                    shouldThrow<InvalidContentException> {
                        strategy.send(request)
                    }
                }
            }
        }

        When("message body exceeds maximum length") {
            Then("it should throw InvalidContentException") {
                runTest {
                    strategy.registerDevice(validDeviceToken)

                    val request = NotificationRequest(
                        recipient = validDeviceToken,
                        subject = "Title",
                        message = "A".repeat(300), // Exceeds 240 char limit
                        channel = NotificationChannel.PUSH
                    )

                    shouldThrow<InvalidContentException> {
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
                        recipient = validDeviceToken,
                        subject = "Title",
                        message = "Test message",
                        channel = NotificationChannel.PUSH
                    )

                    shouldThrow<ChannelUnavailableException> {
                        strategy.send(request)
                    }
                }
            }
        }

        When("device registration and unregistration") {
            Then("should track devices correctly") {
                runTest {
                    strategy.registerDevice(validDeviceToken)
                    strategy.registerDevice("another_valid_token_" + "x".repeat(48))

                    // Send to registered device
                    val request1 = NotificationRequest(
                        recipient = validDeviceToken,
                        subject = "Test",
                        message = "Test",
                        channel = NotificationChannel.PUSH
                    )
                    strategy.send(request1).status shouldBe NotificationStatus.SENT

                    // Unregister and send again
                    strategy.unregisterDevice(validDeviceToken)
                    val request2 = NotificationRequest(
                        recipient = validDeviceToken,
                        subject = "Test",
                        message = "Test",
                        channel = NotificationChannel.PUSH
                    )
                    strategy.send(request2).status shouldBe NotificationStatus.FAILED
                }
            }
        }
    }
})
