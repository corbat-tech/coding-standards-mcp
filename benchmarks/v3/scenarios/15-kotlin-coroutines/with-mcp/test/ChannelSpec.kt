package com.notification.test

import com.notification.domain.*
import com.notification.infrastructure.EmailChannel
import com.notification.infrastructure.EmailConfig
import com.notification.infrastructure.PushChannel
import com.notification.infrastructure.PushConfig
import com.notification.infrastructure.SmsChannel
import com.notification.infrastructure.SmsConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ChannelSpec : DescribeSpec({

    describe("EmailChannel") {

        val emailChannel = EmailChannel(EmailConfig(sendDelayMs = 10))

        describe("send") {

            it("should send email successfully with valid recipient") {
                // Given
                val notification = Notification(
                    recipient = "user@example.com",
                    subject = "Welcome",
                    content = "Welcome to our service!",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = emailChannel.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                result.isSuccess().shouldBeTrue()
                (result as NotificationResult.Success).channelType shouldBe ChannelType.EMAIL
            }

            it("should fail with invalid email recipient") {
                // Given
                val notification = Notification(
                    recipient = "not-an-email",
                    subject = "Test",
                    content = "Content",
                    channelType = ChannelType.EMAIL
                )

                // When
                val result = emailChannel.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Failure>()
                (result as NotificationResult.Failure).error
                    .shouldBeInstanceOf<NotificationError.InvalidRecipient>()
            }
        }

        describe("validateRecipient") {

            it("should accept valid email addresses") {
                emailChannel.validateRecipient("user@example.com").shouldBeTrue()
                emailChannel.validateRecipient("user.name@example.co.uk").shouldBeTrue()
                emailChannel.validateRecipient("user+tag@example.org").shouldBeTrue()
            }

            it("should reject invalid email addresses") {
                emailChannel.validateRecipient("not-an-email").shouldBeFalse()
                emailChannel.validateRecipient("@example.com").shouldBeFalse()
                emailChannel.validateRecipient("user@").shouldBeFalse()
                emailChannel.validateRecipient("").shouldBeFalse()
            }
        }

        describe("isAvailable") {

            it("should always be available") {
                emailChannel.isAvailable().shouldBeTrue()
            }
        }
    }

    describe("SmsChannel") {

        val smsChannel = SmsChannel(SmsConfig(sendDelayMs = 10))

        describe("send") {

            it("should send SMS successfully with valid phone number") {
                // Given
                val notification = Notification(
                    recipient = "+12025551234",
                    subject = "Alert",
                    content = "Your verification code is 123456",
                    channelType = ChannelType.SMS
                )

                // When
                val result = smsChannel.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                (result as NotificationResult.Success).channelType shouldBe ChannelType.SMS
            }

            it("should fail with invalid phone number") {
                // Given
                val notification = Notification(
                    recipient = "not-a-phone",
                    subject = "Test",
                    content = "Content",
                    channelType = ChannelType.SMS
                )

                // When
                val result = smsChannel.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Failure>()
                (result as NotificationResult.Failure).error
                    .shouldBeInstanceOf<NotificationError.InvalidRecipient>()
            }
        }

        describe("validateRecipient") {

            it("should accept valid phone numbers") {
                smsChannel.validateRecipient("+12025551234").shouldBeTrue()
                smsChannel.validateRecipient("+442071234567").shouldBeTrue()
                smsChannel.validateRecipient("12025551234").shouldBeTrue()
            }

            it("should reject invalid phone numbers") {
                smsChannel.validateRecipient("not-a-phone").shouldBeFalse()
                smsChannel.validateRecipient("+1").shouldBeFalse()
                smsChannel.validateRecipient("").shouldBeFalse()
                smsChannel.validateRecipient("abc123").shouldBeFalse()
            }
        }
    }

    describe("PushChannel") {

        val pushChannel = PushChannel(PushConfig(sendDelayMs = 10))

        describe("send") {

            it("should send push notification with valid device token") {
                // Given
                val validToken = "a".repeat(64) // 64 hex characters
                val notification = Notification(
                    recipient = validToken,
                    subject = "New Message",
                    content = "You have a new message!",
                    channelType = ChannelType.PUSH
                )

                // When
                val result = pushChannel.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Success>()
                (result as NotificationResult.Success).channelType shouldBe ChannelType.PUSH
            }

            it("should fail with invalid device token") {
                // Given
                val notification = Notification(
                    recipient = "short-token",
                    subject = "Test",
                    content = "Content",
                    channelType = ChannelType.PUSH
                )

                // When
                val result = pushChannel.send(notification)

                // Then
                result.shouldBeInstanceOf<NotificationResult.Failure>()
                (result as NotificationResult.Failure).error
                    .shouldBeInstanceOf<NotificationError.InvalidRecipient>()
            }
        }

        describe("validateRecipient") {

            it("should accept valid device tokens") {
                val token64 = "a".repeat(64)
                val token100 = "b".repeat(100)
                val token200 = "c".repeat(200)

                pushChannel.validateRecipient(token64).shouldBeTrue()
                pushChannel.validateRecipient(token100).shouldBeTrue()
                pushChannel.validateRecipient(token200).shouldBeTrue()
            }

            it("should reject invalid device tokens") {
                pushChannel.validateRecipient("short").shouldBeFalse()
                pushChannel.validateRecipient("").shouldBeFalse()
                pushChannel.validateRecipient("g".repeat(64)).shouldBeFalse() // 'g' not hex
                pushChannel.validateRecipient("a".repeat(63)).shouldBeFalse() // too short
            }
        }
    }
})
