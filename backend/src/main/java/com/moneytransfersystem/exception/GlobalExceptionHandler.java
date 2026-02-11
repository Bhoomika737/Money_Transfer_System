package com.moneytransfersystem.exception;

import com.moneytransfersystem.domain.exceptions.AccountNotFoundException;
import com.moneytransfersystem.domain.exceptions.AccountNotActiveException;
import com.moneytransfersystem.domain.exceptions.InsufficientBalanceException;
import com.moneytransfersystem.domain.exceptions.DuplicateTranferException;
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

    // 1. Account Not Found -> 404 NOT FOUND
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(AccountNotFoundException ex) {
        return buildResponse("Account Not Found", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // 2. Insufficient Balance -> 400 BAD REQUEST
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex) {
        return buildResponse("Insufficient Balance", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 3. Account Closed/Inactive -> 403 FORBIDDEN (or 400)
    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotActive(AccountNotActiveException ex) {
        return buildResponse("Account Inactive", ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    // 4. Duplicate Transfer -> 409 CONFLICT
    @ExceptionHandler(DuplicateTranferException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateTranferException ex) {
        return buildResponse("Duplicate Transaction", ex.getMessage(), HttpStatus.CONFLICT);
    }

    // 5. Invalid Arguments (e.g. negative numbers) -> 400 BAD REQUEST
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse("Invalid Request", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // 6. Validation Errors (e.g. missing fields) -> 400 BAD REQUEST
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

    // 7. Fallback for everything else -> 500 INTERNAL SERVER ERROR
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {

        return buildResponse("Internal Server Error", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper Method
    private ResponseEntity<Map<String, Object>> buildResponse(String errorType, String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("error", errorType);
        error.put("message", message);
        error.put("status", status.value());
        return new ResponseEntity<>(error, status);
    }
}