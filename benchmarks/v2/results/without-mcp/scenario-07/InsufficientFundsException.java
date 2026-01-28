package com.example.bank;

public class InsufficientFundsException extends TransferException {
    public InsufficientFundsException(String accountId) {
        super("Insufficient funds in account: " + accountId);
    }
}
