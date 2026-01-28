package com.example.transfer.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Transfer(
    String id,
    String sourceAccountId,
    String targetAccountId,
    BigDecimal amount,
    TransferStatus status,
    String failureReason,
    Instant createdAt,
    Instant completedAt
) {
    public static Transfer create(
        String id,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        Instant createdAt
    ) {
        return new Transfer(
            id, sourceAccountId, targetAccountId, amount,
            TransferStatus.PENDING, null, createdAt, null
        );
    }

    public Transfer markCompleted(Instant completedAt) {
        return new Transfer(
            id, sourceAccountId, targetAccountId, amount,
            TransferStatus.COMPLETED, null, createdAt, completedAt
        );
    }

    public Transfer markFailed(String reason, Instant failedAt) {
        return new Transfer(
            id, sourceAccountId, targetAccountId, amount,
            TransferStatus.FAILED, reason, createdAt, failedAt
        );
    }

    public Transfer markRolledBack(Instant rolledBackAt) {
        return new Transfer(
            id, sourceAccountId, targetAccountId, amount,
            TransferStatus.ROLLED_BACK, failureReason, createdAt, rolledBackAt
        );
    }
}
