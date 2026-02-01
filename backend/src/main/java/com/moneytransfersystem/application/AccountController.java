// src/main/java/com/moneytransfersystem/application/AccountController.java
package com.moneytransfersystem.application;

import com.moneytransfersystem.domain.dtos.AccountResponse;
import com.moneytransfersystem.domain.entities.Account;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable("id") String id) {
        return accountRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private AccountResponse toResponse(Account a) {
        AccountResponse r = new AccountResponse();
        r.setAccountId(a.getId());
        r.setHolderName(a.getHolderName());
        r.setBalance(a.getBalance());
        r.setStatus(a.getStatus().name());
        return r;
    }
}
