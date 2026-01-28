package com.example.transfer.domain;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    private final String accountId;
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(
        String accountId,
        BigDecimal currentBalance,
        BigDecimal requestedAmount
    ) {
        super(String.format(
            "Insufficient funds in account %s: balance=%s, requested=%s",
            accountId, currentBalance, requestedAmount
        ));
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
