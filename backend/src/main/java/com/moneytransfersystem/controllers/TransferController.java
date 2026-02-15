package com.moneytransfersystem.controllers;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.DuplicateTranferException;
import com.moneytransfersystem.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<Object> transfer(@Valid @RequestBody TransferRequest request) {
        try {
            TransactionLog result = transferService.transfer(request);
            
            // Return the transaction object directly (tests expect top-level transaction fields)
            if ("SUCCESS".equals(result.getStatus().toString())) {
                return ResponseEntity.ok(result);
            } else if ("FAILED".equals(result.getStatus().toString())) {
                // Return failed transaction with 400 status and the transaction object as body
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.ok(result);
        } catch (DuplicateTranferException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Duplicate transaction",
                    "message", e.getMessage()
            ));
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Account not found",
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Invalid transfer",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Transfer failed",
                    "message", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred"
            ));
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransactionLog>> getHistory(@PathVariable String accountId) {
        return ResponseEntity.ok(transferService.getTransactionHistory(accountId));
    }
}
