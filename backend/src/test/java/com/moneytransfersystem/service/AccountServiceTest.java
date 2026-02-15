package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private String testAccountId;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID().toString();
        testPassword = "testPassword123";
        testAccount = Account.builder()
                .id(testAccountId)
                .holderName("John Doe")
                .balance(BigDecimal.valueOf(1000.00))
                .status(AccountStatus.ACTIVE)
                .password("encodedPassword")
                .build();
    }

    @Test
    @DisplayName("Should find account by ID successfully")
    void testFindById_Success() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        Optional<Account> result = accountService.findById(testAccountId);

        assertTrue(result.isPresent());
        assertEquals(testAccountId, result.get().getId());
        assertEquals("John Doe", result.get().getHolderName());
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    @Test
    @DisplayName("Should return empty Optional when account not found")
    void testFindById_NotFound() {
        when(accountRepository.findById(anyString())).thenReturn(Optional.empty());

        Optional<Account> result = accountService.findById("nonExistentId");

        assertFalse(result.isPresent());
        verify(accountRepository, times(1)).findById("nonExistentId");
    }

    @Test
    @DisplayName("Should create account with encoded password")
    void testCreateAccount_Success() {
        String encodedPassword = "encoded" + testPassword;
        when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account result = accountService.createAccount("John Doe", BigDecimal.valueOf(1000.00), testPassword);

        assertNotNull(result);
        assertEquals("John Doe", result.getHolderName());
        assertEquals(BigDecimal.valueOf(1000.00), result.getBalance());
        assertEquals(AccountStatus.ACTIVE, result.getStatus());
        verify(passwordEncoder, times(1)).encode(testPassword);
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should authenticate account successfully with correct password")
    void testAuthenticate_Success() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, "encodedPassword")).thenReturn(true);

        boolean result = accountService.authenticate(testAccountId, testPassword);

        assertTrue(result);
        verify(accountRepository, times(1)).findById(testAccountId);
        verify(passwordEncoder, times(1)).matches(testPassword, "encodedPassword");
    }

    @Test
    @DisplayName("Should fail authentication with incorrect password")
    void testAuthenticate_Failure() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, "encodedPassword")).thenReturn(false);

        boolean result = accountService.authenticate(testAccountId, testPassword);

        assertFalse(result);
        verify(passwordEncoder, times(1)).matches(testPassword, "encodedPassword");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when authenticating non-existent account")
    void testAuthenticate_AccountNotFound() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> accountService.authenticate(testAccountId, testPassword));
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    @Test
    @DisplayName("Should get account successfully")
    void testGetAccount_Success() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        Account result = accountService.getAccount(testAccountId);

        assertNotNull(result);
        assertEquals(testAccountId, result.getId());
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when getting non-existent account")
    void testGetAccount_NotFound() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> accountService.getAccount(testAccountId));
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    @Test
    @DisplayName("Should load user by username successfully")
    void testLoadUserByUsername_Success() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        var userDetails = accountService.loadUserByUsername(testAccountId);

        assertNotNull(userDetails);
        assertEquals(testAccountId, userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException for non-existent account")
    void testLoadUserByUsername_NotFound() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> accountService.loadUserByUsername(testAccountId));
    }

    @Test
    @DisplayName("Should credit account successfully")
    void testCredit_Success() {
        BigDecimal creditAmount = BigDecimal.valueOf(500.00);
        
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.credit(testAccountId, creditAmount);

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when crediting non-existent account")
    void testCredit_AccountNotFound() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> accountService.credit(testAccountId, BigDecimal.valueOf(100.00)));
    }

    @Test
    @DisplayName("Should debit account successfully")
    void testDebit_Success() {
        BigDecimal debitAmount = BigDecimal.valueOf(100.00);
        
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.debit(testAccountId, debitAmount);

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionLogRepository, times(1)).save(any(TransactionLog.class));
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when debiting non-existent account")
    void testDebit_AccountNotFound() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> accountService.debit(testAccountId, BigDecimal.valueOf(100.00)));
    }

    @Test
    @DisplayName("Should close account successfully")
    void testCloseAccount_Success() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.closeAccount(testAccountId);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(AccountStatus.CLOSED, accountCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when closing non-existent account")
    void testCloseAccount_AccountNotFound() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, 
            () -> accountService.closeAccount(testAccountId));
    }
}
