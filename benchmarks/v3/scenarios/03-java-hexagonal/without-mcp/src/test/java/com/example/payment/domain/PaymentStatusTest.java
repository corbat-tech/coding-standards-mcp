package com.example.payment.domain;

import com.example.payment.domain.valueobject.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentStatus enum.
 */
@DisplayName("PaymentStatus Enum")
class PaymentStatusTest {

    @Test
    @DisplayName("PENDING should not be terminal")
    void pendingShouldNotBeTerminal() {
        assertFalse(PaymentStatus.PENDING.isTerminal());
    }

    @Test
    @DisplayName("PROCESSING should not be terminal")
    void processingShouldNotBeTerminal() {
        assertFalse(PaymentStatus.PROCESSING.isTerminal());
    }

    @Test
    @DisplayName("COMPLETED should be terminal")
    void completedShouldBeTerminal() {
        assertTrue(PaymentStatus.COMPLETED.isTerminal());
    }

    @Test
    @DisplayName("FAILED should be terminal")
    void failedShouldBeTerminal() {
        assertTrue(PaymentStatus.FAILED.isTerminal());
    }

    @Test
    @DisplayName("REFUNDED should be terminal")
    void refundedShouldBeTerminal() {
        assertTrue(PaymentStatus.REFUNDED.isTerminal());
    }

    @Test
    @DisplayName("COMPLETED should be refundable")
    void completedShouldBeRefundable() {
        assertTrue(PaymentStatus.COMPLETED.canBeRefunded());
    }

    @Test
    @DisplayName("PARTIALLY_REFUNDED should be refundable")
    void partiallyRefundedShouldBeRefundable() {
        assertTrue(PaymentStatus.PARTIALLY_REFUNDED.canBeRefunded());
    }

    @Test
    @DisplayName("PENDING should not be refundable")
    void pendingShouldNotBeRefundable() {
        assertFalse(PaymentStatus.PENDING.canBeRefunded());
    }

    @Test
    @DisplayName("FAILED should not be refundable")
    void failedShouldNotBeRefundable() {
        assertFalse(PaymentStatus.FAILED.canBeRefunded());
    }

    @Test
    @DisplayName("COMPLETED should be successful")
    void completedShouldBeSuccessful() {
        assertTrue(PaymentStatus.COMPLETED.isSuccessful());
    }

    @Test
    @DisplayName("PARTIALLY_REFUNDED should be successful")
    void partiallyRefundedShouldBeSuccessful() {
        assertTrue(PaymentStatus.PARTIALLY_REFUNDED.isSuccessful());
    }

    @Test
    @DisplayName("FAILED should not be successful")
    void failedShouldNotBeSuccessful() {
        assertFalse(PaymentStatus.FAILED.isSuccessful());
    }

    @Test
    @DisplayName("All statuses should have descriptions")
    void allStatusesShouldHaveDescriptions() {
        for (PaymentStatus status : PaymentStatus.values()) {
            assertNotNull(status.getDescription());
            assertFalse(status.getDescription().isEmpty());
        }
    }
}
