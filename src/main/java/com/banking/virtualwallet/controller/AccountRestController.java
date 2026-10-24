package com.banking.virtualwallet.controller;

import com.banking.virtualwallet.dto.TransactionDTOs.DepositRequest;
import com.banking.virtualwallet.dto.WalletDTOs.AccountResponse;
import com.banking.virtualwallet.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Account Management", description = "Account details and balance management")
public class AccountRestController {

    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Retrieves full account details and verifies the wallet sum invariant.
     */
    @GetMapping
    @Operation(summary = "Get current account details", description = "Returns total available balance, wallet balances, and verifies sum invariant")
    public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(accountService.getAccountDetails(username));
    }

    /**
     * Returns an overview summary of account balance and account metadata for the dashboard.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get account summary", description = "Returns total balance, available balance, and monthly change for dashboard")
    public ResponseEntity<Map<String, Object>> getAccountSummary(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        AccountResponse account = accountService.getAccountDetails(username);
        
        Map<String, Object> summary = Map.of(
            "totalBalance", account.getTotalBalance(),
            "availableBalance", account.getTotalBalance(),
            "monthlyChange", BigDecimal.ZERO,
            "monthlyChangePercent", 0.0,
            "accountNumber", account.getAccountNumber(),
            "accountType", "Primary"
        );
        return ResponseEntity.ok(summary);
    }

    /**
     * Credits user account balance with deposited funds, automatically routing to the General wallet.
     */
    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds to account", description = "Credits account balance and auto-allocates to General Wallet")
    public ResponseEntity<AccountResponse> depositFunds(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody DepositRequest request) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(accountService.depositFunds(username, request.getAmount()));
    }

    /**
     * Transfers funds to another account via Account Number or UPI ID, debiting the selected virtual wallet.
     */
    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds to another account", description = "Account-to-account transfer by recipient account number")
    public ResponseEntity<Map<String, Object>> transferToAccount(@AuthenticationPrincipal UserDetails userDetails,
                                                                 @Valid @RequestBody com.banking.virtualwallet.dto.TransactionDTOs.AccountTransferRequest request) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(accountService.transferBetweenAccounts(username, request.getToAccountNumber(), request.getToUpiId(), request.getFromWalletId(), request.getBackupWalletId(), request.getAmount(), request.getDescription()));
    }
}
