package com.example.bank;

public class AccountNotFoundException extends TransferException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}
