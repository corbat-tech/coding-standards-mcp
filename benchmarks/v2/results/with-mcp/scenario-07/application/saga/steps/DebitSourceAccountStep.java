package com.example.transfer.application.saga.steps;

import com.example.transfer.application.saga.SagaStep;
import com.example.transfer.application.saga.TransferContext;
import com.example.transfer.domain.Account;
import com.example.transfer.domain.port.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebitSourceAccountStep implements SagaStep<TransferContext> {
    private static final Logger log = LoggerFactory.getLogger(DebitSourceAccountStep.class);

    private final AccountRepository accountRepository;

    public DebitSourceAccountStep(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void execute(TransferContext context) {
        Account debited = context.getSourceAccount().debit(context.getAmount());
        accountRepository.save(debited);
        context.setSourceAccount(debited);
        context.setSourceDebited(true);

        log.info("Debited {} from account {}", context.getAmount(), context.getSourceAccountId());
    }

    @Override
    public void compensate(TransferContext context) {
        if (!context.isSourceDebited()) {
            return;
        }

        Account credited = context.getSourceAccount().credit(context.getAmount());
        accountRepository.save(credited);
        context.setSourceAccount(credited);
        context.setSourceDebited(false);

        log.info("Compensation: Credited {} back to account {}",
            context.getAmount(), context.getSourceAccountId());
    }

    @Override
    public String getName() {
        return "DebitSourceAccount";
    }
}
