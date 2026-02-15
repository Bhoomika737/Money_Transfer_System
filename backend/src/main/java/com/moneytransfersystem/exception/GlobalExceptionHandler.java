package com.moneytransfersystem.exception;

import com.moneytransfersystem.domain.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Account Not Found -> 404
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(AccountNotFoundException ex) {
        return buildResponse("Account Not Found", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // 2. Insufficient Balance -> 400
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return buildResponse("Insufficient Balance", ex.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
    }

    // 3. Account Inactive -> 403
    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotActive(AccountNotActiveException ex) {
        return buildResponse("Account Inactive", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    // 4. Duplicate Transfer -> 409
    @ExceptionHandler(DuplicateTranferException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateTranferException ex) {
        return buildResponse("Duplicate Transaction", ex.getMessage(), HttpStatus.CONFLICT);
    }

    // 5. Invalid Arguments -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse("Invalid Request", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 6. Validation Errors -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now());
        errors.put("error", "Validation Error");
        errors.put("status", HttpStatus.BAD_REQUEST.value());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        errors.put("details", fieldErrors);

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // ✅ 7. Generic Handler (FIXED FOR DEBUGGING)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {

        // ✅ Print full exception in console
        ex.printStackTrace();

        // ✅ Return real exception message
        return buildResponse(
                "Internal Server Error",
                ex.getMessage(),   // <-- REAL ERROR SHOWN NOW
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Helper Method
    private ResponseEntity<Map<String, Object>> buildResponse(String errorType,
                                                             String message,
                                                             HttpStatus status) {

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("error", errorType);
        error.put("message", message);
        error.put("status", status.value());

        return new ResponseEntity<>(error, status);
    }
}
