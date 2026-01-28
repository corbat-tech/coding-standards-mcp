package com.example.transfer.domain;

import java.math.BigDecimal;

public record Account(
    String id,
    String holderName,
    BigDecimal balance
) {
    public Account debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        return new Account(id, holderName, balance.subtract(amount));
    }

    public Account credit(BigDecimal amount) {
        return new Account(id, holderName, balance.add(amount));
    }
}
