package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public AccountService(AccountRepository accountRepository,
                          TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    public Account createAccount(String holderName, BigDecimal initialBalance) {
        Account account = Account.create(holderName, initialBalance, AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    @Transactional
    public void credit(String accountId, BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        try {
            Account account = getAccount(accountId);
            account.credit(amount);
            accountRepository.save(account);

            TransactionLog successLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(null) // credit has no source account
                    .toAccountId(accountId)
                    .amount(amount)
                    .status(TransactionStatus.SUCCESS)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .failureReason(null)
                    .createdOn(Instant.now())
                    .build();

            transactionLogRepository.save(successLog);

        } catch (Exception ex) {
            TransactionLog failedLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(null)
                    .toAccountId(accountId)
                    .amount(amount)
                    .status(TransactionStatus.FAILED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .failureReason(ex.getMessage())
                    .createdOn(Instant.now())
                    .build();

            transactionLogRepository.save(failedLog);
            throw ex;
        }
    }

    @Transactional
    public void debit(String accountId, BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        try {
            Account account = getAccount(accountId);
            account.debit(amount);
            accountRepository.save(account);

            TransactionLog successLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(accountId)
                    .toAccountId(null) // debit has no destination account
                    .amount(amount)
                    .status(TransactionStatus.SUCCESS)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .failureReason(null)
                    .createdOn(Instant.now())
                    .build();

            transactionLogRepository.save(successLog);

        } catch (Exception ex) {
            TransactionLog failedLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(accountId)
                    .toAccountId(null)
                    .amount(amount)
                    .status(TransactionStatus.FAILED)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .failureReason(ex.getMessage())
                    .createdOn(Instant.now())
                    .build();

            transactionLogRepository.save(failedLog);
            throw ex;
        }
    }
    @Transactional
    public void closeAccount(String accountId) {
        Account account = getAccount(accountId);
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }
}
