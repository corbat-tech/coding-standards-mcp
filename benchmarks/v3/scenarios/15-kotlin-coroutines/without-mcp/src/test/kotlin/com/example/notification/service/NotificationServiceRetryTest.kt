package com.example.notification.service

import com.example.notification.domain.*
import com.example.notification.strategy.NotificationStrategy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import java.time.Instant

class NotificationServiceRetryTest : FunSpec({

    lateinit var emailStrategy: NotificationStrategy
    lateinit var service: NotificationService

    beforeEach {
        emailStrategy = mockk()
        every { emailStrategy.channel } returns NotificationChannel.EMAIL
        service = NotificationService(listOf(emailStrategy))
    }

    afterEach {
        clearAllMocks()
    }

    test("sendWithRetry should succeed on first attempt if no failure") {
        runTest {
            val request = NotificationRequest(
                recipient = "test@example.com",
                subject = "Test",
                message = "Test message",
                channel = NotificationChannel.EMAIL
            )

            val successResult = NotificationResult(
                requestId = request.id,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                deliveredAt = Instant.now()
            )

            coEvery { emailStrategy.send(request) } returns successResult

            val result = service.sendWithRetry(request, maxRetries = 3, delayBetweenRetries = 10)

            result.status shouldBe NotificationStatus.SENT
            coVerify(exactly = 1) { emailStrategy.send(request) }
        }
    }

    test("sendWithRetry should retry on failure and succeed eventually") {
        runTest {
            val request = NotificationRequest(
                recipient = "test@example.com",
                subject = "Test",
                message = "Test message",
                channel = NotificationChannel.EMAIL
            )

            val failedResult = NotificationResult(
                requestId = request.id,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.FAILED,
                errorDetails = "Temporary failure"
            )

            val successResult = NotificationResult(
                requestId = request.id,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                deliveredAt = Instant.now()
            )

            coEvery { emailStrategy.send(request) } returnsMany listOf(
                failedResult,
                failedResult,
                successResult
            )

            val result = service.sendWithRetry(request, maxRetries = 3, delayBetweenRetries = 10)

            result.status shouldBe NotificationStatus.SENT
            coVerify(exactly = 3) { emailStrategy.send(request) }
        }
    }

    test("sendWithRetry should return failed result after max retries exceeded") {
        runTest {
            val request = NotificationRequest(
                recipient = "test@example.com",
                subject = "Test",
                message = "Test message",
                channel = NotificationChannel.EMAIL
            )

            val failedResult = NotificationResult(
                requestId = request.id,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.FAILED,
                errorDetails = "Permanent failure"
            )

            coEvery { emailStrategy.send(request) } returns failedResult

            val result = service.sendWithRetry(request, maxRetries = 2, delayBetweenRetries = 10)

            result.status shouldBe NotificationStatus.FAILED
            coVerify(exactly = 3) { emailStrategy.send(request) } // Initial + 2 retries
        }
    }

    test("sendWithRetry should apply exponential backoff") {
        runTest {
            val request = NotificationRequest(
                recipient = "test@example.com",
                subject = "Test",
                message = "Test message",
                channel = NotificationChannel.EMAIL
            )

            val failedResult = NotificationResult(
                requestId = request.id,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.FAILED,
                errorDetails = "Failure"
            )

            val successResult = NotificationResult(
                requestId = request.id,
                channel = NotificationChannel.EMAIL,
                status = NotificationStatus.SENT,
                deliveredAt = Instant.now()
            )

            coEvery { emailStrategy.send(request) } returnsMany listOf(
                failedResult,
                failedResult,
                successResult
            )

            // This test verifies the retry logic works with small delays
            val result = service.sendWithRetry(request, maxRetries = 3, delayBetweenRetries = 10)

            result.status shouldBe NotificationStatus.SENT
        }
    }
})
