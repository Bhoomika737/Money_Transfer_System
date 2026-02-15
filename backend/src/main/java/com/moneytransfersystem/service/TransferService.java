package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.DuplicateTranferException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import com.moneytransfersystem.constants.AppConstants;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {
    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;

    public TransferService(AccountRepository accountRepository, TransactionLogRepository transactionLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
    }

    public List<TransactionLog> getTransactionHistory(String accountId) {
        return transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(accountId, accountId);
    }

    @Transactional
    public TransactionLog transfer(TransferRequest request) {
        String transactionId = UUID.randomUUID().toString();

        try {
            Optional<TransactionLog> existing =
                    transactionLogRepository.findByIdempotencyKey(request.getIdempotencyKey());

            if (existing.isPresent()) {
                logger.error(
                        "Transfer rejected | class={} | method=transfer | idempotencyKey={} | reason=DUPLICATE_TRANSACTION",
                        this.getClass().getSimpleName(),
                        request.getIdempotencyKey()
                );
                throw new DuplicateTranferException(AppConstants.DUPLICATE_TRANSACTION);
            }

            validateTransfer(request);

            Account fromAccount = accountRepository.findById(request.getFromAccountId())
                    .orElseThrow(() -> {
                        logger.error(
                                "Transfer failed | class={} | method=transfer | txId={} | fromAccountId={} | reason=SENDER_NOT_FOUND",
                                this.getClass().getSimpleName(),
                                transactionId,
                                request.getFromAccountId()
                        );
                        return new AccountNotFoundException(AppConstants.SENDER_ACCOUNT_NOT_FOUND);
                    });
            Account toAccount = accountRepository.findById(request.getToAccountId())
                    .orElseThrow(() -> {
                        logger.error(
                                "Transfer failed | class={} | method=transfer | txId={} | toAccountId={} | reason=RECEIVER_NOT_FOUND",
                                this.getClass().getSimpleName(),
                                transactionId,
                                request.getToAccountId()
                        );
                        return new AccountNotFoundException(AppConstants.RECEIVER_ACCOUNT_NOT_FOUND);
                    });

            fromAccount.debit(request.getAmount());
            toAccount.credit(request.getAmount());

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            TransactionLog successLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .status(TransactionStatus.SUCCESS)
                    .createdOn(Instant.now())
                    .idempotencyKey(request.getIdempotencyKey())
                    .remarks(request.getRemarks())
                    .build();

            transactionLogRepository.save(successLog);

            logger.info(
                    "Transfer success | class={} | method=transfer | txId={} | fromAccountId={} | toAccountId={} | amount={}",
                    this.getClass().getSimpleName(),
                    transactionId,
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    request.getAmount()
            );

            return successLog;

        } catch (DuplicateTranferException e) {
            // For duplicate transactions, re-throw without saving (transaction already exists)
            logger.warn("Duplicate transaction detected with idempotencyKey={}", request.getIdempotencyKey());
            throw e;
        } catch (IllegalArgumentException | AccountNotFoundException e) {
            // Validation or not-found errors should be propagated to callers/tests
            logger.warn("Transfer validation/not-found error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Log and save failed transaction with failure reason for unexpected errors
            String failureReason = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            
            logger.error(
                    "Transfer failed | class={} | method=transfer | txId={} | fromAccountId={} | toAccountId={} | reason={}",
                    this.getClass().getSimpleName(),
                    transactionId,
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    failureReason
            );

            // Save failed transaction with remarks containing the error
            String remarks = request.getRemarks() != null && !request.getRemarks().isEmpty() 
                    ? request.getRemarks() + " | Error: " + failureReason
                    : "Error: " + failureReason;

            TransactionLog failedLog = TransactionLog.builder()
                    .id(transactionId)
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .status(TransactionStatus.FAILED)
                    .failureReason(failureReason)
                    .createdOn(Instant.now())
                    .idempotencyKey(request.getIdempotencyKey())
                    .remarks(remarks)
                    .build();

            transactionLogRepository.save(failedLog);

            // Return the failed transaction (don't throw) so it's sent to frontend
            return failedLog;
        }
    }

    private void validateTransfer(TransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            logger.error(
                    "Transfer validation failed | class={} | method=validateTransfer | accountId={} | reason=SAME_ACCOUNT",
                    this.getClass().getSimpleName(),
                    request.getFromAccountId()
            );
            throw new IllegalArgumentException(
                    AppConstants.SAME_ACCOUNT_TRANSFER
            );
        }
    }
}