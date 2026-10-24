package com.banking.virtualwallet.controller;

import com.banking.virtualwallet.dto.TransactionDTOs.InsufficientFundsResponse;
import com.banking.virtualwallet.service.InsufficientFundsNeedBackupException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles multi-tier payment deficit exceptions, returning HTTP 400 with eligible backup wallets.
     */
    @ExceptionHandler(InsufficientFundsNeedBackupException.class)
    public ResponseEntity<InsufficientFundsResponse> handleInsufficientFundsNeedBackup(InsufficientFundsNeedBackupException ex) {
        InsufficientFundsResponse response = new InsufficientFundsResponse(
                ex.getRequestedAmount(),
                ex.getAvailablePrimary(),
                ex.getAvailableGeneral(),
                ex.getDeficit(),
                ex.getEligibleWallets(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles bad request / business logic rule validation exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "BAD_REQUEST");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles internal state inconsistency exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "ILLEGAL_STATE");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles Jakarta Bean Validation errors on incoming request DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> response = new HashMap<>();
        response.put("error", "VALIDATION_FAILED");
        response.put("details", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * A request for a static asset (for example a stale Next.js chunk or font
     * file) that is not on disk. Spring presets the response content type from
     * the file extension, so replying with a JSON body here would fail with
     * HttpMessageNotWritableException. Return a bare 404 instead.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Last-resort handler. The response content type may already be preset from
     * a file extension (e.g. {@code text/javascript} for a missing chunk), so a
     * Map body would blow up while being written and turn a clean error into an
     * opaque 500. Force {@code application/json} so the body always serialises.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "INTERNAL_ERROR");
        error.put("message", ex.getMessage() == null ? "Unexpected error" : ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }
}