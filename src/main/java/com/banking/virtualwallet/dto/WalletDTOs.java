package com.banking.virtualwallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Objects for virtual wallet CRUD operations, transfers, and reallocations.
 */
public class WalletDTOs {

    /** Request payload for creating a new virtual wallet. */
    public static class WalletCreateRequest {
        @NotBlank(message = "Wallet name is required")
        private String name;

        private String category;

        @NotNull(message = "Initial allocation amount is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Allocation must be >= 0")
        private BigDecimal initialAllocation = BigDecimal.ZERO;

        public WalletCreateRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getInitialAllocation() { return initialAllocation; }
        public void setInitialAllocation(BigDecimal initialAllocation) { this.initialAllocation = initialAllocation; }
    }

    /** Request payload for renaming a virtual wallet. */
    public static class WalletRenameRequest {
        @NotBlank(message = "Wallet name is required")
        @Size(max = 40, message = "Wallet name must be 40 characters or fewer")
        private String name;

        public WalletRenameRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /** Request payload for moving allocated funds between two wallets. */
    public static class WalletTransferRequest {
        @NotNull(message = "Source wallet ID is required")
        private Long fromWalletId;

        @NotNull(message = "Destination wallet ID is required")
        private Long toWalletId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Transfer amount must be greater than 0")
        private BigDecimal amount;

        public WalletTransferRequest() {}

        public Long getFromWalletId() { return fromWalletId; }
        public void setFromWalletId(Long fromWalletId) { this.fromWalletId = fromWalletId; }
        public Long getToWalletId() { return toWalletId; }
        public void setToWalletId(Long toWalletId) { this.toWalletId = toWalletId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /** Single wallet balance assignment in a reallocation batch. */
    public static class WalletReallocationItem {
        private Long walletId;
        private BigDecimal newBalance;

        public WalletReallocationItem() {}
        public WalletReallocationItem(Long walletId, BigDecimal newBalance) {
            this.walletId = walletId;
            this.newBalance = newBalance;
        }

        public Long getWalletId() { return walletId; }
        public void setWalletId(Long walletId) { this.walletId = walletId; }
        public BigDecimal getNewBalance() { return newBalance; }
        public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    }

    /** Request payload for rebalancing multiple wallets simultaneously. */
    public static class WalletReallocateRequest {
        @NotNull(message = "Allocations list is required")
        private List<WalletReallocationItem> allocations;

        public WalletReallocateRequest() {}

        public List<WalletReallocationItem> getAllocations() { return allocations; }
        public void setAllocations(List<WalletReallocationItem> allocations) { this.allocations = allocations; }
    }

    /** Response payload describing a virtual wallet and its allocation share. */
    public static class WalletResponse {
        private Long id;
        private String name;
        private String category;
        private BigDecimal balance;
        private boolean isDefaultGeneral;
        private double percentageOfTotal;

        public WalletResponse() {}

        public WalletResponse(Long id, String name, String category, BigDecimal balance, boolean isDefaultGeneral, double percentageOfTotal) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.balance = balance;
            this.isDefaultGeneral = isDefaultGeneral;
            this.percentageOfTotal = percentageOfTotal;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public boolean isDefaultGeneral() { return isDefaultGeneral; }
        public void setDefaultGeneral(boolean defaultGeneral) { isDefaultGeneral = defaultGeneral; }
        public double getPercentageOfTotal() { return percentageOfTotal; }
        public void setPercentageOfTotal(double percentageOfTotal) { this.percentageOfTotal = percentageOfTotal; }
    }

    /** Response payload returning master account status and wallet breakdown. */
    public static class AccountResponse {
        private String accountNumber;
        private BigDecimal totalBalance;
        private BigDecimal totalWalletsBalance;
        private boolean balanced;
        private List<WalletResponse> wallets;

        public AccountResponse() {}

        public AccountResponse(String accountNumber, BigDecimal totalBalance, BigDecimal totalWalletsBalance, boolean balanced, List<WalletResponse> wallets) {
            this.accountNumber = accountNumber;
            this.totalBalance = totalBalance;
            this.totalWalletsBalance = totalWalletsBalance;
            this.balanced = balanced;
            this.wallets = wallets;
        }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public BigDecimal getTotalBalance() { return totalBalance; }
        public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
        public BigDecimal getTotalWalletsBalance() { return totalWalletsBalance; }
        public void setTotalWalletsBalance(BigDecimal totalWalletsBalance) { this.totalWalletsBalance = totalWalletsBalance; }
        public boolean isBalanced() { return balanced; }
        public void setBalanced(boolean balanced) { this.balanced = balanced; }
        public List<WalletResponse> getWallets() { return wallets; }
        public void setWallets(List<WalletResponse> wallets) { this.wallets = wallets; }
    }
}
