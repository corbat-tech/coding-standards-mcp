package com.example.bank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    private InMemoryAccountRepository accountRepository;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        transferService = new TransferService(accountRepository);
    }

    @Test
    void execute_SuccessfulTransfer() {
        Account source = new Account("source-1", "Alice", new BigDecimal("1000.00"));
        Account target = new Account("target-1", "Bob", new BigDecimal("500.00"));
        accountRepository.addAccount(source);
        accountRepository.addAccount(target);

        Transfer transfer = transferService.execute("source-1", "target-1", new BigDecimal("200.00"));

        assertEquals(Transfer.TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(new BigDecimal("800.00"), source.getBalance());
        assertEquals(new BigDecimal("700.00"), target.getBalance());
    }

    @Test
    void execute_SourceAccountNotFound() {
        Account target = new Account("target-1", "Bob", new BigDecimal("500.00"));
        accountRepository.addAccount(target);

        assertThrows(AccountNotFoundException.class, () -> {
            transferService.execute("non-existent", "target-1", new BigDecimal("100.00"));
        });
    }

    @Test
    void execute_TargetAccountNotFound() {
        Account source = new Account("source-1", "Alice", new BigDecimal("1000.00"));
        accountRepository.addAccount(source);

        assertThrows(AccountNotFoundException.class, () -> {
            transferService.execute("source-1", "non-existent", new BigDecimal("100.00"));
        });
    }

    @Test
    void execute_InsufficientFunds() {
        Account source = new Account("source-1", "Alice", new BigDecimal("100.00"));
        Account target = new Account("target-1", "Bob", new BigDecimal("500.00"));
        accountRepository.addAccount(source);
        accountRepository.addAccount(target);

        assertThrows(InsufficientFundsException.class, () -> {
            transferService.execute("source-1", "target-1", new BigDecimal("200.00"));
        });

        // Verify balances unchanged
        assertEquals(new BigDecimal("100.00"), source.getBalance());
        assertEquals(new BigDecimal("500.00"), target.getBalance());
    }

    @Test
    void execute_ExactBalance() {
        Account source = new Account("source-1", "Alice", new BigDecimal("100.00"));
        Account target = new Account("target-1", "Bob", new BigDecimal("0.00"));
        accountRepository.addAccount(source);
        accountRepository.addAccount(target);

        Transfer transfer = transferService.execute("source-1", "target-1", new BigDecimal("100.00"));

        assertEquals(Transfer.TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), source.getBalance().setScale(2));
        assertEquals(new BigDecimal("100.00"), target.getBalance());
    }

    @Test
    void execute_TransferCreatesCompletedTimestamp() {
        Account source = new Account("source-1", "Alice", new BigDecimal("1000.00"));
        Account target = new Account("target-1", "Bob", new BigDecimal("500.00"));
        accountRepository.addAccount(source);
        accountRepository.addAccount(target);

        Transfer transfer = transferService.execute("source-1", "target-1", new BigDecimal("100.00"));

        assertNotNull(transfer.getCompletedAt());
    }
}
