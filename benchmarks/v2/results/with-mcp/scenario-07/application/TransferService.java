package com.example.transfer.application;

import com.example.transfer.application.saga.SagaExecutionException;
import com.example.transfer.application.saga.SagaOrchestrator;
import com.example.transfer.application.saga.TransferContext;
import com.example.transfer.application.saga.steps.CreditTargetAccountStep;
import com.example.transfer.application.saga.steps.DebitSourceAccountStep;
import com.example.transfer.application.saga.steps.ValidateAccountsStep;
import com.example.transfer.domain.Transfer;
import com.example.transfer.domain.port.AccountRepository;
import com.example.transfer.domain.port.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final Clock clock;

    public TransferService(
        AccountRepository accountRepository,
        TransferRepository transferRepository,
        Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.clock = clock;
    }

    public Transfer executeTransfer(
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount
    ) {
        String transferId = UUID.randomUUID().toString();
        log.info("Starting transfer {} from {} to {} amount {}",
            transferId, sourceAccountId, targetAccountId, amount);

        Transfer transfer = Transfer.create(
            transferId,
            sourceAccountId,
            targetAccountId,
            amount,
            clock.instant()
        );
        transferRepository.save(transfer);

        TransferContext context = new TransferContext(
            transferId,
            sourceAccountId,
            targetAccountId,
            amount
        );
        context.setTransfer(transfer);

        SagaOrchestrator<TransferContext> saga = createSaga();

        try {
            saga.execute(context);

            Transfer completed = transfer.markCompleted(clock.instant());
            transferRepository.save(completed);

            log.info("Transfer {} completed successfully", transferId);
            return completed;

        } catch (SagaExecutionException e) {
            log.error("Transfer {} failed: {}", transferId, e.getMessage());

            Transfer failed = transfer.markFailed(e.getCause().getMessage(), clock.instant());
            Transfer rolledBack = failed.markRolledBack(clock.instant());
            transferRepository.save(rolledBack);

            return rolledBack;
        }
    }

    private SagaOrchestrator<TransferContext> createSaga() {
        return new SagaOrchestrator<>(List.of(
            new ValidateAccountsStep(accountRepository),
            new DebitSourceAccountStep(accountRepository),
            new CreditTargetAccountStep(accountRepository)
        ));
    }
}
