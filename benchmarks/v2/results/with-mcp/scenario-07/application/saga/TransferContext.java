package com.example.transfer.application.saga;

import com.example.transfer.domain.Account;
import com.example.transfer.domain.Transfer;

import java.math.BigDecimal;

public class TransferContext {
    private final String transferId;
    private final String sourceAccountId;
    private final String targetAccountId;
    private final BigDecimal amount;

    private Transfer transfer;
    private Account sourceAccount;
    private Account targetAccount;
    private boolean sourceDebited;
    private boolean targetCredited;

    public TransferContext(
        String transferId,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount
    ) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.sourceDebited = false;
        this.targetCredited = false;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public String getTargetAccountId() {
        return targetAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(Account targetAccount) {
        this.targetAccount = targetAccount;
    }

    public boolean isSourceDebited() {
        return sourceDebited;
    }

    public void setSourceDebited(boolean sourceDebited) {
        this.sourceDebited = sourceDebited;
    }

    public boolean isTargetCredited() {
        return targetCredited;
    }

    public void setTargetCredited(boolean targetCredited) {
        this.targetCredited = targetCredited;
    }
}
