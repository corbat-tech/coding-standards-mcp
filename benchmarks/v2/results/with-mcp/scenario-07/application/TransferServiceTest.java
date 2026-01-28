package com.example.transfer.application;

import com.example.transfer.domain.Account;
import com.example.transfer.domain.Transfer;
import com.example.transfer.domain.TransferStatus;
import com.example.transfer.domain.port.AccountRepository;
import com.example.transfer.domain.port.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    private InMemoryAccountRepository accountRepository;
    private TransferService service;

    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneId.UTC);

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        service = new TransferService(accountRepository, transferRepository, FIXED_CLOCK);
    }

    @Test
    void should_complete_transfer_when_sufficient_funds() {
        // Arrange
        Account source = new Account("acc-1", "John", BigDecimal.valueOf(1000));
        Account target = new Account("acc-2", "Jane", BigDecimal.valueOf(500));
        accountRepository.save(source);
        accountRepository.save(target);

        // Act
        Transfer result = service.executeTransfer("acc-1", "acc-2", BigDecimal.valueOf(100));

        // Assert
        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(accountRepository.findById("acc-1").get().balance())
            .isEqualByComparingTo(BigDecimal.valueOf(900));
        assertThat(accountRepository.findById("acc-2").get().balance())
            .isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    void should_rollback_when_insufficient_funds() {
        // Arrange
        Account source = new Account("acc-1", "John", BigDecimal.valueOf(50));
        Account target = new Account("acc-2", "Jane", BigDecimal.valueOf(500));
        accountRepository.save(source);
        accountRepository.save(target);

        // Act
        Transfer result = service.executeTransfer("acc-1", "acc-2", BigDecimal.valueOf(100));

        // Assert
        assertThat(result.status()).isEqualTo(TransferStatus.ROLLED_BACK);
        assertThat(accountRepository.findById("acc-1").get().balance())
            .isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(accountRepository.findById("acc-2").get().balance())
            .isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void should_rollback_when_source_account_not_found() {
        // Arrange
        Account target = new Account("acc-2", "Jane", BigDecimal.valueOf(500));
        accountRepository.save(target);

        // Act
        Transfer result = service.executeTransfer("acc-1", "acc-2", BigDecimal.valueOf(100));

        // Assert
        assertThat(result.status()).isEqualTo(TransferStatus.ROLLED_BACK);
        assertThat(result.failureReason()).contains("Account not found");
    }

    @Test
    void should_rollback_when_target_account_not_found() {
        // Arrange
        Account source = new Account("acc-1", "John", BigDecimal.valueOf(1000));
        accountRepository.save(source);

        // Act
        Transfer result = service.executeTransfer("acc-1", "acc-2", BigDecimal.valueOf(100));

        // Assert
        assertThat(result.status()).isEqualTo(TransferStatus.ROLLED_BACK);
        assertThat(result.failureReason()).contains("Account not found");
    }

    @Test
    void should_save_transfer_record_when_completed() {
        // Arrange
        Account source = new Account("acc-1", "John", BigDecimal.valueOf(1000));
        Account target = new Account("acc-2", "Jane", BigDecimal.valueOf(500));
        accountRepository.save(source);
        accountRepository.save(target);

        ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);

        // Act
        service.executeTransfer("acc-1", "acc-2", BigDecimal.valueOf(100));

        // Assert
        verify(transferRepository, atLeast(2)).save(captor.capture());
        Transfer finalTransfer = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalTransfer.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void should_debit_source_before_crediting_target() {
        // Arrange
        Account source = new Account("acc-1", "John", BigDecimal.valueOf(1000));
        Account target = new Account("acc-2", "Jane", BigDecimal.valueOf(500));
        accountRepository.save(source);
        accountRepository.save(target);

        // Act
        service.executeTransfer("acc-1", "acc-2", BigDecimal.valueOf(100));

        // Assert - verify order through final balances
        assertThat(accountRepository.findById("acc-1").get().balance())
            .isEqualByComparingTo(BigDecimal.valueOf(900));
        assertThat(accountRepository.findById("acc-2").get().balance())
            .isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    static class InMemoryAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts = new HashMap<>();

        @Override
        public Optional<Account> findById(String accountId) {
            return Optional.ofNullable(accounts.get(accountId));
        }

        @Override
        public void save(Account account) {
            accounts.put(account.id(), account);
        }
    }
}
