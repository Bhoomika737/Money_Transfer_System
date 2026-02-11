package com.moneytransfersystem.controllers;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.entities.TransactionLog;
import com.moneytransfersystem.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransactionLog> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.transfer(request));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransactionLog>> getHistory(@PathVariable String accountId) {
        return ResponseEntity.ok(transferService.getTransactionHistory(accountId));
    }
}