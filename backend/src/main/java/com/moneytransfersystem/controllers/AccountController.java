package com.moneytransfersystem.controllers;

import com.moneytransfersystem.domain.entities.Account;
import com.moneytransfersystem.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        return accountService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("holderName");
        String rawPassword = (String) payload.get("password");

        BigDecimal initialBalance;
        Object balanceObj = payload.get("balance");

        if (balanceObj instanceof Integer integer) {
            initialBalance = BigDecimal.valueOf(integer);
        } else if (balanceObj instanceof Double dbl) {
            initialBalance = BigDecimal.valueOf(dbl);
        } else {
            initialBalance = new BigDecimal(balanceObj.toString());
        }

        Account newAccount = accountService.createAccount(name, initialBalance, rawPassword);
        return ResponseEntity.ok(newAccount);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> payload) {
        String accountId = payload.get("accountId");
        String password = payload.get("password");

        boolean authenticated = accountService.authenticate(accountId, password);

        if (authenticated) {
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "accountId", accountId
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                    "message", "Invalid credentials"
            ));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> payload) {
        String accountId = payload.get("accountId");
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        try {
            accountService.changePassword(accountId, currentPassword, newPassword);
            return ResponseEntity.ok(Map.of(
                    "message", "Password changed successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "message", "Current password is incorrect"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to change password"
            ));
        }
    }
}
