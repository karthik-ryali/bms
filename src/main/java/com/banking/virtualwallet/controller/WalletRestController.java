package com.banking.virtualwallet.controller;

import com.banking.virtualwallet.dto.WalletDTOs.*;
import com.banking.virtualwallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Virtual Wallets", description = "Virtual wallet creation, transfers, and allocations")
public class WalletRestController {

    private final WalletService walletService;

    public WalletRestController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Retrieves all logical virtual wallets configured for the authenticated user with balance and allocation share.
     */
    @GetMapping
    @Operation(summary = "Get user virtual wallets", description = "Retrieves all logical virtual wallets with individual balances and allocation percentages")
    public ResponseEntity<List<WalletResponse>> getWallets(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(walletService.getWalletsForUser(username));
    }

    /**
     * Creates a new virtual wallet, funding its initial allocation from the General wallet.
     */
    @PostMapping
    @Operation(summary = "Create a virtual wallet", description = "Creates a new virtual wallet. Initial allocation is deducted from the General Wallet")
    public ResponseEntity<WalletResponse> createWallet(@AuthenticationPrincipal UserDetails userDetails,
                                                       @Valid @RequestBody WalletCreateRequest request) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(walletService.createWallet(username, request));
    }

    /**
     * Transfers an allocation between two virtual wallets without altering the master account balance.
     */
    @PostMapping("/transfer")
    @Operation(summary = "Transfer between wallets", description = "Transfers money allocation between two virtual wallets without changing total account balance")
    public ResponseEntity<Map<String, String>> transfer(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody WalletTransferRequest request) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        walletService.transferBetweenWallets(username, request);
        return ResponseEntity.ok(Map.of("message", "Transfer successful"));
    }

    /**
     * Rebalances multiple wallets simultaneously, asserting the balance invariant.
     */
    @PostMapping("/reallocate")
    @Operation(summary = "Reallocate all wallets", description = "Rebalances wallet allocations ensuring sum of balances equals total balance")
    public ResponseEntity<Map<String, String>> reallocate(@AuthenticationPrincipal UserDetails userDetails,
                                                          @Valid @RequestBody WalletReallocateRequest request) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        walletService.reallocateWallets(username, request);
        return ResponseEntity.ok(Map.of("message", "Reallocation successful"));
    }

    /**
     * Renames an existing virtual wallet while preserving its balance.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Rename a virtual wallet", description = "Renames an existing virtual wallet without altering its balance")
    public ResponseEntity<WalletResponse> renameWallet(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable("id") Long id,
                                                       @Valid @RequestBody WalletRenameRequest request) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        return ResponseEntity.ok(walletService.renameWallet(username, id, request));
    }

    /**
     * Deletes a virtual wallet and refunds its remaining balance back to the default General wallet.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete wallet", description = "Deletes a virtual wallet and refunds its remaining balance back to General Wallet")
    public ResponseEntity<Map<String, String>> deleteWallet(@AuthenticationPrincipal UserDetails userDetails,
                                                            @PathVariable("id") Long id) {
        String username = userDetails != null ? userDetails.getUsername() : "karthik";
        walletService.deleteWallet(username, id);
        return ResponseEntity.ok(Map.of("message", "Wallet deleted and balance returned to General Wallet"));
    }
}
