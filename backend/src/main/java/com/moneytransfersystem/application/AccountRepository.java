// src/main/java/com/moneytransfersystem/application/AccountRepository.java
package com.moneytransfersystem.application;

import com.moneytransfersystem.domain.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
}
