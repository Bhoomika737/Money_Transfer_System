package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.DuplicateTranferException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Unit Tests")
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private TransferService transferService;

    private Account fromAccount;
    private Account toAccount;
    private TransferRequest transferRequest;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        String fromAccountId = UUID.randomUUID().toString();
        String toAccountId = UUID.randomUUID().toString();
        idempotencyKey = UUID.randomUUID().toString();

        fromAccount = Account.builder()
                .id(fromAccountId)
                .holderName("Sender")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .password("password")
                .build();

        toAccount = Account.builder()
                .id(toAccountId)
                .holderName("Receiver")
                .balance(BigDecimal.valueOf(500.00))
                .status(AccountStatus.ACTIVE)
                .password("password")
                .build();

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(fromAccountId);
        transferRequest.setToAccountId(toAccountId);
        transferRequest.setAmount(BigDecimal.valueOf(100.00));
        transferRequest.setRemarks("Test transfer");
        transferRequest.setIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("Should transfer successfully between accounts")
    void testTransfer_Success() {
        BigDecimal initialFromBalance = fromAccount.getBalance();
        BigDecimal initialToBalance = toAccount.getBalance();
        BigDecimal transferAmount = transferRequest.getAmount();

        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionLog result = transferService.transfer(transferRequest);

        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(fromAccount.getId(), result.getFromAccountId());
        assertEquals(toAccount.getId(), result.getToAccountId());
        assertEquals(transferAmount, result.getAmount());
        
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("Should reject duplicate transfer with same idempotency key")
    void testTransfer_DuplicateTransaction() {
        TransactionLog existingLog = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(transferRequest.getAmount())
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .createdOn(Instant.now())
                .build();

        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingLog));

        assertThrows(DuplicateTranferException.class, 
            () -> transferService.transfer(transferRequest));
        
        verify(accountRepository, never()).save(any());
        verify(transactionLogRepository, never()).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when transferring to same account")
    void testTransfer_SameAccount() {
        transferRequest.setToAccountId(transferRequest.getFromAccountId());
        
        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, 
            () -> transferService.transfer(transferRequest));
        
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when sender account not found")
    void testTransfer_SenderNotFound() {
        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccount.getId()))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> transferService.transfer(transferRequest));
        
        verify(transactionLogRepository, times(1)).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when receiver account not found")
    void testTransfer_ReceiverNotFound() {
        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId()))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> transferService.transfer(transferRequest));
    }

    @Test
    @DisplayName("Should retrieve transaction history for account")
    void testGetTransactionHistory_Success() {
        String accountId = fromAccount.getId();
        List<TransactionLog> transactionHistoryList = new ArrayList<>();
        
        TransactionLog log1 = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(accountId)
                .toAccountId(toAccount.getId())
                .amount(BigDecimal.valueOf(100.00))
                .status(TransactionStatus.SUCCESS)
                .createdOn(Instant.now())
                .build();
        
        TransactionLog log2 = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(toAccount.getId())
                .toAccountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .status(TransactionStatus.SUCCESS)
                .createdOn(Instant.now().minusSeconds(3600))
                .build();
        
        transactionHistoryList.add(log1);
        transactionHistoryList.add(log2);

        when(transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(
                accountId, accountId))
                .thenReturn(transactionHistoryList);

        List<TransactionLog> result = transferService.getTransactionHistory(accountId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(log1.getId(), result.get(0).getId());
        assertEquals(log2.getId(), result.get(1).getId());
        
        verify(transactionLogRepository, times(1))
                .findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(accountId, accountId);
    }

    @Test
    @DisplayName("Should return empty list when no transaction history exists")
    void testGetTransactionHistory_Empty() {
        String accountId = fromAccount.getId();
        
        when(transactionLogRepository.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(
                accountId, accountId))
                .thenReturn(new ArrayList<>());

        List<TransactionLog> result = transferService.getTransactionHistory(accountId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transactionLogRepository, times(1))
                .findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(accountId, accountId);
    }

    @Test
    @DisplayName("Should update account balances correctly after transfer")
    void testTransfer_BalanceUpdate() {
        BigDecimal initialFromBalance = fromAccount.getBalance();
        BigDecimal initialToBalance = toAccount.getBalance();
        BigDecimal transferAmount = transferRequest.getAmount();

        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transferService.transfer(transferRequest);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(accountCaptor.capture());
        
        List<Account> savedAccounts = accountCaptor.getAllValues();
        assertEquals(2, savedAccounts.size());
    }

    @Test
    @DisplayName("Should save transaction log with correct details")
    void testTransfer_TransactionLogDetails() {
        when(transactionLogRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccount.getId()))
                .thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(toAccount.getId()))
                .thenReturn(Optional.of(toAccount));
        when(transactionLogRepository.save(any(TransactionLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionLog result = transferService.transfer(transferRequest);

        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(fromAccount.getId(), savedLog.getFromAccountId());
        assertEquals(toAccount.getId(), savedLog.getToAccountId());
        assertEquals(transferRequest.getAmount(), savedLog.getAmount());
        assertEquals(TransactionStatus.SUCCESS, savedLog.getStatus());
        assertEquals(idempotencyKey, savedLog.getIdempotencyKey());
    }
}
