package com.moneytransfersystem.domain.entities;

import com.moneytransfersystem.domain.enums.AccountStatus;
import com.moneytransfersystem.constants.AppConstants;
import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Account {
    private static final Logger logger =
            LoggerFactory.getLogger(Account.class);

    @Id
    @Column(name = "account_id", length = 64, nullable = false, updatable = false)
    private String id;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "balance", precision = 19, scale = 4, nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Version
    private Long version;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    // NEW FIELD: password for authentication
    @Column(name = "password", nullable = false)
    private String password;


    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        lastUpdated = Instant.now();
    }

    public static Account create(String holderName, BigDecimal initialBalance, AccountStatus status, String rawPassword) {
        return Account.builder()
                .id(UUID.randomUUID().toString())
                .holderName(holderName)
                .balance(initialBalance)
                .status(status)
                .lastUpdated(Instant.now())
                .password(rawPassword) // store encoded password here
                .build();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error(
                    "Debit failed | class={} | method=debit | accountId={} | reason=INVALID_AMOUNT | requested={}",
                    this.getClass().getSimpleName(), id, amount
            );
            throw new IllegalArgumentException(AppConstants.INVALID_DEBIT_AMOUNT);
        }

        if (!isActive()) {
            logger.error(
                    "Debit failed | class={} | method=debit | accountId={} | reason=ACCOUNT_INACTIVE",
                    this.getClass().getSimpleName(), id
            );
            throw new IllegalStateException(AppConstants.ACCOUNT_NOT_ACTIVE);
        }

        if (balance.compareTo(amount) < 0) {
            logger.error(
                    "Debit failed | class={} | method=debit | accountId={} | balance={} | requested={} | reason=INSUFFICIENT_BALANCE",
                    this.getClass().getSimpleName(), id, balance, amount
            );
            throw new IllegalArgumentException(AppConstants.INSUFFICIENT_BALANCE);
        }

        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error(
                    "Credit failed | class={} | method=credit | accountId={} | reason=INVALID_AMOUNT | requested={}",
                    this.getClass().getSimpleName(), id, amount
            );
            throw new IllegalArgumentException(AppConstants.INVALID_CREDIT_AMOUNT);
        }

        if (!isActive()) {
            logger.error(
                    "Credit failed | class={} | method=credit | accountId={} | reason=ACCOUNT_INACTIVE",
                    this.getClass().getSimpleName(), id
            );
            throw new IllegalStateException(AppConstants.ACCOUNT_NOT_ACTIVE);
        }

        balance = balance.add(amount);
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

}
