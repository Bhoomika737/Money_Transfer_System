package com.moneytransfersystem.domain.entities;

import com.moneytransfersystem.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "transaction_logs")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class TransactionLog {

    @Id
    @Column(name = "transaction_id", length = 64)
    private String id;

    @Column(name = "from_account_id")
    private String fromAccountId;

    @Column(name = "to_account_id")
    private String toAccountId;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_on")
    private Instant createdOn;

    @Column(name ="remarks")
    private  String remarks;
}