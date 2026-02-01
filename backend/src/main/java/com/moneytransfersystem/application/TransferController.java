// src/main/java/com/moneytransfersystem/application/TransferController.java
package com.moneytransfersystem.application;

import com.moneytransfersystem.domain.dtos.TransferRequest;
import com.moneytransfersystem.domain.dtos.TransferResponse;
import com.moneytransfersystem.domain.service.MoneyTransferDomainService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final MoneyTransferDomainService transferService;

    public TransferController(MoneyTransferDomainService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.ok(response);
    }
}
