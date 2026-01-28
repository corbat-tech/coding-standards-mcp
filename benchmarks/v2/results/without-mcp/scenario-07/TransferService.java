package com.example.bank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransferService {

    private final AccountRepository accountRepository;

    public TransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Transfer execute(String sourceAccountId, String targetAccountId, BigDecimal amount) {
        String transferId = UUID.randomUUID().toString();
        Transfer transfer = new Transfer(transferId, sourceAccountId, targetAccountId, amount);

        try {
            // Step 1: Validate accounts
            Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new AccountNotFoundException(sourceAccountId));

            Account targetAccount = accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new AccountNotFoundException(targetAccountId));

            // Step 2: Validate sufficient funds
            if (!sourceAccount.hasSufficientFunds(amount)) {
                throw new InsufficientFundsException(sourceAccountId);
            }

            // Step 3: Debit source account
            sourceAccount.debit(amount);
            accountRepository.save(sourceAccount);
            transfer.setStatus(Transfer.TransferStatus.DEBITED);

            // Step 4: Credit target account
            try {
                targetAccount.credit(amount);
                accountRepository.save(targetAccount);
            } catch (Exception e) {
                // Rollback: restore source account
                rollbackDebit(sourceAccount, amount);
                throw new TransferException("Failed to credit target account", e);
            }

            // Success
            transfer.setStatus(Transfer.TransferStatus.COMPLETED);
            transfer.setCompletedAt(Instant.now());

        } catch (TransferException e) {
            transfer.setStatus(Transfer.TransferStatus.FAILED);
            transfer.setFailureReason(e.getMessage());
            throw e;
        }

        return transfer;
    }

    private void rollbackDebit(Account account, BigDecimal amount) {
        account.credit(amount);
        accountRepository.save(account);
    }
}
