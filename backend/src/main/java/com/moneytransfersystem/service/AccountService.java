package com.moneytransfersystem.service;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.domain.enums.TransactionStatus;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.repository.AccountRepository;
import com.moneytransfersystem.repository.TransactionLogRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moneytransfersystem.constants.AppConstants;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {
    private static final Logger logger =
            LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository,
                          TransactionLogRepository transactionLogRepository,
                          PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Account> findById(String id) {
        return accountRepository.findById(id);
    }

    public Account createAccount(String holderName, BigDecimal initialBalance, String rawPassword) {
        Account account = Account.create(holderName, initialBalance, AccountStatus.ACTIVE, passwordEncoder.encode(rawPassword));
        return accountRepository.save(account);
    }

    public boolean authenticate(String accountId, String rawPassword) {
        Account account = getAccount(accountId);
        return passwordEncoder.matches(rawPassword, account.getPassword());
    }

    @Transactional
    public void changePassword(String accountId, String currentPassword, String newPassword) {
        Account account = getAccount(accountId);
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        // Update password
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
        logger.info("Password changed successfully for accountId={}", accountId);
    }


    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found | accountId={}", accountId);
                    return new AccountNotFoundException(
                            AppConstants.ACCOUNT_NOT_FOUND + accountId
                    );
                });
    }

    public UserDetails loadUserByUsername(String accountId) throws UsernameNotFoundException {
        Account account = getAccount(accountId);
        return User.withUsername(account.getId()) .password(account.getPassword()) .roles("USER") .build();
    }

    @Transactional
    public void credit(String accountId, BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        Account account = getAccount(accountId);
        account.credit(amount);
        accountRepository.save(account);

        logSuccess(transactionId, null, accountId, amount);
    }

    @Transactional
    public void debit(String accountId, BigDecimal amount) {
        String transactionId = UUID.randomUUID().toString();
        Account account = getAccount(accountId);
        account.debit(amount);
        accountRepository.save(account);

        logSuccess(transactionId, accountId, null, amount);
    }

    @Transactional
    public void closeAccount(String accountId) {
        Account account = getAccount(accountId);
        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    private void logSuccess(String txId, String from, String to, BigDecimal amount) {
        TransactionLog log = TransactionLog.builder()
                .id(txId)
                .fromAccountId(from)
                .toAccountId(to)
                .amount(amount)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(UUID.randomUUID().toString())
                .createdOn(Instant.now())
                .build();
        transactionLogRepository.save(log);
    }
}
