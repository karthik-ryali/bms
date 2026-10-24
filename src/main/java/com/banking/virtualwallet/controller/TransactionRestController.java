package com.banking.virtualwallet.controller;

import com.banking.virtualwallet.dto.TransactionDTOs.PaymentRequest;
import com.banking.virtualwallet.dto.TransactionDTOs.PaymentResponse;
import com.banking.virtualwallet.entity.Transaction;
import com.banking.virtualwallet.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction recording, multi-tier overflow payments, and transaction history")
public class TransactionRestController {

    private final TransactionService transactionService;

    public TransactionRestController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Processes a payment transaction using the flagship multi-tier overflow algorithm
     * across primary, General, and backup virtual wallets.
     */
    @PostMapping
    @Operation(summary = "Record a new payment transaction",
               description = "Processes payment using target wallet. In case of deficit, automatically utilizes General Wallet overflow, and subsequently prompts/uses alternate backup wallet if required.")
    public ResponseEntity<PaymentResponse> makePayment(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody PaymentRequest request) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(transactionService.processPayment(username, request));
    }

    /**
     * Retrieves the chronological transaction ledger for the user, with optional filtering by wallet ID.
     */
    @GetMapping
    @Operation(summary = "Get transaction history",
               description = "Retrieves wallet-wise or full account transaction history ordered chronologically.")
    public ResponseEntity<List<Transaction>> getTransactions(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestParam(value = "walletId", required = false) Long walletId) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(transactionService.getTransactionsForUser(username, walletId));
    }
}
