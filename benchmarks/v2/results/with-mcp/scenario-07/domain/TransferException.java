package com.example.transfer.domain;

public class TransferException extends RuntimeException {
    private final String transferId;

    public TransferException(String transferId, String message) {
        super(message);
        this.transferId = transferId;
    }

    public TransferException(String transferId, String message, Throwable cause) {
        super(message, cause);
        this.transferId = transferId;
    }

    public String getTransferId() {
        return transferId;
    }
}
