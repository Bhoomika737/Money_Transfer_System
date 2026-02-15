package com.moneytransfersystem.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.DuplicateTranferException;
import com.moneytransfersystem.exception.GlobalExceptionHandler;
import com.moneytransfersystem.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferController Unit Tests")
class TransferControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    private ObjectMapper objectMapper;
    private TransferRequest transferRequest;
    private String fromAccountId;
    private String toAccountId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(transferController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        this.objectMapper = new ObjectMapper();

        fromAccountId = UUID.randomUUID().toString();
        toAccountId = UUID.randomUUID().toString();
        idempotencyKey = UUID.randomUUID().toString();

        transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(fromAccountId);
        transferRequest.setToAccountId(toAccountId);
        transferRequest.setAmount(BigDecimal.valueOf(100.00));
        transferRequest.setRemarks("Test transfer");
        transferRequest.setIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("Should transfer successfully with valid request")
    void testTransfer_Success() throws Exception {
        TransactionLog transactionLog = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(BigDecimal.valueOf(100.00))
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .createdOn(Instant.now())
                .remarks("Test transfer")
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenReturn(transactionLog);

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccountId", equalTo(fromAccountId)))
                .andExpect(jsonPath("$.toAccountId", equalTo(toAccountId)))
                .andExpect(jsonPath("$.amount", equalTo(100.00)))
                .andExpect(jsonPath("$.status", equalTo("SUCCESS")))
                .andExpect(jsonPath("$.remarks", equalTo("Test transfer")));

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should handle missing idempotency key validation")
    void testTransfer_MissingIdempotencyKey() throws Exception {
        TransferRequest invalidRequest = new TransferRequest();
        invalidRequest.setFromAccountId(fromAccountId);
        invalidRequest.setToAccountId(toAccountId);
        invalidRequest.setAmount(BigDecimal.valueOf(100.00));
        // idempotencyKey is missing

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should handle missing fromAccountId validation")
    void testTransfer_MissingFromAccountId() throws Exception {
        TransferRequest invalidRequest = new TransferRequest();
        invalidRequest.setToAccountId(toAccountId);
        invalidRequest.setAmount(BigDecimal.valueOf(100.00));
        invalidRequest.setIdempotencyKey(idempotencyKey);

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should handle missing toAccountId validation")
    void testTransfer_MissingToAccountId() throws Exception {
        TransferRequest invalidRequest = new TransferRequest();
        invalidRequest.setFromAccountId(fromAccountId);
        invalidRequest.setAmount(BigDecimal.valueOf(100.00));
        invalidRequest.setIdempotencyKey(idempotencyKey);

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should handle invalid amount validation")
    void testTransfer_InvalidAmount() throws Exception {
        TransferRequest invalidRequest = new TransferRequest();
        invalidRequest.setFromAccountId(fromAccountId);
        invalidRequest.setToAccountId(toAccountId);
        invalidRequest.setAmount(BigDecimal.valueOf(-100.00));
        invalidRequest.setIdempotencyKey(idempotencyKey);

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should handle missing amount validation")
    void testTransfer_MissingAmount() throws Exception {
        TransferRequest invalidRequest = new TransferRequest();
        invalidRequest.setFromAccountId(fromAccountId);
        invalidRequest.setToAccountId(toAccountId);
        invalidRequest.setIdempotencyKey(idempotencyKey);

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(transferService, never()).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should get transaction history successfully")
    void testGetHistory_Success() throws Exception {
        String accountId = fromAccountId;
        List<TransactionLog> transactions = new ArrayList<>();

        TransactionLog log1 = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(BigDecimal.valueOf(100.00))
                .status(TransactionStatus.SUCCESS)
                .createdOn(Instant.now())
                .build();

        TransactionLog log2 = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(toAccountId)
                .toAccountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .status(TransactionStatus.SUCCESS)
                .createdOn(Instant.now().minusSeconds(3600))
                .build();

        transactions.add(log1);
        transactions.add(log2);

        when(transferService.getTransactionHistory(accountId))
                .thenReturn(transactions);

        mockMvc.perform(get("/api/transfers/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].amount", equalTo(100.00)))
                .andExpect(jsonPath("$[1].amount", equalTo(50.00)));

        verify(transferService, times(1)).getTransactionHistory(accountId);
    }

    @Test
    @DisplayName("Should return empty list for transaction history")
    void testGetHistory_Empty() throws Exception {
        String accountId = fromAccountId;

        when(transferService.getTransactionHistory(accountId))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/transfers/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(transferService, times(1)).getTransactionHistory(accountId);
    }

    @Test
    @DisplayName("Should handle duplicate transfer exception")
    void testTransfer_DuplicateTransaction() throws Exception {
        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new DuplicateTranferException("Duplicate transaction"));

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isConflict());

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should handle account not found exception")
    void testTransfer_AccountNotFound() throws Exception {
        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isNotFound());

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should handle illegal argument exception")
    void testTransfer_SameAccountTransfer() throws Exception {
        when(transferService.transfer(any(TransferRequest.class)))
                .thenThrow(new IllegalArgumentException("Cannot transfer to same account"));

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());

        verify(transferService, times(1)).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Should return transaction log with SUCCESS status")
    void testTransfer_SuccessStatus() throws Exception {
        TransactionLog transactionLog = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(BigDecimal.valueOf(100.00))
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .createdOn(Instant.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenReturn(transactionLog);

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("SUCCESS")));
    }

    @Test
    @DisplayName("Should include idempotency key in response")
    void testTransfer_IdempotencyKeyInResponse() throws Exception {
        TransactionLog transactionLog = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(BigDecimal.valueOf(100.00))
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .createdOn(Instant.now())
                .build();

        when(transferService.transfer(any(TransferRequest.class)))
                .thenReturn(transactionLog);

        mockMvc.perform(post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey", equalTo(idempotencyKey)));
    }

    @Test
    @DisplayName("Should retrieve transaction history in descending order by date")
    void testGetHistory_OrderByCreatedOn() throws Exception {
        String accountId = fromAccountId;
        List<TransactionLog> transactions = new ArrayList<>();

        Instant now = Instant.now();
        
        TransactionLog log1 = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(accountId)
                .toAccountId(toAccountId)
                .amount(BigDecimal.valueOf(100.00))
                .status(TransactionStatus.SUCCESS)
                .createdOn(now)
                .build();

        TransactionLog log2 = TransactionLog.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(toAccountId)
                .toAccountId(accountId)
                .amount(BigDecimal.valueOf(50.00))
                .status(TransactionStatus.SUCCESS)
                .createdOn(now.minusSeconds(3600))
                .build();

        transactions.add(log1);
        transactions.add(log2);

        when(transferService.getTransactionHistory(accountId))
                .thenReturn(transactions);

        mockMvc.perform(get("/api/transfers/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount", equalTo(100.00)))
                .andExpect(jsonPath("$[1].amount", equalTo(50.00)));
    }
}
