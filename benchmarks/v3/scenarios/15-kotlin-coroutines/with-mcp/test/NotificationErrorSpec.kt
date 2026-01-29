package com.notification.test

import com.notification.domain.ChannelType
import com.notification.domain.NotificationError
import com.notification.domain.NotificationException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class NotificationErrorSpec : DescribeSpec({

    describe("NotificationError") {

        describe("isRetryable") {

            it("should return true for ChannelUnavailable") {
                val error = NotificationError.ChannelUnavailable(ChannelType.EMAIL)
                error.isRetryable().shouldBeTrue()
            }

            it("should return true for RateLimitExceeded") {
                val error = NotificationError.RateLimitExceeded(
                    channelType = ChannelType.SMS,
                    retryAfterSeconds = 60
                )
                error.isRetryable().shouldBeTrue()
            }

            it("should return true for DeliveryFailed with cause") {
                val error = NotificationError.DeliveryFailed(
                    channelType = ChannelType.EMAIL,
                    message = "Connection timeout",
                    cause = RuntimeException("Timeout")
                )
                error.isRetryable().shouldBeTrue()
            }

            it("should return false for DeliveryFailed without cause") {
                val error = NotificationError.DeliveryFailed(
                    channelType = ChannelType.EMAIL,
                    message = "Invalid content"
                )
                error.isRetryable().shouldBeFalse()
            }

            it("should return false for ChannelNotSupported") {
                val error = NotificationError.ChannelNotSupported(ChannelType.PUSH)
                error.isRetryable().shouldBeFalse()
            }

            it("should return false for ValidationFailed") {
                val error = NotificationError.ValidationFailed(
                    violations = listOf("Content is required")
                )
                error.isRetryable().shouldBeFalse()
            }

            it("should return false for InvalidRecipient") {
                val error = NotificationError.InvalidRecipient(
                    channelType = ChannelType.EMAIL,
                    recipient = "invalid"
                )
                error.isRetryable().shouldBeFalse()
            }

            it("should return false for RetryExhausted") {
                val error = NotificationError.RetryExhausted(
                    channelType = ChannelType.EMAIL,
                    attempts = 3
                )
                error.isRetryable().shouldBeFalse()
            }
        }

        describe("error messages") {

            it("should have meaningful message for ChannelNotSupported") {
                val error = NotificationError.ChannelNotSupported(ChannelType.SMS)
                error.message shouldContain "SMS"
                error.message shouldContain "not supported"
            }

            it("should have meaningful message for InvalidRecipient") {
                val error = NotificationError.InvalidRecipient(
                    channelType = ChannelType.EMAIL,
                    recipient = "bad@email"
                )
                error.message shouldContain "bad@email"
                error.message shouldContain "EMAIL"
            }

            it("should have meaningful message for RateLimitExceeded") {
                val error = NotificationError.RateLimitExceeded(
                    channelType = ChannelType.SMS,
                    retryAfterSeconds = 120
                )
                error.message shouldContain "120"
                error.message shouldContain "SMS"
            }

            it("should have meaningful message for RetryExhausted") {
                val error = NotificationError.RetryExhausted(
                    channelType = ChannelType.PUSH,
                    attempts = 5
                )
                error.message shouldContain "5"
                error.message shouldContain "PUSH"
            }

            it("should have meaningful message for ValidationFailed") {
                val error = NotificationError.ValidationFailed(
                    violations = listOf("Field A is required", "Field B is too long")
                )
                error.message shouldContain "Field A is required"
                error.message shouldContain "Field B is too long"
            }
        }
    }

    describe("NotificationException") {

        it("should wrap NotificationError correctly") {
            val error = NotificationError.DeliveryFailed(
                channelType = ChannelType.EMAIL,
                message = "SMTP error",
                cause = RuntimeException("Connection refused")
            )

            val exception = NotificationException(error)

            exception.error shouldBe error
            exception.message shouldBe error.message
            exception.cause shouldBe error.cause
        }
    }
})
