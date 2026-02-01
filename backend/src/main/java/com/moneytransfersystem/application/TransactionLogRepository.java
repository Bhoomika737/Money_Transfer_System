// src/main/java/com/moneytransfersystem/application/TransactionLogRepository.java
package com.moneytransfersystem.application;

import com.moneytransfersystem.domain.entities.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);
}
