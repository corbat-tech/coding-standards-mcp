package com.example.transfer.domain.port;

import com.example.transfer.domain.Account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(String accountId);
    void save(Account account);
}
