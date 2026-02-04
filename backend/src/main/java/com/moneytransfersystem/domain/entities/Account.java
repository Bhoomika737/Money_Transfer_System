package com.moneytransfersystem.domain.entities;

import com.moneytransfersystem.domain.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
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

    /**
     * Factory method to create a new account with a generated UUID.
     */
    public static Account create(String holderName, BigDecimal initialBalance, AccountStatus status) {
        return Account.builder()
                .id(UUID.randomUUID().toString())
                .holderName(holderName)
                .balance(initialBalance)
                .status(status)
                .lastUpdated(Instant.now())
                .build();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        if (!isActive()) {
            throw new IllegalStateException("Account is not active");
        }
        balance = balance.subtract(amount);
        lastUpdated = Instant.now();
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        if (!isActive()) {
            throw new IllegalStateException("Account is not active");
        }
        balance = balance.add(amount);
        lastUpdated = Instant.now();
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}
