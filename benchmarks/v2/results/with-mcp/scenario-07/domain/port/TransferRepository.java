package com.example.transfer.domain.port;

import com.example.transfer.domain.Transfer;

import java.util.Optional;

public interface TransferRepository {
    void save(Transfer transfer);
    Optional<Transfer> findById(String transferId);
}
