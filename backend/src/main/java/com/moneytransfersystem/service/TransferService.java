package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public TransferService(AccountRepository accountRepository,
                           TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Transactional
    public TransactionLog transfer(String fromAccountId,
                                   String toAccountId,
                                   BigDecimal amount,
                                   String idempotencyKey) {

        validateTransfer(fromAccountId, toAccountId, amount);

        String transactionId = UUID.randomUUID().toString();

        try {
            Account fromAccount = accountRepository.findById(fromAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Sender account not found"));

            Account toAccount = accountRepository.findById(toAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Receiver account not found"));

            fromAccount.debit(amount);
            toAccount.credit(amount);

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            TransactionLog successLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(fromAccountId)
                    .toAccountId(toAccountId)
                    .amount(amount)
                    .status(TransactionStatus.SUCCESS)
                    .createdOn(Instant.now())
                    .idempotencyKey(idempotencyKey)
                    .failureReason(null)
                    .build();

            return transactionLogRepository.save(successLog);

        } catch (Exception ex) {

            TransactionLog failedLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(fromAccountId)
                    .toAccountId(toAccountId)
                    .amount(amount)
                    .status(TransactionStatus.FAILED)
                    .idempotencyKey(idempotencyKey)
                    .failureReason(ex.getMessage())
                    .createdOn(Instant.now())
                    .build();

            transactionLogRepository.save(failedLog);
            throw ex;
        }
    }

    private void validateTransfer(String fromAccountId,
                                  String toAccountId,
                                  BigDecimal amount) {

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
    }
}
