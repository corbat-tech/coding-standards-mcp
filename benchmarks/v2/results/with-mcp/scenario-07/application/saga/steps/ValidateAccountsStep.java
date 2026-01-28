package com.example.transfer.application.saga.steps;

import com.example.transfer.application.saga.SagaStep;
import com.example.transfer.application.saga.TransferContext;
import com.example.transfer.domain.Account;
import com.example.transfer.domain.AccountNotFoundException;
import com.example.transfer.domain.port.AccountRepository;

public class ValidateAccountsStep implements SagaStep<TransferContext> {
    private final AccountRepository accountRepository;

    public ValidateAccountsStep(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void execute(TransferContext context) {
        Account source = accountRepository.findById(context.getSourceAccountId())
            .orElseThrow(() -> new AccountNotFoundException(context.getSourceAccountId()));

        Account target = accountRepository.findById(context.getTargetAccountId())
            .orElseThrow(() -> new AccountNotFoundException(context.getTargetAccountId()));

        context.setSourceAccount(source);
        context.setTargetAccount(target);
    }

    @Override
    public void compensate(TransferContext context) {
        // No compensation needed for validation
    }

    @Override
    public String getName() {
        return "ValidateAccounts";
    }
}
