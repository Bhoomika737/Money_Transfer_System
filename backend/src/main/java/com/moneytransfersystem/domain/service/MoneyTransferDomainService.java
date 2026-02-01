package com.moneytransfersystem.domain.service;

import com.moneytransfersystem.application.AccountRepository;
import com.moneytransfersystem.application.TransactionLogRepository;
import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.dtos.TransferResponse;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class MoneyTransferDomainService {

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public MoneyTransferDomainService(AccountRepository accountRepository,
                                      TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        BigDecimal amount = request.getAmount();

        // NEW: check for negative or zero transfer amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // idempotency check
        if (request.getIdempotencyKey() != null) {
            Optional<TransactionLog> existing = transactionLogRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                TransactionLog log = existing.get();
                return new TransferResponse(log.getId(), log.getStatus(), log.getAmount(),
                        log.getStatus() == TransactionStatus.SUCCESS ? "Already processed" : "Previously failed");
            }
        }

        Account from = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccountId()));
        Account to = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccountId()));

        if (!from.isActive()) throw new AccountNotActiveException(from.getId());
        if (!to.isActive()) throw new AccountNotActiveException(to.getId());

        if (from.getBalance().compareTo(amount) < 0) {
            TransactionLog failed = new TransactionLog(from.getId(), to.getId(), amount,
                    TransactionStatus.FAILED, request.getIdempotencyKey());
            failed.setFailureReason("Insufficient balance");
            transactionLogRepository.save(failed);
            throw new InsufficentBalanceException(from.getId(), amount, from.getBalance());
        }

        from.debit(amount);
        to.credit(amount);

        accountRepository.save(from);
        accountRepository.save(to);

        TransactionLog log = new TransactionLog(from.getId(), to.getId(), amount,
                TransactionStatus.SUCCESS, request.getIdempotencyKey());
        transactionLogRepository.save(log);

        return new TransferResponse(log.getId(), TransactionStatus.SUCCESS, amount, "Transfer successful");
    }
}
