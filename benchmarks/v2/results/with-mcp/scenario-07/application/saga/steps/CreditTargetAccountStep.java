package com.example.transfer.application.saga.steps;

import com.example.transfer.application.saga.SagaStep;
import com.example.transfer.application.saga.TransferContext;
import com.example.transfer.domain.Account;
import com.example.transfer.domain.port.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreditTargetAccountStep implements SagaStep<TransferContext> {
    private static final Logger log = LoggerFactory.getLogger(CreditTargetAccountStep.class);

    private final AccountRepository accountRepository;

    public CreditTargetAccountStep(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void execute(TransferContext context) {
        Account credited = context.getTargetAccount().credit(context.getAmount());
        accountRepository.save(credited);
        context.setTargetAccount(credited);
        context.setTargetCredited(true);

        log.info("Credited {} to account {}", context.getAmount(), context.getTargetAccountId());
    }

    @Override
    public void compensate(TransferContext context) {
        if (!context.isTargetCredited()) {
            return;
        }

        Account debited = context.getTargetAccount().debit(context.getAmount());
        accountRepository.save(debited);
        context.setTargetAccount(debited);
        context.setTargetCredited(false);

        log.info("Compensation: Debited {} from account {}",
            context.getAmount(), context.getTargetAccountId());
    }

    @Override
    public String getName() {
        return "CreditTargetAccount";
    }
}
